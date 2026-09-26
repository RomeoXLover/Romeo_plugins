package com.romeo.paper;

import com.romeo.paper.services.NpcService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Server-level features: server list (MOTD, counts, icon) and NPC click
 * actions. Click-action execution is delegated to {@link NpcService} so the
 * GUI "Test Action" button runs exactly the same code path.
 */
final class ServerFeatureListener implements Listener {
    private final JavaPlugin plugin;
    private final NpcManager npcManager;
    private final NpcService npcService;

    ServerFeatureListener(JavaPlugin plugin, NpcManager npcManager, NpcService npcService) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.npcService = npcService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent event) {
        event.setMotd(LegacyText.colorize(plugin.getConfig().getString("server-list.motd", "Romeo Server")));
        event.setMaxPlayers(plugin.getConfig().getInt("server-list.max-players", event.getMaxPlayers()));
        Object online = plugin.getConfig().get("server-list.online-players");
        if (online instanceof Number) {
            invoke(event, "setNumPlayers", new Class<?>[]{int.class}, new Object[]{((Number) online).intValue()});
        }
        Object icon = plugin.getConfig().get("server-list.cached-icon");
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
                npcService.executeActions(event.getPlayer(), name, "rightclick");
            }
        }
    }

    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand && event.getDamager() instanceof Player) {
            String name = findName((ArmorStand) event.getEntity());
            if (name != null) {
                event.setCancelled(true);
                npcService.executeActions((Player) event.getDamager(), name, "leftclick");
            }
        }
    }

    private String findName(ArmorStand stand) {
        for (NpcData data : npcManager.all()) {
            if (npcManager.entity(data.getName()) == stand) {
                return data.getName();
            }
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
