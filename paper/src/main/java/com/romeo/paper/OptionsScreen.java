package com.romeo.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class OptionsScreen implements CommandExecutor, Listener {
    private final RomeoPaperPlugin plugin;
    private final SettingsGUI settingsGUI;
    private final NpcManager npcManager;
    private final Map<UUID, Prompt> pending = new HashMap<UUID, Prompt>();
    private final Set<UUID> vanished = new HashSet<UUID>();

    OptionsScreen(RomeoPaperPlugin plugin, SettingsGUI settingsGUI, NpcManager npcManager) {
        this.plugin = plugin;
        this.settingsGUI = settingsGUI;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the settings panel.");
            return true;
        }
        open((Player) sender);
        return true;
    }

    void open(Player player) {
        player.openInventory(buildInventory(player));
    }

    private Inventory buildInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, legacy(titleComponent()));
        fillGlass(inventory);

        addButton(inventory, 10, Material.EYE_OF_ENDER, buttonText("Vanish: " + (vanished.contains(player.getUniqueId()) ? "ON" : "OFF")), "Toggle visibility for yourself");
        addButton(inventory, 19, Material.PAPER, buttonText("Edit MOTD..."), "Type a new server MOTD into chat");
        addButton(inventory, 28, Material.PAINTING, buttonText("Server Icon..."), "Paste a server icon image URL");
        addButton(inventory, 37, Material.CHEST, buttonText("Reload Config..."), "Re-read config.yml values");

        addButton(inventory, 15, Material.WATCH, buttonText("Player Ping: " + getPing(player) + " ms"), "Current latency for this player");
        addButton(inventory, 24, Material.PAPER, buttonText("Player Count..."), "Set online/max player counts");
        addButton(inventory, 33, Material.ARMOR_STAND, buttonText("NPC Studio..."), "Open NPC tools and manager controls");
        addButton(inventory, 42, Material.REDSTONE, buttonText("Force Save"), "Write data to disk immediately");

        addButton(inventory, 49, Material.BARRIER, buttonText("Done"), "Close the settings view");
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().equals(legacy(titleComponent()))) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        handleClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = pending.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage() == null ? "" : event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(LegacyText.colorize("&7[Settings] Cancelled."));
            open(player);
            return;
        }
        if (message.isEmpty()) {
            player.sendMessage(LegacyText.colorize("&cInput cannot be empty."));
            open(player);
            return;
        }
        handlePrompt(player, prompt, message);
        open(player);
    }

    private void handleClick(Player player, int slot) {
        if (slot == 10) {
            toggleVanish(player);
        } else if (slot == 19) {
            ask(player, Prompt.MOTD, "Type the new MOTD in chat or type 'cancel'.");
        } else if (slot == 28) {
            ask(player, Prompt.ICON_URL, "Type the image URL in chat or type 'cancel'.");
        } else if (slot == 37) {
            plugin.reloadConfig();
            player.sendMessage(LegacyText.colorize("&a[Settings] Configuration reloaded."));
        } else if (slot == 15) {
            player.sendMessage(LegacyText.colorize("&a[Settings] Your ping: " + getPing(player) + " ms"));
        } else if (slot == 24) {
            ask(player, Prompt.PLAYER_COUNT, "Type: online <count> or max <count> or type 'cancel'.");
        } else if (slot == 33) {
            if (settingsGUI != null) {
                settingsGUI.open(player);
            } else {
                player.sendMessage(LegacyText.colorize("&7[NPC Studio] NPC tools are available through the advanced settings UI."));
            }
        } else if (slot == 42) {
            plugin.saveConfig();
            if (npcManager != null) {
                npcManager.save();
            }
            player.sendMessage(LegacyText.colorize("&a[Settings] Data saved to disk."));
        } else if (slot == 49) {
            player.closeInventory();
        }
    }

    private void handlePrompt(Player player, Prompt prompt, String value) {
        if (prompt == Prompt.MOTD) {
            Bukkit.dispatchCommand(player, "editmotd " + value);
        } else if (prompt == Prompt.ICON_URL) {
            Bukkit.dispatchCommand(player, "editicon " + value);
        } else if (prompt == Prompt.PLAYER_COUNT) {
            String[] parts = value.split("\\s+");
            if (parts.length == 2 && (parts[0].equalsIgnoreCase("online") || parts[0].equalsIgnoreCase("max"))) {
                Bukkit.dispatchCommand(player, "editplayers " + parts[0] + " " + parts[1]);
            } else {
                player.sendMessage(LegacyText.colorize("&c[Settings] Use: online <count> or max <count>"));
            }
        }
    }

    private void ask(Player player, Prompt prompt, String message) {
        pending.put(player.getUniqueId(), prompt);
        player.closeInventory();
        player.sendMessage(LegacyText.colorize("&b[Settings] &f" + message + " &7(Type cancel to exit.)"));
    }

    private void toggleVanish(Player player) {
        UUID id = player.getUniqueId();
        if (vanished.contains(id)) {
            vanished.remove(id);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other != player) {
                    other.showPlayer(player);
                }
            }
            player.sendMessage(LegacyText.colorize("&a[Settings] Vanish disabled."));
        } else {
            vanished.add(id);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other != player) {
                    other.hidePlayer(player);
                }
            }
            player.sendMessage(LegacyText.colorize("&a[Settings] Vanish enabled."));
        }
    }

    private int getPing(Player player) {
        return 0;
    }

    private void fillGlass(Inventory inventory) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    private void addButton(Inventory inventory, int slot, Material material, Component name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(legacy(name));
        List<String> lore = new ArrayList<String>();
        lore.add(LegacyText.colorize(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private Component titleComponent() {
        return Component.text("Romeo Settings...")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD);
    }

    private Component buttonText(String text) {
        return Component.text(text).color(NamedTextColor.GOLD);
    }

    private String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    private enum Prompt {
        MOTD,
        ICON_URL,
        PLAYER_COUNT
    }
}
