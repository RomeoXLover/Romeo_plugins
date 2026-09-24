package com.romeo.paper;

import org.bukkit.ChatColor;

public final class LegacyText {
    private LegacyText() {
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
