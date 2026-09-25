package com.romeo.paper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class NpcData {
    final String name;
    String world;
    double x;
    double y;
    double z;
    float yaw;
    float pitch;
    String skin = "none";
    String displayName;
    boolean nametag = true;
    boolean glow;
    boolean look;
    boolean enabled = true;
    final Map<String, List<String>> actions = new LinkedHashMap<String, List<String>>();

    NpcData(String name) {
        this.name = name;
        actions.put("rightclick", new ArrayList<String>());
        actions.put("leftclick", new ArrayList<String>());
    }
}
