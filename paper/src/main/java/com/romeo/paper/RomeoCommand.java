package com.romeo.paper;

import com.romeo.paper.services.Services;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Thin command frontend. All business logic lives in the shared services
 * layer ({@link Services}) so the settings GUI and the commands operate on
 * exactly the same backend code paths.
 */
public final class RomeoCommand implements CommandExecutor {
    private final Services services;

    public RomeoCommand(Services services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ENGLISH);
        if (name.equals("ping")) {
            return ping(sender);
        }
        if (name.equals("settings")) {
            return settings(sender);
        }
        if (!sender.hasPermission("plugin.admin")) {
            sender.sendMessage(LegacyText.colorize("&cYou do not have permission."));
            return true;
        }
        if (name.equals("editmotd")) {
            return editMotd(sender, args);
        }
        if (name.equals("editplayers")) {
            return editPlayers(sender, args);
        }
        if (name.equals("editicon")) {
            return editIcon(sender, args);
        }
        if (name.equals("npc")) {
            return npc(sender, args);
        }
        return false;
    }

    private boolean settings(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the settings panel.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("plugin.admin")) {
            player.sendMessage(LegacyText.colorize("&cYou do not have permission."));
            return true;
        }
        // GUI layer hooks in here in P3 (GuiManager.openMain).
        player.sendMessage(LegacyText.colorize("&7[Settings] GUI opens here once the GUI layer is wired."));
        return true;
    }

    private boolean ping(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /ping.");
            return true;
        }
        Player player = (Player) sender;
        sender.sendMessage(LegacyText.colorize("&aYour latency: &f" + services.ping().formatted(player)));
        return true;
    }

    private boolean editMotd(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /editmotd <text>");
            return true;
        }
        if (services.motd().setMotd(join(args, 0))) {
            sender.sendMessage(LegacyText.colorize("&aMOTD updated."));
        } else {
            sender.sendMessage(LegacyText.colorize("&cMOTD cannot be empty."));
        }
        return true;
    }

    private boolean editPlayers(CommandSender sender, String[] args) {
        if (args.length != 2 || (!args[0].equalsIgnoreCase("online") && !args[0].equalsIgnoreCase("max"))) {
            sender.sendMessage("Usage: /editplayers <online|max> <count>");
            return true;
        }
        try {
            int count = Integer.parseInt(args[1]);
            boolean ok = args[0].equalsIgnoreCase("online")
                    ? services.playerCount().setOnlineDisplay(count)
                    : services.playerCount().setMaxDisplay(count);
            sender.sendMessage(LegacyText.colorize(ok ? "&aDisplayed player count updated."
                    : "&cCount must be a non-negative number."));
        } catch (NumberFormatException exception) {
            sender.sendMessage(LegacyText.colorize("&cCount must be a non-negative number."));
        }
        return true;
    }

    private boolean editIcon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length != 1) {
            sender.sendMessage("Usage: /editicon <imageUrl>");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: /editicon <imageUrl>");
            return true;
        }
        sender.sendMessage(LegacyText.colorize("&eDownloading and converting server icon..."));
        services.icon().downloadAsync(args[0], new com.romeo.paper.services.IconService.Callback() {
            @Override
            public void onDone(boolean success, String message) {
                sender.sendMessage(LegacyText.colorize(success ? "&aServer icon updated and cached."
                        : "&c" + message));
            }
        });
        return true;
    }

    private boolean npc(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /npc <create|remove|list|move|info|skin|name|nametag|glow|look|tp|enable|disable|save|reload|rightclick|leftclick|particle>");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        if (action.equals("list")) {
            StringBuilder names = new StringBuilder("&aNPCs: ");
            for (NpcData data : services.npc().all()) {
                names.append(data.getName()).append(data.isEnabled() ? " &7" : " &8").append(" ");
            }
            sender.sendMessage(LegacyText.colorize(names.toString()));
            return true;
        }
        if (action.equals("save")) {
            services.npc().save();
            sender.sendMessage(LegacyText.colorize("&aNPCs saved."));
            return true;
        }
        if (action.equals("reload")) {
            services.npc().reload();
            sender.sendMessage(LegacyText.colorize("&aNPCs reloaded."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("This NPC operation requires a player.");
            return true;
        }
        Player player = (Player) sender;
        if (action.equals("create")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /npc create <name>");
                return true;
            }
            NpcData data = services.npc().create(args[1], player.getLocation());
            sender.sendMessage(data == null ? "An NPC with that name already exists."
                    : LegacyText.colorize("&aNPC created."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("NPC name is required.");
            return true;
        }
        String npcName = args[1];
        NpcData data = services.npc().get(npcName);
        if (data == null) {
            sender.sendMessage("NPC not found.");
            return true;
        }
        if (action.equals("remove")) {
            services.npc().remove(npcName);
        } else if (action.equals("move")) {
            services.npc().move(npcName, player.getLocation());
        } else if (action.equals("tp")) {
            player.teleport(new Location(Bukkit.getWorld(data.getWorld()), data.getX(), data.getY(), data.getZ(),
                    data.getYaw(), data.getPitch()));
        } else if (action.equals("enable") || action.equals("disable")) {
            services.npc().setEnabled(npcName, action.equals("enable"));
        } else if (action.equals("info")) {
            sender.sendMessage(LegacyText.colorize("&a" + data.getName() + " &7(" + data.getWorld() + " "
                    + data.getX() + ", " + data.getY() + ", " + data.getZ() + ") skin=" + data.getSkin()));
        } else if (action.equals("skin") && args.length >= 3) {
            services.npc().setSkinAsync(npcName, args[2], new com.romeo.paper.services.NpcService.SkinCallback() {
                @Override
                public void onDone(boolean success, String message) {
                    sender.sendMessage(LegacyText.colorize((success ? "&a" : "&c") + message));
                }
            });
            return true; // save happens in the service after the lookup
        } else if (action.equals("name") && args.length >= 3) {
            services.npc().setDisplayName(npcName, join(args, 2));
        } else if (action.equals("nametag") || action.equals("glow") || action.equals("look")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /npc " + action + " <name> <true|false>");
                return true;
            }
            boolean value = Boolean.parseBoolean(args[2]);
            if (action.equals("nametag")) {
                services.npc().setNametag(npcName, value);
            } else if (action.equals("glow")) {
                services.npc().setGlow(npcName, value);
            } else {
                services.npc().setLook(npcName, value);
            }
        } else if ((action.equals("rightclick") || action.equals("leftclick")) && args.length >= 4) {
            services.npc().addAction(npcName, action, args[2], join(args, 3));
        } else if (action.equals("particle") && args.length == 3) {
            if (!services.npc().playParticle(npcName, args[2])) {
                sender.sendMessage(LegacyText.colorize("&cParticle is not available on this server."));
                return true;
            }
        } else {
            sender.sendMessage("Invalid NPC operation or arguments.");
            return true;
        }
        sender.sendMessage(LegacyText.colorize("&aNPC updated."));
        return true;
    }

    private static String join(String[] args, int start) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                result.append(' ');
            }
            result.append(args[i]);
        }
        return result.toString();
    }
}
