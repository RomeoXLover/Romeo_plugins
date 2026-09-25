package com.romeo.paper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
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

final class NpcManager {
    private final JavaPlugin plugin;
    private final Map<String, NpcData> npcs = new LinkedHashMap<String, NpcData>();
    private final Map<String, ArmorStand> entities = new LinkedHashMap<String, ArmorStand>();
    private final File file;
    private FileConfiguration configuration;

    NpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    void load() {
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
            data.world = section.getString("world", "");
            data.x = section.getDouble("x");
            data.y = section.getDouble("y");
            data.z = section.getDouble("z");
            data.yaw = (float) section.getDouble("yaw");
            data.pitch = (float) section.getDouble("pitch");
            data.skin = section.getString("skin", "none");
            data.displayName = section.getString("display-name");
            data.nametag = section.getBoolean("nametag", true);
            data.glow = section.getBoolean("glow", false);
            data.look = section.getBoolean("look", false);
            data.enabled = section.getBoolean("enabled", true);
            data.actions.get("rightclick").addAll(section.getStringList("actions.rightclick"));
            data.actions.get("leftclick").addAll(section.getStringList("actions.leftclick"));
            npcs.put(normalize(key), data);
            if (data.enabled) {
                spawn(data);
            }
        }
    }

    void save() {
        configuration = new YamlConfiguration();
        ConfigurationSection root = configuration.createSection("npcs");
        for (NpcData data : npcs.values()) {
            ConfigurationSection section = root.createSection(data.name);
            section.set("world", data.world);
            section.set("x", data.x);
            section.set("y", data.y);
            section.set("z", data.z);
            section.set("yaw", data.yaw);
            section.set("pitch", data.pitch);
            section.set("skin", data.skin);
            section.set("display-name", data.displayName);
            section.set("nametag", data.nametag);
            section.set("glow", data.glow);
            section.set("look", data.look);
            section.set("enabled", data.enabled);
            section.set("actions.rightclick", data.actions.get("rightclick"));
            section.set("actions.leftclick", data.actions.get("leftclick"));
        }
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to save npcs.yml: " + exception.getMessage());
        }
    }

    NpcData create(String name, Location location) {
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

    boolean remove(String name) {
        String key = normalize(name);
        NpcData removed = npcs.remove(key);
        if (removed == null) {
            return false;
        }
        removeEntity(key);
        return true;
    }

    NpcData get(String name) {
        return npcs.get(normalize(name));
    }

    Collection<NpcData> all() {
        return npcs.values();
    }

    ArmorStand entity(String name) {
        return entities.get(normalize(name));
    }

    void move(String name, Location location) {
        NpcData data = get(name);
        if (data != null) {
            updateLocation(data, location);
            ArmorStand stand = entities.get(normalize(name));
            if (stand != null) {
                stand.teleport(location);
            } else if (data.enabled) {
                spawn(data);
            }
        }
    }

    void setEnabled(String name, boolean enabled) {
        NpcData data = get(name);
        if (data == null) {
            return;
        }
        data.enabled = enabled;
        if (enabled) {
            spawn(data);
        } else {
            removeEntity(normalize(name));
        }
    }

    private void spawn(NpcData data) {
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            plugin.getLogger().warning("World not loaded for NPC " + data.name + ": " + data.world);
            return;
        }
        removeEntity(normalize(data.name));
        Location location = new Location(world, data.x, data.y, data.z, data.yaw, data.pitch);
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomNameVisible(data.nametag);
        stand.setCustomName(LegacyText.colorize(data.displayName == null ? data.name : data.displayName));
        tryInvoke(stand, "setMarker", new Class<?>[]{boolean.class}, new Object[]{true});
        tryInvoke(stand, "setGlowing", new Class<?>[]{boolean.class}, new Object[]{data.glow});
        applySkin(stand, data.skin);
        entities.put(normalize(data.name), stand);
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
        data.world = location.getWorld().getName();
        data.x = location.getX();
        data.y = location.getY();
        data.z = location.getZ();
        data.yaw = location.getYaw();
        data.pitch = location.getPitch();
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
}
