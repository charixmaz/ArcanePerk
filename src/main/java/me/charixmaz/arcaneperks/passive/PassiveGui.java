package me.charixmaz.arcaneperks.passive;

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

public class PassiveGui {

    private final ArcanePerks plugin;
    private final PassivePerkManager manager;

    public PassiveGui(ArcanePerks plugin, PassivePerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ----- root GUI (Head / Torso / Legs / Feet buttons) -----

    public void openRoot(Player p) {
        Inventory inv = Bukkit.createInventory(
                new PassiveGuiHolder(PassiveGuiHolder.GuiType.ROOT, null),
                27,
                color("&5Arcane &7» &dPassive Perks")
        );

        inv.setItem(10, categoryItem(Material.PLAYER_HEAD, "&eHead Perks", "&7Vision, awareness, precision"));
        inv.setItem(12, categoryItem(Material.LEATHER_CHESTPLATE, "&aTorso Perks", "&7Vitality, defense, regen"));
        inv.setItem(14, categoryItem(Material.LEATHER_LEGGINGS, "&bLegs Perks", "&7Mobility, agility"));
        inv.setItem(16, categoryItem(Material.LEATHER_BOOTS, "&dFeet Perks", "&7Ground control, movement"));

        p.openInventory(inv);
    }

    // ----- category GUI -----

    public void openCategory(Player p, PassiveCategory cat) {
        Inventory inv = Bukkit.createInventory(
                new PassiveGuiHolder(PassiveGuiHolder.GuiType.CATEGORY, cat),
                27,
                color("&5Arcane &7» &d" + cat.getDisplayName())
        );

        switch (cat) {
            case HEAD -> {
                setPerkItem(inv, 10, PassivePerkType.EAGLE_SIGHT, p);
                setPerkItem(inv, 12, PassivePerkType.SIXTH_SENSE, p);
                setPerkItem(inv, 14, PassivePerkType.CRITICAL_MIND, p);
                setPerkItem(inv, 16, PassivePerkType.FOCUS, p);
            }
            case TORSO -> {
                setPerkItem(inv, 10, PassivePerkType.ADRENALINE, p);
                setPerkItem(inv, 12, PassivePerkType.IRON_SKIN, p);
                setPerkItem(inv, 14, PassivePerkType.FIRE_HEART, p);
                setPerkItem(inv, 16, PassivePerkType.METABOLIC_RECOVERY, p);
            }
            case LEGS -> {
                setPerkItem(inv, 10, PassivePerkType.SOFT_LANDING, p);
                setPerkItem(inv, 12, PassivePerkType.CLIMBERS_GRIP, p);
                setPerkItem(inv, 14, PassivePerkType.MOMENTUM, p);
                setPerkItem(inv, 16, PassivePerkType.STEP_ASSIST, p);
            }
            case FEET -> {
                setPerkItem(inv, 10, PassivePerkType.SWIFT_SPEED, p);
                setPerkItem(inv, 12, PassivePerkType.SILENT_STEPS, p);
                setPerkItem(inv, 14, PassivePerkType.PATHFINDER, p);
                setPerkItem(inv, 16, PassivePerkType.PREDATORS_TREAD, p);
            }
        }

        p.openInventory(inv);
    }

    // ----- helpers -----

    private ItemStack categoryItem(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> lore = new ArrayList<>();
            lore.add(color(loreLine));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void setPerkItem(Inventory inv, int slot, PassivePerkType type, Player p) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int level = manager.getLevel(p, type);
        boolean enabled = manager.isEnabled(p, type);

        meta.setDisplayName(color(type.getDisplayName() + " &7[Lv." + level + "]"));

        List<String> lore = new ArrayList<>();
        lore.add(color(enabled ? "&aActive" : "&cInactive"));
        lore.add(color("&7Click to toggle"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        inv.setItem(slot, item);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
