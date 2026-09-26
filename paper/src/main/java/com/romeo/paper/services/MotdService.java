package com.romeo.paper.services;

/**
 * Shared business logic for the server list MOTD.
 *
 * Used by both the {@code /editmotd} command and the settings GUI — never
 * duplicate this logic in a second place.
 */
public final class MotdService {
    public static final String CONFIG_KEY = "server-list.motd";
    public static final String DEFAULT_MOTD = "Romeo Server";

    private final ConfigService config;

    public MotdService(ConfigService config) {
        this.config = config;
    }

    /** Current MOTD, falling back to the built-in default. */
    public String getMotd() {
        return config.getString(CONFIG_KEY, DEFAULT_MOTD);
    }

    /** Stores a new MOTD. Input must be trimmed and non-empty before calling. */
    public boolean setMotd(String motd) {
        if (motd == null || motd.trim().isEmpty()) {
            return false;
        }
        return config.set(CONFIG_KEY, motd.trim());
    }

    /** Restores the default MOTD. */
    public boolean reset() {
        return config.set(CONFIG_KEY, DEFAULT_MOTD);
    }

    /** How the MOTD will look in the server list (color codes applied). */
    public String preview() {
        return com.romeo.paper.LegacyText.colorize(getMotd());
    }
}
