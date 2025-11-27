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

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // ---------------------------------------------------------------------
    // ROOT ANATOMY GUI
    // ---------------------------------------------------------------------

    public void openRoot(Player p) {
        Inventory inv = Bukkit.createInventory(
                new PassiveGuiHolder(PassiveGuiHolder.GuiType.ROOT),
                27,
                cc("&5&lArcane Anatomy")
        );

        // Simple layout: put categories in row 2
        setCategory(inv, 10, PassiveCategory.HEAD);
        setCategory(inv, 12, PassiveCategory.TORSO);
        setCategory(inv, 14, PassiveCategory.LEGS);
        setCategory(inv, 16, PassiveCategory.FEET);

        p.openInventory(inv);
    }

    private void setCategory(Inventory inv, int slot, PassiveCategory cat) {
        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cc("&d" + cat.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(cc("&7" + cat.getDescription()));
        lore.add("");
        lore.add(cc("&eClick to view perks"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    // ---------------------------------------------------------------------
    // CATEGORY GUI
    // ---------------------------------------------------------------------

    public void openCategory(Player p, PassiveCategory cat) {
        Inventory inv = Bukkit.createInventory(
                new PassiveGuiHolder(PassiveGuiHolder.GuiType.CATEGORY, cat),
                27,
                cc("&5Arcane &7- &d" + cat.getDisplayName())
        );

        // Map existing perks into categories
        // You can move these around later or add more
        switch (cat) {
            case HEAD -> {
                // no perks yet
            }
            case TORSO -> {
                // Adrenaline
                setPerkItem(inv, 11, PassivePerkType.ADRENALINE, p);
            }
            case LEGS -> {
                // Soft Landing
                setPerkItem(inv, 13, PassivePerkType.SOFT_LANDING, p);
            }
            case FEET -> {
                // Swift Step
                setPerkItem(inv, 15, PassivePerkType.SWIFT_STEP, p);
            }
        }

        // Add a "back" button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(cc("&cBack"));
        back.setItemMeta(bm);
        inv.setItem(18, back);

        p.openInventory(inv);
    }

    private void setPerkItem(Inventory inv, int slot, PassivePerkType perk, Player p) {
        boolean has = switch (perk) {
            case SWIFT_STEP -> manager.hasSwiftStep(p);
            case ADRENALINE -> manager.hasAdrenaline(p);
            case SOFT_LANDING -> manager.hasSoftLanding(p);
        };

        int level = manager.getLevel(p, perk, 0); // 0 -> will display "Locked" if no perm

        Material mat;
        String name;
        String desc;

        switch (perk) {
            case SWIFT_STEP -> {
                mat = Material.LEATHER_BOOTS;
                name = "Swift Step";
                desc = "Occasional speed burst while walking.";
            }
            case ADRENALINE -> {
                mat = Material.REDSTONE;
                name = "Adrenaline";
                desc = "Heal when close to death.";
            }
            case SOFT_LANDING -> {
                mat = Material.SLIME_BLOCK;
                name = "Soft Landing";
                desc = "Reduced fall damage.";
            }
            default -> {
                mat = Material.BARRIER;
                name = perk.name();
                desc = "Passive perk.";
            }
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cc((has ? "&a" : "&c") + name));

        List<String> lore = new ArrayList<>();
        lore.add(cc("&7" + desc));
        lore.add("");

        if (has) {
            lore.add(cc("&aUnlocked"));
            lore.add(cc("&7Level: &f" + level));
            lore.add("");
            lore.add(cc("&8(Unlock system: via LuckPerms perms)"));
        } else {
            lore.add(cc("&cLocked"));
            lore.add(cc("&7Requires permission:"));
            lore.add(cc("&f" + perk.getPermPrefix() + ".x"));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        inv.setItem(slot, item);
    }
}
