package com.romeo.paper.services;

/**
 * Shared business logic for the displayed online/max player counts in the
 * server list. Used by {@code /editplayers} and the settings GUI.
 */
public final class PlayerCountService {
    public static final String ONLINE_KEY = "server-list.online-players";
    public static final String MAX_KEY = "server-list.max-players";

    private final ConfigService config;

    public PlayerCountService(ConfigService config) {
        this.config = config;
    }

    /** Configured fake online count, or null when the real count is shown. */
    public Integer getOnlineDisplay() {
        return asInt(config.getRaw(ONLINE_KEY));
    }

    /** Configured max display count. */
    public int getMaxDisplay() {
        return config.getInt(MAX_KEY, 20);
    }

    /** Sets the fake online count. Returns false when the value is invalid. */
    public boolean setOnlineDisplay(int count) {
        return setCount(ONLINE_KEY, count);
    }

    /** Sets the displayed max player count. Returns false when invalid. */
    public boolean setMaxDisplay(int count) {
        return setCount(MAX_KEY, count);
    }

    /** Clears the fake online count so the real count shows again. */
    public boolean resetOnline() {
        return config.clear(ONLINE_KEY);
    }

    /** Restores the max count default (20). */
    public boolean resetMax() {
        return config.set(MAX_KEY, 20);
    }

    private boolean setCount(String key, int count) {
        if (count < 0) {
            return false;
        }
        return config.set(key, count);
    }

    private static Integer asInt(Object raw) {
        return raw instanceof Number ? Integer.valueOf(((Number) raw).intValue()) : null;
    }
}
