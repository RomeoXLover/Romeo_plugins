package com.romeo.paper;

import com.romeo.common.api.PluginLogger;
import com.romeo.common.core.BasePluginLifecycle;
import com.romeo.paper.services.Services;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RomeoPaperPlugin extends JavaPlugin {
    private NpcManager npcManager;
    private Services services;
    private final BasePluginLifecycle lifecycle = new BasePluginLifecycle(new PaperLogger()) {
        @Override
        protected String getName() {
            return "RomeoPaperPlugin";
        }
    };

    @Override
    public void onEnable() {
        getLogger().info("Starting...");
        saveDefaultConfig();
        npcManager = new NpcManager(this);
        services = new Services(this, npcManager);
        services.input().register();
        getLogger().info("Input system registered.");
        npcManager.load();
        getLogger().info("NPC system loaded (" + npcManager.all().size() + " NPCs).");
        RomeoCommand command = new RomeoCommand(services);
        getCommand("ping").setExecutor(command);
        getCommand("editmotd").setExecutor(command);
        getCommand("editplayers").setExecutor(command);
        getCommand("editicon").setExecutor(command);
        getCommand("npc").setExecutor(command);
        getCommand("settings").setExecutor(command);
        getLogger().info("Commands registered.");
        Bukkit.getPluginManager().registerEvents(
                new ServerFeatureListener(this, npcManager, services.npc()), this);
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (NpcData data : npcManager.all()) {
                    if (!data.isEnabled() || !data.isLook() || npcManager.entity(data.getName()) == null) {
                        continue;
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().getName().equals(data.getWorld())
                                && player.getLocation().distanceSquared(npcManager.entity(data.getName()).getLocation()) <= 100.0D) {
                            NpcManager.lookAt(npcManager.entity(data.getName()), player);
                            break;
                        }
                    }
                }
            }
        }, 10L, 10L);
        lifecycle.onEnable();
        getLogger().info("Ready.");
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.input().unregister();
        }
        if (npcManager != null) {
            npcManager.save();
        }
        lifecycle.onDisable();
        getLogger().info("Romeo Paper plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    public Services services() {
        return services;
    }

    private static final class PaperLogger implements PluginLogger {
        @Override
        public void info(String message) {
            java.util.logging.Logger.getLogger("RomeoPaperPlugin").info(message);
        }

        @Override
        public void warn(String message) {
            java.util.logging.Logger.getLogger("RomeoPaperPlugin").warning(message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            java.util.logging.Logger.getLogger("RomeoPaperPlugin").severe(message);
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }
}
