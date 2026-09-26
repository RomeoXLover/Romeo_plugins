package com.romeo.paper.services;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Single owner of the plugin configuration file.
 *
 * Every read/write of config.yml goes through here so that there is exactly one
 * save path and one reload path. Services and commands never call
 * {@code plugin.getConfig().set(...)} / {@code plugin.saveConfig()} directly.
 */
public final class ConfigService {
    private final JavaPlugin plugin;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Reads a string value with a fallback when the key is absent. */
    public String getString(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    /** Reads an integer value with a fallback when the key is absent. */
    public int getInt(String path, int fallback) {
        return plugin.getConfig().getInt(path, fallback);
    }

    /** Reads a value that may be absent or null (e.g. unset online-players). */
    public Object getRaw(String path) {
        return plugin.getConfig().get(path);
    }

    /** Persists a value to config.yml. Returns false when saving failed. */
    public boolean set(String path, Object value) {
        try {
            plugin.getConfig().set(path, value);
            plugin.saveConfig();
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to save config value " + path + ": " + exception.getMessage());
            return false;
        }
    }

    /** Removes a key so the default (or null) applies again. */
    public boolean clear(String path) {
        return set(path, null);
    }

    /** Re-reads config.yml from disk. Returns false when the reload failed. */
    public boolean reload() {
        try {
            plugin.reloadConfig();
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to reload config: " + exception.getMessage());
            return false;
        }
    }
}
