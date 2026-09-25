package com.romeo.paper;

import com.romeo.common.api.PluginLogger;
import com.romeo.common.core.BasePluginLifecycle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RomeoPaperPlugin extends JavaPlugin {
    private NpcManager npcManager;
    private SettingsGUI settingsGUI;
    private OptionsScreen optionsScreen;
    private final BasePluginLifecycle lifecycle = new BasePluginLifecycle(new PaperLogger()) {
        @Override
        protected String getName() {
            return "RomeoPaperPlugin";
        }
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        npcManager = new NpcManager(this);
        npcManager.load();
        settingsGUI = new SettingsGUI(this, npcManager);
        optionsScreen = new OptionsScreen(this, settingsGUI, npcManager);
        RomeoCommand command = new RomeoCommand(this, npcManager, settingsGUI, optionsScreen);
        getCommand("ping").setExecutor(command);
        getCommand("editmotd").setExecutor(command);
        getCommand("editplayers").setExecutor(command);
        getCommand("editicon").setExecutor(command);
        getCommand("npc").setExecutor(command);
        getCommand("settings").setExecutor(command);
        Bukkit.getPluginManager().registerEvents(settingsGUI, this);
        Bukkit.getPluginManager().registerEvents(optionsScreen, this);
        Bukkit.getPluginManager().registerEvents(new ServerFeatureListener(this, npcManager), this);
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (NpcData data : npcManager.all()) {
                    if (!data.enabled || !data.look || npcManager.entity(data.name) == null) {
                        continue;
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().getName().equals(data.world)
                                && player.getLocation().distanceSquared(npcManager.entity(data.name).getLocation()) <= 100.0D) {
                            NpcManager.lookAt(npcManager.entity(data.name), player);
                            break;
                        }
                    }
                }
            }
        }, 10L, 10L);
        lifecycle.onEnable();
        getLogger().info("Romeo Paper plugin enabled.");
    }

    @Override
    public void onDisable() {
        lifecycle.onDisable();
        getLogger().info("Romeo Paper plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
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
