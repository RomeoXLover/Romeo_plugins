package com.romeo.paper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ServerFeatureListener implements Listener {
    private final JavaPlugin plugin;
    private final NpcManager npcManager;

    ServerFeatureListener(JavaPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent event) {
        event.setMotd(plugin.getConfig().getString("server-list.motd", "Romeo Server"));
        event.setMaxPlayers(plugin.getConfig().getInt("server-list.max-players", event.getMaxPlayers()));
        Integer online = getInteger(plugin.getConfig().get("server-list.online-players"));
        if (online != null) {
            invoke(event, "setNumPlayers", new Class<?>[]{int.class}, new Object[]{online});
        }
        Object icon = plugin.getDataFolder().getAbsolutePath().length() == 0 ? null : plugin.getConfig().get("server-list.cached-icon");
        if (icon instanceof String) {
            try {
                Object cached = Bukkit.getServer().loadServerIcon(new java.io.File((String) icon));
                for (Method method : event.getClass().getMethods()) {
                    if (method.getName().equals("setServerIcon") && method.getParameterTypes().length == 1
                            && method.getParameterTypes()[0].isInstance(cached)) {
                        method.invoke(event, cached);
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand) {
            String name = findName((ArmorStand) event.getRightClicked());
            if (name != null) {
                event.setCancelled(true);
                executeActions(event.getPlayer(), name, "rightclick");
            }
        }
    }

    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand && event.getDamager() instanceof Player) {
            String name = findName((ArmorStand) event.getEntity());
            if (name != null) {
                event.setCancelled(true);
                executeActions((Player) event.getDamager(), name, "leftclick");
            }
        }
    }

    private String findName(ArmorStand stand) {
        for (NpcData data : npcManager.all()) {
            if (npcManager.entity(data.name) == stand) {
                return data.name;
            }
        }
        return null;
    }

    private void executeActions(Player player, String npcName, String click) {
        NpcData data = npcManager.get(npcName);
        if (data == null || !data.enabled) {
            return;
        }
        List<String> actions = data.actions.get(click);
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
                    player.getWorld().playSound(npcManager.entity(npcName).getLocation(), sound, 1.0f, 1.0f);
                }
            } catch (Throwable exception) {
                plugin.getLogger().warning("NPC action failed for " + npcName + ": " + exception.getMessage());
            }
        }
    }

    private static Integer getInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    static void invoke(Object target, String name, Class<?>[] types, Object[] values) {
        try {
            Method method = target.getClass().getMethod(name, types);
            method.invoke(target, values);
        } catch (Throwable ignored) {
        }
    }
}
