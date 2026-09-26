package com.romeo.paper.input;

import com.romeo.paper.LegacyText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ONE chat-input system for the whole plugin.
 *
 * Design contracts (work.md §6):
 * - A single AsyncPlayerChatEvent listener serves every GUI and command flow;
 *   nothing else may listen for chat input.
 * - The chat event is always cancelled while a session is active.
 * - All callbacks run on the MAIN thread (async chat -> runTask hop).
 * - Validation errors re-prompt instead of ending the session; "cancel"
 *   aborts; sessions expire silently after the configured timeout.
 * - Sessions can be chained (multi-step flows) via {@link InputSession#then}.
 */
public final class InputManager implements Listener {
    private static final long DEFAULT_TIMEOUT_TICKS = 30L * 20L;

    private final JavaPlugin plugin;
    private final Map<UUID, InputSession<?>> sessions = new ConcurrentHashMap<UUID, InputSession<?>>();
    private int cleanupTaskId = -1;

    public InputManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers the listener and the periodic expiry sweep. Called once on enable. */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        cleanupTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                expireStaleSessions();
            }
        }, 200L, 200L).getTaskId();
    }

    /** Unregisters the listener and cancels the sweep. Called on disable. */
    public void unregister() {
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
        HandlerList.unregisterAll(this);
        sessions.clear();
    }

    // ------------------------------------------------------------------
    // Typed begin APIs
    // ------------------------------------------------------------------

    /** Begins a free-text session (color codes allowed). */
    public InputSession<String> beginString(Player player, String prompt, java.util.function.Consumer<String> onSuccess) {
        return begin(player, InputSession.Type.STRING, prompt, onSuccess);
    }

    /** Begins an integer session; invalid numbers re-prompt with "Invalid number. Try again.". */
    public InputSession<Integer> beginInteger(Player player, String prompt, java.util.function.Consumer<Integer> onSuccess) {
        return begin(player, InputSession.Type.INTEGER, prompt, onSuccess);
    }

    /** Begins a URL session restricted to http(s); invalid URLs re-prompt with "Invalid URL.". */
    public InputSession<String> beginUrl(Player player, String prompt, java.util.function.Consumer<String> onSuccess) {
        return begin(player, InputSession.Type.URL, prompt, onSuccess);
    }

    /**
     * Begins a yes/confirm vs no/cancel session. Exactly one of the two
     * callbacks runs, on the main thread.
     */
    public InputSession<String> beginConfirmation(Player player, String prompt,
                                                  final Runnable onYes, final Runnable onNo) {
        InputSession<String> session = begin(player, InputSession.Type.CONFIRMATION, prompt,
                new java.util.function.Consumer<String>() {
                    @Override
                    public void accept(String answer) {
                        String lowered = answer.toLowerCase(java.util.Locale.ENGLISH);
                        if (lowered.equals("yes") || lowered.equals("confirm")) {
                            onYes.run();
                        } else {
                            onNo.run();
                        }
                    }
                });
        session.withoutCancel();
        return session;
    }

    /**
     * Begins a selection session over the given options. The player types one
     * of the option labels (case-insensitive); invalid choices re-prompt.
     */
    public InputSession<String> beginSelection(Player player, String prompt,
                                               final java.util.List<String> options,
                                               final java.util.function.Consumer<String> onSuccess) {
        InputSession<String> session = begin(player, InputSession.Type.SELECTION, prompt, onSuccess);
        session.validatedBy(new java.util.function.Predicate<String>() {
            @Override
            public boolean test(String message) {
                for (String option : options) {
                    if (option.equalsIgnoreCase(message)) {
                        return true;
                    }
                }
                return false;
            }
        });
        session.withInvalidMessage("&cPick one of: " + joinOptions(options));
        return session;
    }

    // ------------------------------------------------------------------
    // Internal machinery
    // ------------------------------------------------------------------

    private <T> InputSession<T> begin(Player player, InputSession.Type type, String prompt,
                                      java.util.function.Consumer<T> onSuccess) {
        InputSession<T> session = new InputSession<T>(player, type, prompt, onSuccess);
        session.withTimeoutSeconds((int) (DEFAULT_TIMEOUT_TICKS / 20L));
        InputSession<?> previous = sessions.put(player.getUniqueId(), session);
        if (previous != null) {
            safeCancelHandler(previous);
        }
        player.closeInventory();
        player.sendMessage(LegacyText.colorize("&b[Input] &f" + prompt + " &7(Type cancel to abort.)"));
        return session;
    }

    /** Single chat entry point for the entire plugin. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final InputSession<?> session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        final String message = event.getMessage() == null ? "" : event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                handleOnMainThread(player, session, message);
            }
        });
    }

    private void handleOnMainThread(Player player, InputSession<?> session, String message) {
        // Re-check: the session may have been replaced or expired meanwhile.
        if (sessions.get(player.getUniqueId()) != session) {
            return;
        }
        if (session.allowsCancel() && message.equalsIgnoreCase("cancel")) {
            finish(player, session, false);
            player.sendMessage(LegacyText.colorize("&7[Input] Cancelled."));
            return;
        }
        InputSession.ValidationResult<?> result = session.validate(message);
        if (!result.isValid()) {
            String error = result.error();
            if (session.onInvalid() != null) {
                session.onInvalid().accept(error);
            }
            player.sendMessage(LegacyText.colorize(error));
            player.sendMessage(LegacyText.colorize("&7[Input] &f" + session.prompt()));
            return;
        }
        sessions.remove(player.getUniqueId());
        runSuccess(player, session, result.value());
        player.sendMessage(LegacyText.colorize("&a[Input] Done."));
    }

    @SuppressWarnings("unchecked")
    private <T> void runSuccess(Player player, InputSession<T> session, Object value) {
        try {
            session.onSuccess().accept((T) value);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Input callback failed: " + throwable.getMessage());
        }
        InputSession<?> following = session.next();
        if (following != null) {
            sessions.put(player.getUniqueId(), following);
            player.closeInventory();
            player.sendMessage(LegacyText.colorize("&b[Input] &f" + following.prompt() + " &7(Type cancel to abort.)"));
        }
    }

    private void finish(Player player, InputSession<?> session, boolean runCancelHandler) {
        sessions.remove(player.getUniqueId());
        if (runCancelHandler && session.onCancel() != null) {
            try {
                session.onCancel().run();
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Input cancel handler failed: " + throwable.getMessage());
            }
        }
    }

    private void safeCancelHandler(InputSession<?> previous) {
        if (previous.onCancel() != null) {
            try {
                previous.onCancel().run();
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Input cancel handler failed: " + throwable.getMessage());
            }
        }
    }

    private void expireStaleSessions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, InputSession<?>> entry : sessions.entrySet()) {
            InputSession<?> session = entry.getValue();
            if (session.timeoutTicks() <= 0) {
                continue;
            }
            if (now - session.createdAt() > session.timeoutTicks() * 50L) {
                Player player = session.player();
                sessions.remove(entry.getKey());
                if (session.onCancel() != null) {
                    try {
                        session.onCancel().run();
                    } catch (Throwable throwable) {
                        plugin.getLogger().warning("Input cancel handler failed: " + throwable.getMessage());
                    }
                }
                player.sendMessage(LegacyText.colorize("&7[Input] Timed out waiting for input."));
            }
        }
    }

    private static String joinOptions(java.util.List<String> options) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(options.get(i));
        }
        return builder.toString();
    }
}
