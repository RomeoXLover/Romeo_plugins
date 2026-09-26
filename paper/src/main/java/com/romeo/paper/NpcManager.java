package com.romeo.paper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Storage + entity lifecycle for ArmorStand-backed NPCs. Persistence only —
 * higher-level behavior lives in {@link com.romeo.paper.services.NpcService}.
 */
public final class NpcManager {
    private final JavaPlugin plugin;
    private final Map<String, NpcData> npcs = new LinkedHashMap<String, NpcData>();
    private final Map<String, ArmorStand> entities = new LinkedHashMap<String, ArmorStand>();
    private final File file;
    private FileConfiguration configuration;

    public NpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void load() {
        removeEntities();
        npcs.clear();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Unable to create npcs.yml: " + exception.getMessage());
            }
        }
        configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = configuration.getConfigurationSection("npcs");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            NpcData data = new NpcData(key);
            data.loadFrom(
                    section.getString("world", ""),
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch"),
                    section.getString("skin", "none"),
                    section.getString("display-name"),
                    section.getBoolean("nametag", true),
                    section.getBoolean("glow", false),
                    section.getBoolean("look", false),
                    section.getBoolean("enabled", true),
                    section.getStringList("actions.rightclick"),
                    section.getStringList("actions.leftclick"));
            npcs.put(normalize(key), data);
            if (data.isEnabled()) {
                spawn(data);
            }
        }
    }

    public void save() {
        configuration = new YamlConfiguration();
        ConfigurationSection root = configuration.createSection("npcs");
        for (NpcData data : npcs.values()) {
            ConfigurationSection section = root.createSection(data.getName());
            section.set("world", data.getWorld());
            section.set("x", data.getX());
            section.set("y", data.getY());
            section.set("z", data.getZ());
            section.set("yaw", data.getYaw());
            section.set("pitch", data.getPitch());
            section.set("skin", data.getSkin());
            section.set("display-name", data.getDisplayName());
            section.set("nametag", data.isNametag());
            section.set("glow", data.isGlow());
            section.set("look", data.isLook());
            section.set("enabled", data.isEnabled());
            section.set("actions.rightclick", data.getActions("rightclick"));
            section.set("actions.leftclick", data.getActions("leftclick"));
        }
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to save npcs.yml: " + exception.getMessage());
        }
    }

    public NpcData create(String name, Location location) {
        String key = normalize(name);
        if (npcs.containsKey(key)) {
            return null;
        }
        NpcData data = new NpcData(name);
        updateLocation(data, location);
        npcs.put(key, data);
        spawn(data);
        return data;
    }

    public boolean remove(String name) {
        String key = normalize(name);
        NpcData removed = npcs.remove(key);
        if (removed == null) {
            return false;
        }
        removeEntity(key);
        return true;
    }

    public NpcData get(String name) {
        return npcs.get(normalize(name));
    }

    public Collection<NpcData> all() {
        return npcs.values();
    }

    public ArmorStand entity(String name) {
        return entities.get(normalize(name));
    }

    public void move(String name, Location location) {
        NpcData data = get(name);
        if (data != null) {
            updateLocation(data, location);
            ArmorStand stand = entities.get(normalize(name));
            if (stand != null) {
                stand.teleport(location);
            } else if (data.isEnabled()) {
                spawn(data);
            }
        }
    }

    public void setEnabled(String name, boolean enabled) {
        NpcData data = get(name);
        if (data == null) {
            return;
        }
        data.setEnabled(enabled);
        if (enabled) {
            spawn(data);
        } else {
            removeEntity(normalize(name));
        }
    }

    private void spawn(NpcData data) {
        World world = Bukkit.getWorld(data.getWorld());
        if (world == null) {
            plugin.getLogger().warning("World not loaded for NPC " + data.getName() + ": " + data.getWorld());
            return;
        }
        removeEntity(normalize(data.getName()));
        Location location = new Location(world, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomNameVisible(data.isNametag());
        stand.setCustomName(LegacyText.colorize(data.getDisplayName() == null ? data.getName() : data.getDisplayName()));
        tryInvoke(stand, "setMarker", new Class<?>[]{boolean.class}, new Object[]{true});
        tryInvoke(stand, "setGlowing", new Class<?>[]{boolean.class}, new Object[]{data.isGlow()});
        applySkin(stand, data.getSkin());
        entities.put(normalize(data.getName()), stand);
    }

    private void applySkin(ArmorStand stand, String skinName) {
        if (skinName == null || skinName.equalsIgnoreCase("none")) {
            return;
        }
        try {
            Material material;
            try {
                material = Material.valueOf("PLAYER_HEAD");
            } catch (IllegalArgumentException ignored) {
                material = Material.valueOf("SKULL_ITEM");
            }
            ItemStack head = new ItemStack(material, 1);
            if (head.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwner(skinName);
                head.setItemMeta(meta);
                stand.setHelmet(head);
            }
        } catch (Throwable exception) {
            plugin.getLogger().fine("Player-head skin is unavailable for NPC " + stand.getCustomName());
        }
    }

    private void removeEntities() {
        for (ArmorStand entity : new ArrayList<ArmorStand>(entities.values())) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        entities.clear();
    }

    private void removeEntity(String key) {
        ArmorStand entity = entities.remove(key);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    private void updateLocation(NpcData data, Location location) {
        data.setLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ENGLISH);
    }

    private static void tryInvoke(Object target, String method, Class<?>[] types, Object[] values) {
        try {
            target.getClass().getMethod(method, types).invoke(target, values);
        } catch (Throwable ignored) {
        }
    }

    static void lookAt(ArmorStand stand, Player player) {
        Location from = stand.getLocation();
        Location to = player.getEyeLocation();
        double x = to.getX() - from.getX();
        double z = to.getZ() - from.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        from.setYaw(yaw);
        stand.teleport(from);
    }

    /** Reflection helper shared with the services layer. */
    public static void invoke(Object target, String name, Class<?>[] types, Object[] values) throws Exception {
        target.getClass().getMethod(name, types).invoke(target, values);
    }

    /** Reflection helper that never throws; used for version-specific API. */
    public static void invokeIfPresent(Object target, String name, Class<?>[] types, Object[] values) {
        try {
            invoke(target, name, types, values);
        } catch (Throwable ignored) {
        }
    }
}
