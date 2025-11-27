package me.charixmaz.arcaneperks.gui;

import me.charixmaz.arcaneperks.ArcanePerks;
import me.charixmaz.arcaneperks.PerkType;
import me.charixmaz.arcaneperks.passive.PassivePerkType;
import me.charixmaz.arcaneperks.unlock.UnlockArcanePerks;
import me.charixmaz.arcaneperks.unlock.UnlockPassivePerks;
import me.charixmaz.arcaneperks.unlock.UnlockCost;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradeGui implements InventoryHolder {

    private final ArcanePerks plugin;
    private final UnlockArcanePerks unlockArcane;
    private final UnlockPassivePerks unlockPassive;

    public UpgradeGui(ArcanePerks plugin,
                      UnlockArcanePerks unlockArcane,
                      UnlockPassivePerks unlockPassive) {
        this.plugin = plugin;
        this.unlockArcane = unlockArcane;
        this.unlockPassive = unlockPassive;
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // Required by InventoryHolder
    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(
                new UpgradeGuiHolder(this),
                54,
                cc("&6Arcane Upgrades")
        );

        // Simple layout example: top 3 rows for Arcane, bottom 3 for Passive
        int slot = 0;
        for (PerkType type : PerkType.values()) {
            inv.setItem(slot++, createArcaneItem(p, type));
            if (slot >= 27) break;
        }

        slot = 27;
        for (PassivePerkType type : PassivePerkType.values()) {
            inv.setItem(slot++, createPassiveItem(p, type));
            if (slot >= 54) break;
        }

        p.openInventory(inv);
    }

    private ItemStack createArcaneItem(Player p, PerkType type) {
        boolean unlocked = unlockArcane.isUnlocked(p, type);
        UnlockCost cost = unlockArcane.getCost(type);

        ItemStack item = new ItemStack(unlocked ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cc((unlocked ? "&a" : "&c") + type.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(cc("&7Type: &fArcane (active)"));
        lore.add("");
        lore.add(cc("&7Unlock cost:"));
        lore.add(cc("&f- XP levels: &e" + cost.getXpLevels()));
        lore.add(cc("&f- Money: &e" + cost.getMoney()));
        lore.add("");
        lore.add(cc(unlocked ?
                "&aClick to upgrade (future)." :
                "&eClick to unlock this perk."
        ));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPassiveItem(Player p, PassivePerkType type) {
        boolean unlocked = unlockPassive.isUnlocked(p, type);
        UnlockCost cost = unlockPassive.getCost(type);

        ItemStack item = new ItemStack(unlocked ? Material.LAPIS_BLOCK : Material.COAL_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cc((unlocked ? "&a" : "&c") + type.getId()));
        List<String> lore = new ArrayList<>();
        lore.add(cc("&7Type: &fPassive"));
        lore.add("");
        lore.add(cc("&7Unlock cost:"));
        lore.add(cc("&f- XP levels: &e" + cost.getXpLevels()));
        lore.add(cc("&f- Money: &e" + cost.getMoney()));
        lore.add("");
        lore.add(cc(unlocked ?
                "&aClick to upgrade (future)." :
                "&eClick to unlock this perk."
        ));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // Called from listener
    public void click(Player p, int slot, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot < 27) {
            // Arcane
            PerkType[] values = PerkType.values();
            if (slot < values.length) {
                unlockArcane.tryUnlock(p, values[slot]);
            }
        } else {
            // Passive
            int idx = slot - 27;
            PassivePerkType[] values = PassivePerkType.values();
            if (idx < values.length) {
                unlockPassive.tryUnlock(p, values[idx]);
            }
        }
    }
}
