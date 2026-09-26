package com.romeo.paper.services;

import com.romeo.paper.LegacyText;
import com.romeo.paper.NpcData;
import com.romeo.paper.NpcManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Shared NPC operations used by {@code /npc} and the NPC Manager GUI. Wraps
 * {@link NpcManager} and owns the click-action execution so the entity
 * listener and the GUI never duplicate behavior.
 */
public final class NpcService {
    private final JavaPlugin plugin;
    private final NpcManager npcManager;

    public NpcService(JavaPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    public NpcManager manager() {
        return npcManager;
    }

    public NpcData get(String name) {
        return npcManager.get(name);
    }

    public Collection<NpcData> all() {
        return npcManager.all();
    }

    /** Creates an NPC at the given location; null when the name is taken. */
    public NpcData create(String name, Location location) {
        NpcData data = npcManager.create(name, location);
        npcManager.save();
        return data;
    }

    /** Removes an NPC. Returns false when it does not exist. */
    public boolean remove(String name) {
        boolean removed = npcManager.remove(name);
        npcManager.save();
        return removed;
    }

    public boolean setEnabled(String name, boolean enabled) {
        npcManager.setEnabled(name, enabled);
        npcManager.save();
        return true;
    }

    /** Moves the NPC to the given location (GUI "Move here" / /npc move). */
    public boolean move(String name, Location location) {
        npcManager.move(name, location);
        npcManager.save();
        return true;
    }

    /** Renames the visible display name of the NPC. */
    public boolean setDisplayName(String name, String displayName) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            return false;
        }
        data.setDisplayName(displayName);
        ArmorStand stand = npcManager.entity(name);
        if (stand != null) {
            stand.setCustomName(LegacyText.colorize(displayName));
        }
        npcManager.save();
        return true;
    }

    public boolean setNametag(String name, boolean visible) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            return false;
        }
        data.setNametag(visible);
        ArmorStand stand = npcManager.entity(name);
        if (stand != null) {
            stand.setCustomNameVisible(visible);
        }
        npcManager.save();
        return true;
    }

    public boolean setGlow(String name, boolean glow) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            return false;
        }
        data.setGlow(glow);
        ArmorStand stand = npcManager.entity(name);
        if (stand != null) {
            NpcManager.invokeIfPresent(stand, "setGlowing", new Class<?>[]{boolean.class}, new Object[]{glow});
        }
        npcManager.save();
        return true;
    }

    public boolean setLook(String name, boolean look) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            return false;
        }
        data.setLook(look);
        npcManager.save();
        return true;
    }

    /** Result of a skin change attempt. */
    public interface SkinCallback {
        void onDone(boolean success, String message);
    }

    /**
     * Validates a skin name against the Mojang profile API off the main thread
     * and stores it on the NPC. Callback runs on the main thread.
     */
    public void setSkinAsync(final String name, final String skinName, final SkinCallback callback) {
        final NpcData data = npcManager.get(name);
        if (data == null) {
            callback.onDone(false, "NPC not found.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Exception failure = null;
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(
                            "https://api.mojang.com/users/profiles/minecraft/" + skinName).openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    if (connection.getResponseCode() != 200) {
                        throw new IllegalStateException("Mojang profile not found");
                    }
                } catch (Exception exception) {
                    failure = exception;
                }
                final Exception finalFailure = failure;
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (finalFailure != null) {
                            callback.onDone(false, "Skin lookup failed: " + finalFailure.getMessage());
                            return;
                        }
                        data.setSkin(skinName);
                        npcManager.save();
                        callback.onDone(true, "Skin metadata updated for " + skinName + ".");
                    }
                });
            }
        });
    }

    /** Adds a click action. Returns false when the type is unsupported. */
    public boolean addAction(String name, String click, String type, String value) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            return false;
        }
        String normalized = type.toLowerCase(Locale.ENGLISH);
        if (!normalized.equals("message") && !normalized.equals("command")
                && !normalized.equals("console") && !normalized.equals("sound")) {
            return false;
        }
        data.getActions(click).add(normalized + "|" + value);
        npcManager.save();
        return true;
    }

    /** Removes all actions of one click type. Returns the removed count. */
    public int clearActions(String name, String click) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            return 0;
        }
        List<String> actions = data.getActions(click);
        int count = actions.size();
        actions.clear();
        npcManager.save();
        return count;
    }

    /**
     * Executes the stored actions of one click type. Shared by the entity
     * click listeners and the GUI "Test Action" button.
     */
    public void executeActions(Player player, String npcName, String click) {
        NpcData data = npcManager.get(npcName);
        if (data == null || !data.isEnabled()) {
            return;
        }
        List<String> actions = data.getActions(click);
        if (actions == null) {
            return;
        }
        for (String raw : actions) {
            int separator = raw.indexOf('|');
            if (separator <= 0) {
                continue;
            }
            String type = raw.substring(0, separator).toLowerCase(Locale.ENGLISH);
            String value = raw.substring(separator + 1).replace("%player%", player.getName());
            try {
                if (type.equals("message")) {
                    player.sendMessage(LegacyText.colorize(value));
                } else if (type.equals("command")) {
                    player.performCommand(value.startsWith("/") ? value.substring(1) : value);
                } else if (type.equals("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.startsWith("/") ? value.substring(1) : value);
                } else if (type.equals("sound")) {
                    Sound sound = Sound.valueOf(value.toUpperCase(Locale.ENGLISH));
                    ArmorStand stand = npcManager.entity(npcName);
                    if (stand != null) {
                        player.getWorld().playSound(stand.getLocation(), sound, 1.0f, 1.0f);
                    }
                }
            } catch (Throwable exception) {
                plugin.getLogger().warning("NPC action failed for " + npcName + ": " + exception.getMessage());
            }
        }
    }

    /** Spawns a one-shot particle burst at the NPC if the type exists on this server. */
    public boolean playParticle(String name, String particleName) {
        ArmorStand stand = npcManager.entity(name);
        if (stand == null) {
            return false;
        }
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object particle = Enum.valueOf((Class) particleClass, particleName.toUpperCase(Locale.ENGLISH));
            NpcManager.invoke(stand.getWorld(), "spawnParticle",
                    new Class<?>[]{particleClass, Location.class, int.class},
                    new Object[]{particle, stand.getLocation(), 10});
            return true;
        } catch (Throwable exception) {
            plugin.getLogger().warning("Particle is not available on this server: " + particleName);
            return false;
        }
    }

    public void save() {
        npcManager.save();
    }

    public void reload() {
        npcManager.load();
    }
}
