package com.romeo.paper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class SettingsGUI implements Listener {
    private static final String MAIN = ChatColor.DARK_GRAY + "Plugin Settings";
    private static final String COMMANDS = ChatColor.DARK_GRAY + "Settings / Commands";
    private static final String COMMAND = ChatColor.DARK_GRAY + "Command Settings";
    private static final String CATEGORY_PREFIX = ChatColor.DARK_GRAY + "Settings / ";
    private static final String NPCS = ChatColor.DARK_GRAY + "Settings / NPCs";
    private static final String NPC = ChatColor.DARK_GRAY + "NPC Settings";
    private static final String CONFIRM = ChatColor.DARK_RED + "Confirm destructive action";
    private static final List<String> COMMAND_NAMES = Arrays.asList(
            "testplugin", "ping", "editmotd", "editplayers", "editicon", "npc");

    private final RomeoPaperPlugin plugin;
    private final NpcManager npcManager;
    private final Map<UUID, String> selectedCommand = new HashMap<UUID, String>();
    private final Map<UUID, String> selectedNpc = new HashMap<UUID, String>();
    private final Map<UUID, String> searchTerms = new HashMap<UUID, String>();
    private final Map<UUID, Prompt> prompts = new HashMap<UUID, Prompt>();
    private final Map<UUID, String> deleteConfirmations = new HashMap<UUID, String>();

    SettingsGUI(RomeoPaperPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    void open(Player player) {
        if (!requireAdmin(player)) {
            return;
        }
        openMain(player);
    }

    private void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, MAIN);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        button(inventory, 20, Material.BOOK, "&bCommands", "&7Manage every registered command");
        button(inventory, 21, Material.REDSTONE, "&dFeatures", "&7Server features and controls");
        button(inventory, 29, Material.EYE_OF_ENDER, "&5Visuals", "&7NPC display and visual settings");
        button(inventory, 30, Material.DIAMOND, "&aGameplay", "&7Gameplay controls");
        button(inventory, 38, Material.COMPASS, "&eUtilities", "&7Tools and server utilities");
        button(inventory, 39, Material.NETHER_STAR, "&fMisc", "&7Other plugin settings");
        button(inventory, 49, Material.NAME_TAG, "&bSearch settings", "&7Find a command or NPC quickly");
        button(inventory, 53, Material.BARRIER, "&cClose", "&7Close this panel");
        player.openInventory(inventory);
    }

    private void openCategory(Player player, String category, List<String> names) {
        Inventory inventory = Bukkit.createInventory(null, 54, CATEGORY_PREFIX + category);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        button(inventory, 4, Material.BOOK, "&b" + category, "&7Existing controls in this category");
        int slot = 20;
        for (String name : names) {
            button(inventory, slot++, commandMaterial(name), "&b/" + name, commandDescription(name));
        }
        button(inventory, 45, Material.ARROW, "&eBack", "&7Return to the main menu");
        button(inventory, 49, Material.REDSTONE, "&dHome", "&7Return to plugin settings");
        button(inventory, 53, Material.BARRIER, "&cClose", "&7Close this panel");
        player.openInventory(inventory);
    }

    private void openCommands(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, COMMANDS);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        String filter = searchTerms.get(player.getUniqueId());
        int slot = 10;
        for (String name : COMMAND_NAMES) {
            if (filter != null && !name.contains(filter.toLowerCase(Locale.ENGLISH))) {
                continue;
            }
            button(inventory, slot, commandMaterial(name), "&b/" + name, commandDescription(name));
            slot++;
            if (slot == 17 || slot == 26 || slot == 35 || slot == 44) {
                slot += 2;
            }
        }
        if (filter != null && !filter.isEmpty()) {
            button(inventory, 4, Material.NAME_TAG, "&bSearch: &f" + filter, "&7Click to search again");
        } else {
            button(inventory, 4, Material.BOOK, "&bCommands", "&7Select a command to configure it");
        }
        button(inventory, 45, Material.ARROW, "&eBack", "&7Return to the main menu");
        button(inventory, 49, Material.COMPASS, "&bSearch", "&7Search commands");
        button(inventory, 53, Material.BARRIER, "&cClose", "&7Close this panel");
        player.openInventory(inventory);
    }

    private void openCommand(Player player, String name) {
        selectedCommand.put(player.getUniqueId(), name);
        Inventory inventory = Bukkit.createInventory(null, 54, COMMAND);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        button(inventory, 4, commandMaterial(name), "&b/" + name, commandDescription(name));
        if (name.equals("editmotd")) {
            button(inventory, 20, Material.PAPER, "&fMOTD", "&7Current: &f" + plugin.getConfig().getString("server-list.motd", "Romeo Server"), "&eClick to edit");
        } else if (name.equals("editplayers")) {
            button(inventory, 20, Material.PAPER, "&fFake online count", "&7Current: &f" + displayedValue("online-players"), "&eClick to edit");
            button(inventory, 22, Material.PAPER, "&fFake max count", "&7Current: &f" + plugin.getConfig().getInt("server-list.max-players", 20), "&eClick to edit");
        } else if (name.equals("editicon")) {
            button(inventory, 20, Material.PAINTING, "&fServer icon", "&7Cached icon: &f" + iconStatus(), "&eClick to edit URL");
        } else if (name.equals("npc")) {
            button(inventory, 20, Material.ARMOR_STAND, "&bNPC control panel", "&7Open the NPC settings menu");
        } else {
            button(inventory, 20, Material.REDSTONE_TORCH_ON, "&aEnabled", "&7This command remains exactly as implemented", "&7Use /" + name + " in chat");
        }
        button(inventory, 45, Material.ARROW, "&eBack", "&7Return to commands");
        button(inventory, 49, Material.REDSTONE, "&dHome", "&7Return to plugin settings");
        button(inventory, 53, Material.BARRIER, "&cClose", "&7Close this panel");
        player.openInventory(inventory);
    }

    private void openNpcs(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, NPCS);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        int slot = 10;
        String filter = searchTerms.get(player.getUniqueId());
        for (NpcData data : npcManager.all()) {
            if (filter != null && !data.name.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))) {
                continue;
            }
            button(inventory, slot, Material.ARMOR_STAND, (data.enabled ? "&a" : "&8") + data.name,
                    "&7Skin: &f" + data.skin, "&7Click to configure");
            slot++;
            if (slot == 17 || slot == 26 || slot == 35 || slot == 44) {
                slot += 2;
            }
        }
        button(inventory, 45, Material.NAME_TAG, "&aCreate NPC", "&7Creates an NPC at your location");
        button(inventory, 47, Material.BOOK, "&fSave NPCs", "&7Persist all NPC settings now");
        button(inventory, 48, Material.PAPER, "&fReload NPCs", "&7Reload npcs.yml and respawn enabled NPCs");
        button(inventory, 49, Material.ARROW, "&eBack", "&7Return to commands");
        button(inventory, 51, Material.COMPASS, "&bSearch", "&7Search NPCs");
        button(inventory, 53, Material.BARRIER, "&cClose", "&7Close this panel");
        player.openInventory(inventory);
    }

    private void openNpc(Player player, String name) {
        NpcData data = npcManager.get(name);
        if (data == null) {
            openNpcs(player);
            return;
        }
        selectedNpc.put(player.getUniqueId(), data.name);
        Inventory inventory = Bukkit.createInventory(null, 54, NPC);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        button(inventory, 4, Material.ARMOR_STAND, "&b" + data.name, "&7" + data.world + " &f" + format(data.x) + ", " + format(data.y) + ", " + format(data.z));
        button(inventory, 19, data.enabled ? Material.EMERALD : Material.REDSTONE, "&fEnabled: " + onOff(data.enabled), "&eClick to toggle visibility/interactivity");
        button(inventory, 20, data.nametag ? Material.NAME_TAG : Material.PAPER, "&fNametag: " + onOff(data.nametag), "&eClick to toggle");
        button(inventory, 21, data.glow ? Material.GLOWSTONE : Material.COAL, "&fGlow: " + onOff(data.glow), "&eClick to toggle");
        button(inventory, 22, data.look ? Material.COMPASS : Material.WATCH, "&fLook at players: " + onOff(data.look), "&eClick to toggle");
        button(inventory, 23, Material.SKULL_ITEM, "&fSkin: &b" + data.skin, "&eClick to edit skin name");
        button(inventory, 24, Material.PAPER, "&fDisplay name", "&7" + (data.displayName == null ? data.name : data.displayName), "&eClick to edit");
        button(inventory, 28, Material.ENDER_PEARL, "&fMove here", "&eTeleport NPC to your location");
        button(inventory, 29, Material.EYE_OF_ENDER, "&fTeleport to NPC", "&eTeleport yourself to the NPC");
        button(inventory, 31, Material.REDSTONE, "&dRight-click actions", "&7" + data.actions.get("rightclick").size() + " configured", "&eClick to add action");
        button(inventory, 32, Material.REDSTONE_TORCH_ON, "&dLeft-click actions", "&7" + data.actions.get("leftclick").size() + " configured", "&eClick to add action");
        button(inventory, 33, Material.FIREWORK, "&dParticle", "&eClick to add a particle action");
        button(inventory, 40, Material.TNT, "&cRemove NPC", "&7Requires confirmation");
        button(inventory, 45, Material.ARROW, "&eBack", "&7Return to NPCs");
        button(inventory, 49, Material.REDSTONE, "&dHome", "&7Return to plugin settings");
        button(inventory, 53, Material.BARRIER, "&cClose", "&7Close this panel");
        player.openInventory(inventory);
    }

    private void openConfirm(Player player, String name) {
        deleteConfirmations.put(player.getUniqueId(), name);
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRM);
        fill(inventory, Material.STAINED_GLASS_PANE, 15, " ");
        button(inventory, 11, Material.TNT, "&cRemove &f" + name, "&7This cannot be undone");
        button(inventory, 15, Material.ARROW, "&aCancel", "&7Keep this NPC");
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTopInventory().getTitle();
        if (!isGui(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!requireAdmin(player)) {
            player.closeInventory();
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }
        String label = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        int slot = event.getRawSlot();
        if (title.equals(MAIN)) {
            handleMainClick(player, slot);
        } else if (title.equals(COMMANDS)) {
            handleCommandsClick(player, slot, label);
        } else if (title.equals(COMMAND)) {
            handleCommandClick(player, slot);
        } else if (title.startsWith(CATEGORY_PREFIX)) {
            handleCategoryClick(player, title, slot, label);
        } else if (title.equals(NPCS)) {
            handleNpcsClick(player, slot, label);
        } else if (title.equals(NPC)) {
            handleNpcClick(player, slot);
        } else if (title.equals(CONFIRM)) {
            handleConfirmClick(player, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        if (slot == 20) openCommands(player);
        else if (slot == 21) openCategory(player, "Features", Arrays.asList("editmotd", "editplayers", "editicon"));
        else if (slot == 29) openCategory(player, "Visuals", Arrays.asList("npc"));
        else if (slot == 30) openCategory(player, "Gameplay", Arrays.asList("npc", "testplugin"));
        else if (slot == 38) openCategory(player, "Utilities", Arrays.asList("ping", "npc"));
        else if (slot == 39) openCategory(player, "Misc", Arrays.asList("testplugin"));
        else if (slot == 49) prompt(player, Prompt.SEARCH_COMMAND, "Type a command or NPC search term in chat.");
        else if (slot == 53) player.closeInventory();
    }

    private void handleCategoryClick(Player player, String title, int slot, String label) {
        if (slot == 45 || slot == 49) {
            openMain(player);
        } else if (slot == 53) {
            player.closeInventory();
        } else if (label.startsWith("/")) {
            String name = label.substring(1);
            if (name.equals("npc")) {
                openNpcs(player);
            } else {
                openCommand(player, name);
            }
        }
    }

    private void handleCommandsClick(Player player, int slot, String label) {
        if (slot == 45) openMain(player);
        else if (slot == 49) prompt(player, Prompt.SEARCH_COMMAND, "Type a command search term in chat.");
        else if (slot == 53) player.closeInventory();
        else if (label.startsWith("/")) openCommand(player, label.substring(1));
    }

    private void handleCommandClick(Player player, int slot) {
        String name = selectedCommand.get(player.getUniqueId());
        if (name == null) {
            openCommands(player);
            return;
        }
        if (slot == 45) openCommands(player);
        else if (slot == 49) openMain(player);
        else if (slot == 53) player.closeInventory();
        else if (name.equals("editmotd") && slot == 20) prompt(player, Prompt.MOTD, "Type the new MOTD in chat. Use & color codes.");
        else if (name.equals("editplayers") && slot == 20) prompt(player, Prompt.ONLINE_COUNT, "Type the fake online player count in chat.");
        else if (name.equals("editplayers") && slot == 22) prompt(player, Prompt.MAX_COUNT, "Type the fake max player count in chat.");
        else if (name.equals("editicon") && slot == 20) prompt(player, Prompt.ICON_URL, "Type the image URL in chat.");
        else if (name.equals("npc") && slot == 20) openNpcs(player);
    }

    private void handleNpcsClick(Player player, int slot, String label) {
        if (slot == 45) prompt(player, Prompt.CREATE_NPC, "Type the new NPC name in chat.");
        else if (slot == 47) {
            npcManager.save();
            player.sendMessage(LegacyText.colorize("&aNPCs saved."));
        } else if (slot == 48) {
            npcManager.load();
            player.sendMessage(LegacyText.colorize("&aNPCs reloaded."));
            openNpcs(player);
        } else if (slot == 49) openMain(player);
        else if (slot == 51) prompt(player, Prompt.SEARCH_NPC, "Type an NPC search term in chat.");
        else if (slot == 53) player.closeInventory();
        else if (label != null && !label.isEmpty() && !label.equals(" ") && !label.startsWith("Create")) openNpc(player, label);
    }

    private void handleNpcClick(Player player, int slot) {
        String name = selectedNpc.get(player.getUniqueId());
        NpcData data = name == null ? null : npcManager.get(name);
        if (data == null) {
            openNpcs(player);
            return;
        }
        if (slot == 19) {
            npcManager.setEnabled(data.name, !data.enabled);
            npcManager.save();
            openNpc(player, data.name);
        } else if (slot == 20) {
            data.nametag = !data.nametag;
            ArmorStand stand = npcManager.entity(data.name);
            if (stand != null) stand.setCustomNameVisible(data.nametag);
            npcManager.save();
            openNpc(player, data.name);
        } else if (slot == 21) {
            data.glow = !data.glow;
            ArmorStand stand = npcManager.entity(data.name);
            if (stand != null) invoke(stand, "setGlowing", new Class<?>[]{boolean.class}, new Object[]{data.glow});
            npcManager.save();
            openNpc(player, data.name);
        } else if (slot == 22) {
            data.look = !data.look;
            npcManager.save();
            openNpc(player, data.name);
        } else if (slot == 23) prompt(player, Prompt.NPC_SKIN, "Type the Minecraft username for the NPC skin.");
        else if (slot == 24) prompt(player, Prompt.NPC_NAME, "Type the formatted display name. Use & color codes.");
        else if (slot == 28) {
            npcManager.move(data.name, player.getLocation());
            npcManager.save();
            openNpc(player, data.name);
        } else if (slot == 29) {
            player.teleport(new org.bukkit.Location(Bukkit.getWorld(data.world), data.x, data.y, data.z, data.yaw, data.pitch));
        } else if (slot == 31) prompt(player, Prompt.RIGHT_ACTION, "Type: message <text>, command <command>, console <command>, or sound <sound>");
        else if (slot == 32) prompt(player, Prompt.LEFT_ACTION, "Type: message <text>, command <command>, console <command>, or sound <sound>");
        else if (slot == 33) prompt(player, Prompt.PARTICLE, "Type a Bukkit particle name.");
        else if (slot == 40) openConfirm(player, data.name);
        else if (slot == 45) openNpcs(player);
        else if (slot == 49) openMain(player);
        else if (slot == 53) player.closeInventory();
    }

    private void handleConfirmClick(Player player, int slot) {
        String name = deleteConfirmations.remove(player.getUniqueId());
        if (slot == 11 && name != null) {
            npcManager.remove(name);
            npcManager.save();
            player.sendMessage(LegacyText.colorize("&aNPC removed."));
            openNpcs(player);
        } else if (slot == 15) {
            openNpcs(player);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final Prompt prompt = prompts.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        final String input = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                handlePrompt(player, prompt, input);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (isGui(event.getView().getTopInventory().getTitle())) {
            deleteConfirmations.remove(event.getPlayer().getUniqueId());
        }
    }

    private void handlePrompt(Player player, Prompt prompt, String input) {
        String value = input.trim();
        if (value.equalsIgnoreCase("cancel")) {
            player.sendMessage(LegacyText.colorize("&7[Settings] Edit cancelled."));
            return;
        }
        if (value.isEmpty()) {
            player.sendMessage(LegacyText.colorize("&cInput cannot be empty."));
            return;
        }
        if (prompt == Prompt.SEARCH_COMMAND) {
            searchTerms.put(player.getUniqueId(), value);
            openCommands(player);
        } else if (prompt == Prompt.SEARCH_NPC) {
            searchTerms.put(player.getUniqueId(), value);
            openNpcs(player);
        } else if (prompt == Prompt.MOTD) {
            dispatch(player, "editmotd " + value);
            reopenCommand(player, "editmotd");
        } else if (prompt == Prompt.ONLINE_COUNT) {
            dispatch(player, "editplayers online " + value);
            reopenCommand(player, "editplayers");
        } else if (prompt == Prompt.MAX_COUNT) {
            dispatch(player, "editplayers max " + value);
            reopenCommand(player, "editplayers");
        } else if (prompt == Prompt.ICON_URL) {
            dispatch(player, "editicon " + value);
            reopenCommand(player, "editicon");
        } else if (prompt == Prompt.CREATE_NPC) {
            dispatch(player, "npc create " + value);
            openNpcs(player);
        } else if (prompt == Prompt.NPC_SKIN) {
            dispatch(player, "npc skin " + selectedNpc.get(player.getUniqueId()) + " " + value);
            reopenNpc(player);
        } else if (prompt == Prompt.NPC_NAME) {
            dispatch(player, "npc name " + selectedNpc.get(player.getUniqueId()) + " " + value);
            reopenNpc(player);
        } else if (prompt == Prompt.RIGHT_ACTION || prompt == Prompt.LEFT_ACTION) {
            String click = prompt == Prompt.RIGHT_ACTION ? "rightclick" : "leftclick";
            dispatch(player, "npc " + click + " " + selectedNpc.get(player.getUniqueId()) + " " + value);
            reopenNpc(player);
        } else if (prompt == Prompt.PARTICLE) {
            dispatch(player, "npc particle " + selectedNpc.get(player.getUniqueId()) + " " + value);
            reopenNpc(player);
        }
    }

    private void prompt(Player player, Prompt prompt, String message) {
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        player.sendMessage(LegacyText.colorize("&b[Settings] &f" + message + " &7(Type cancel to stop.)"));
    }

    private void dispatch(Player player, String command) {
        if (command.equalsIgnoreCase("cancel")) return;
        Bukkit.dispatchCommand(player, command);
    }

    private void reopenCommand(Player player, String command) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                openCommand(player, command);
            }
        });
    }

    private void reopenNpc(Player player) {
        String name = selectedNpc.get(player.getUniqueId());
        if (name != null) openNpc(player, name);
    }

    private boolean requireAdmin(Player player) {
        return true;
    }

    private boolean isGui(String title) {
        return MAIN.equals(title) || COMMANDS.equals(title) || COMMAND.equals(title) || title.startsWith(CATEGORY_PREFIX) || NPCS.equals(title) || NPC.equals(title) || CONFIRM.equals(title);
    }

    private String displayedValue(String path) {
        Object value = plugin.getConfig().get("server-list." + path);
        return value == null ? "server default" : String.valueOf(value);
    }

    private String iconStatus() {
        String icon = plugin.getConfig().getString("server-list.cached-icon");
        return icon == null || icon.isEmpty() ? "none" : "cached";
    }

    private String commandDescription(String name) {
        if (name.equals("testplugin")) return "&7Basic plugin test command";
        if (name.equals("ping")) return "&7Shows player latency";
        if (name.equals("editmotd")) return "&7Changes the server list MOTD";
        if (name.equals("editplayers")) return "&7Changes displayed player counts";
        if (name.equals("editicon")) return "&7Downloads and caches a server icon";
        return "&7NPC management and interaction actions";
    }

    private Material commandMaterial(String name) {
        if (name.equals("ping")) return Material.WATCH;
        if (name.equals("npc")) return Material.ARMOR_STAND;
        if (name.equals("editicon")) return Material.PAINTING;
        return name.startsWith("edit") ? Material.PAPER : Material.BOOK;
    }

    private String onOff(boolean value) {
        return value ? "&aON" : "&cOFF";
    }

    private String format(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private void fill(Inventory inventory, Material material, int data, String name) {
        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, item);
    }

    private void button(Inventory inventory, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(LegacyText.colorize(name));
        List<String> colored = new ArrayList<String>();
        for (String line : lore) colored.add(LegacyText.colorize(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object[] values) {
        try {
            target.getClass().getMethod(name, types).invoke(target, values);
        } catch (Throwable ignored) {
        }
    }

    private enum Prompt {
        SEARCH_COMMAND, SEARCH_NPC, MOTD, ONLINE_COUNT, MAX_COUNT, ICON_URL,
        CREATE_NPC, NPC_SKIN, NPC_NAME, RIGHT_ACTION, LEFT_ACTION, PARTICLE
    }
}
