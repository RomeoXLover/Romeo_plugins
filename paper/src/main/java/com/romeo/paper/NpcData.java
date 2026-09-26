package com.romeo.paper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted NPC state. Fields are private with accessors so the services
 * layer can operate on NPCs from another package.
 */
public final class NpcData {
    private final String name;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String skin = "none";
    private String displayName;
    private boolean nametag = true;
    private boolean glow;
    private boolean look;
    private boolean enabled = true;
    private final Map<String, List<String>> actions = new LinkedHashMap<String, List<String>>();

    public NpcData(String name) {
        this.name = name;
        actions.put("rightclick", new ArrayList<String>());
        actions.put("leftclick", new ArrayList<String>());
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getSkin() {
        return skin;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isNametag() {
        return nametag;
    }

    public boolean isGlow() {
        return glow;
    }

    public boolean isLook() {
        return look;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Mutable accessors used by the service layer. */
    public void setLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setNametag(boolean nametag) {
        this.nametag = nametag;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public void setLook(boolean look) {
        this.look = look;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Action list for "rightclick" or "leftclick". */
    public List<String> getActions(String click) {
        return actions.get(click);
    }

    /** Package-private bulk load used by NpcManager when reading npcs.yml. */
    void loadFrom(String world, double x, double y, double z, float yaw, float pitch,
                  String skin, String displayName, boolean nametag, boolean glow, boolean look,
                  boolean enabled, List<String> rightClick, List<String> leftClick) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.skin = skin;
        this.displayName = displayName;
        this.nametag = nametag;
        this.glow = glow;
        this.look = look;
        this.enabled = enabled;
        actions.get("rightclick").addAll(rightClick);
        actions.get("leftclick").addAll(leftClick);
    }
}
