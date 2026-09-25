package com.romeo.paper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RomeoCommand implements CommandExecutor {
    private final RomeoPaperPlugin plugin;
    private final NpcManager npcManager;
    private final SettingsGUI settingsGUI;
    private final OptionsScreen optionsScreen;

    RomeoCommand(RomeoPaperPlugin plugin, NpcManager npcManager, SettingsGUI settingsGUI, OptionsScreen optionsScreen) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.settingsGUI = settingsGUI;
        this.optionsScreen = optionsScreen;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ENGLISH);
        if (name.equals("ping")) {
            return ping(sender);
        }
        if (name.equals("settings")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can open the settings panel.");
                return true;
            }
            if (optionsScreen != null) {
                optionsScreen.open((Player) sender);
            } else if (settingsGUI != null) {
                settingsGUI.open((Player) sender);
            }
            return true;
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

    private boolean ping(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /ping.");
            return true;
        }
        Integer ping = getPing((Player) sender);
        sender.sendMessage(LegacyText.colorize("&aYour latency: &f" + (ping == null ? "unavailable" : ping + " ms")));
        return true;
    }

    private boolean editMotd(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /editmotd <text>");
            return true;
        }
        String motd = join(args, 0);
        plugin.getConfig().set("server-list.motd", motd);
        plugin.saveConfig();
        sender.sendMessage(LegacyText.colorize("&aMOTD updated."));
        return true;
    }

    private boolean editPlayers(CommandSender sender, String[] args) {
        if (args.length != 2 || (!args[0].equalsIgnoreCase("online") && !args[0].equalsIgnoreCase("max"))) {
            sender.sendMessage("Usage: /editplayers <online|max> <count>");
            return true;
        }
        try {
            int count = Integer.parseInt(args[1]);
            if (count < 0) {
                throw new NumberFormatException("negative");
            }
            plugin.getConfig().set("server-list." + (args[0].equalsIgnoreCase("online") ? "online-players" : "max-players"), count);
            plugin.saveConfig();
            sender.sendMessage(LegacyText.colorize("&aDisplayed player count updated."));
        } catch (NumberFormatException exception) {
            sender.sendMessage(LegacyText.colorize("&cCount must be a non-negative number."));
        }
        return true;
    }

    private boolean editIcon(final CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /editicon <imageUrl>");
            return true;
        }
        final String url = args[0];
        sender.sendMessage(LegacyText.colorize("&eDownloading and converting server icon..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    File icon = new File(plugin.getDataFolder(), "server-icon.png");
                    downloadIcon(url, icon);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getConfig().set("server-list.cached-icon", new File(plugin.getDataFolder(), "server-icon.png").getAbsolutePath());
                            plugin.saveConfig();
                            sender.sendMessage(LegacyText.colorize("&aServer icon updated and cached."));
                        }
                    });
                } catch (Exception exception) {
                    sender.sendMessage(LegacyText.colorize("&cIcon update failed: " + exception.getMessage()));
                }
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
            for (NpcData data : npcManager.all()) {
                names.append(data.name).append(data.enabled ? " &7" : " &8").append(" ");
            }
            sender.sendMessage(LegacyText.colorize(names.toString()));
            return true;
        }
        if (action.equals("save")) {
            npcManager.save();
            sender.sendMessage(LegacyText.colorize("&aNPCs saved."));
            return true;
        }
        if (action.equals("reload")) {
            npcManager.load();
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
            NpcData data = npcManager.create(args[1], player.getLocation());
            sender.sendMessage(data == null ? "An NPC with that name already exists." : LegacyText.colorize("&aNPC created."));
            npcManager.save();
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("NPC name is required.");
            return true;
        }
        String npcName = args[1];
        NpcData data = npcManager.get(npcName);
        if (data == null) {
            sender.sendMessage("NPC not found.");
            return true;
        }
        if (action.equals("remove")) {
            npcManager.remove(npcName);
        } else if (action.equals("move")) {
            npcManager.move(npcName, player.getLocation());
        } else if (action.equals("tp")) {
            player.teleport(new Location(Bukkit.getWorld(data.world), data.x, data.y, data.z, data.yaw, data.pitch));
        } else if (action.equals("enable") || action.equals("disable")) {
            npcManager.setEnabled(npcName, action.equals("enable"));
        } else if (action.equals("info")) {
            sender.sendMessage(LegacyText.colorize("&a" + data.name + " &7(" + data.world + " " + data.x + ", " + data.y + ", " + data.z + ") skin=" + data.skin));
        } else if (action.equals("skin") && args.length >= 3) {
            fetchSkin(data, args[2], sender);
        } else if (action.equals("name") && args.length >= 3) {
            data.displayName = join(args, 2);
            ArmorStand stand = npcManager.entity(data.name);
            if (stand != null) {
                stand.setCustomName(LegacyText.colorize(data.displayName));
            }
        } else if (action.equals("nametag") || action.equals("glow") || action.equals("look")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /npc " + action + " <name> <true|false>");
                return true;
            }
            boolean value = Boolean.parseBoolean(args[2]);
            if (action.equals("nametag")) data.nametag = value;
            if (action.equals("glow")) data.glow = value;
            if (action.equals("look")) data.look = value;
            ArmorStand stand = npcManager.entity(data.name);
            if (stand != null) {
                if (action.equals("nametag")) stand.setCustomNameVisible(value);
                if (action.equals("glow")) MethodBridge.invokeIfPresent(stand, "setGlowing", new Class<?>[]{boolean.class}, new Object[]{value});
            }
        } else if ((action.equals("rightclick") || action.equals("leftclick")) && args.length >= 4) {
            addAction(data, action, args);
        } else if (action.equals("particle") && args.length == 3) {
            spawnParticle(data, args[2]);
        } else {
            sender.sendMessage("Invalid NPC operation or arguments.");
            return true;
        }
        npcManager.save();
        sender.sendMessage(LegacyText.colorize("&aNPC updated."));
        return true;
    }

    private void addAction(NpcData data, String click, String[] args) {
        String type = args[2].toLowerCase(Locale.ENGLISH);
        if (!type.equals("message") && !type.equals("command") && !type.equals("console") && !type.equals("sound")) {
            return;
        }
        data.actions.get(click).add(type + "|" + join(args, 3));
    }

    private void spawnParticle(NpcData data, String particleName) {
        try {
            ArmorStand stand = npcManager.entity(data.name);
            Object particle = Enum.valueOf((Class) Class.forName("org.bukkit.Particle"), particleName.toUpperCase(Locale.ENGLISH));
            MethodBridge.invoke(stand.getWorld(), "spawnParticle", new Class<?>[]{Class.forName("org.bukkit.Particle"), Location.class, int.class}, new Object[]{particle, stand.getLocation(), 10});
        } catch (Throwable exception) {
            plugin.getLogger().warning("Particle is not available on this server: " + particleName);
        }
    }

    private void fetchSkin(final NpcData data, final String skinName, final CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + skinName).openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    if (connection.getResponseCode() != 200) throw new IllegalStateException("Mojang profile not found");
                    data.skin = skinName;
                    npcManager.save();
                    sender.sendMessage(LegacyText.colorize("&aSkin metadata updated for " + skinName + "."));
                } catch (Exception exception) {
                    sender.sendMessage(LegacyText.colorize("&cSkin lookup failed: " + exception.getMessage()));
                }
            }
        });
    }

    private static Integer getPing(Player player) {
        try {
            Object value = player.getClass().getMethod("getPing").invoke(player);
            return value instanceof Number ? ((Number) value).intValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void downloadIcon(String url, File target) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "RomeoPaperPlugin/1.0");
        try (InputStream input = connection.getInputStream()) {
            BufferedImage source = ImageIO.read(input);
            if (source == null) throw new IllegalArgumentException("URL is not an image");
            BufferedImage output = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            graphics.drawImage(source.getScaledInstance(64, 64, Image.SCALE_SMOOTH), 0, 0, null);
            graphics.dispose();
            target.getParentFile().mkdirs();
            ImageIO.write(output, "PNG", target);
        }
    }

    private static String join(String[] args, int start) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) result.append(' ');
            result.append(args[i]);
        }
        return result.toString();
    }

    private static final class MethodBridge {
        static void invoke(Object target, String name, Class<?>[] types, Object[] values) throws Exception {
            target.getClass().getMethod(name, types).invoke(target, values);
        }

        static void invokeIfPresent(Object target, String name, Class<?>[] types, Object[] values) {
            try {
                invoke(target, name, types, values);
            } catch (Throwable ignored) {
            }
        }
    }
}
