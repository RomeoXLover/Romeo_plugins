package com.romeo.paper.services;

import com.romeo.paper.NpcManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wiring holder for the shared services layer. Built once in
 * {@code RomeoPaperPlugin#onEnable} and passed to both the command layer and
 * the GUI layer so every frontend shares the same backend instances.
 */
public final class Services {
    private final ConfigService config;
    private final MotdService motd;
    private final PlayerCountService playerCount;
    private final IconService icon;
    private final PingService ping;
    private final NpcService npc;

    public Services(JavaPlugin plugin, NpcManager npcManager) {
        this.config = new ConfigService(plugin);
        this.motd = new MotdService(config);
        this.playerCount = new PlayerCountService(config);
        this.icon = new IconService(plugin, config);
        this.ping = new PingService();
        this.npc = new NpcService(plugin, npcManager);
    }

    public ConfigService config() {
        return config;
    }

    public MotdService motd() {
        return motd;
    }

    public PlayerCountService playerCount() {
        return playerCount;
    }

    public IconService icon() {
        return icon;
    }

    public PingService ping() {
        return ping;
    }

    public NpcService npc() {
        return npc;
    }
}
