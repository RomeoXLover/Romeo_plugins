package com.romeo.paper;

import com.romeo.common.api.PluginLogger;
import com.romeo.common.core.BasePluginLifecycle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class RomeoPaperPlugin extends JavaPlugin {
    private final BasePluginLifecycle lifecycle = new BasePluginLifecycle(new PaperLogger()) {
        @Override
        protected String getName() {
            return "RomeoPaperPlugin";
        }
    };

    @Override
    public void onEnable() {
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
        if (command.getName().equalsIgnoreCase("testplugin")) {
            sender.sendMessage(LegacyText.colorize("&a[Romeo] Test plugin is active on Paper/Bukkit."));
            return true;
        }
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
