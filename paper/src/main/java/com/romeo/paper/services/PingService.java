package com.romeo.paper.services;

import org.bukkit.entity.Player;

/**
 * Shared latency lookup used by {@code /ping} and the Commands → Utility GUI
 * button. Reflection keeps compatibility across server versions.
 */
public final class PingService {
    /** Player latency in ms, or null when the server does not expose it. */
    public Integer ping(Player player) {
        try {
            Object value = player.getClass().getMethod("getPing").invoke(player);
            return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Formatted one-line result for chat/GUI messages. */
    public String formatted(Player player) {
        Integer ping = ping(player);
        return ping == null ? "unavailable" : ping + " ms";
    }
}
