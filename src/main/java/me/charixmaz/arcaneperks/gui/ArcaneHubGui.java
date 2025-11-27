package me.charixmaz.arcaneperks.gui;

import me.charixmaz.arcaneperks.ArcanePerks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArcaneHubGui {

    private final ArcanePerks plugin;

    public ArcaneHubGui(ArcanePerks plugin) {
        this.plugin = plugin;
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(
                new ArcaneHubGuiHolder(ArcaneHubGuiHolder.HubType.MAIN),
                54,
                cc("&5&lArcane Matrix")
        );

        // filler
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
        }

        // Left: Arcane (active) button at center-left
        inv.setItem(20, createButton(
                Material.ENCHANTED_BOOK,
                "&dArcane Perks",
                "&7Active perks you toggle manually.",
                "&eClick to open Arcane GUI"
        ));

        // Right: Passive button at center-right
        inv.setItem(24, createButton(
                Material.TOTEM_OF_UNDYING,
                "&dPassive Arcane",
                "&7Always-on passive perks (movement, head, etc.).",
                "&eClick to open Passive GUI"
        ));

        // Upgrade / unlock button in the very center
        inv.setItem(22, createButton(
                Material.NETHER_STAR,
                "&6Unlock / Upgrade",
                "&7Manage unlocks and levels for all perks.",
                "&eClick to open upgrade menu"
        ));

        p.openInventory(inv);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cc(name));
        List<String> lore = new ArrayList<>();
        for (String l : loreLines) {
            lore.add(cc(l));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
