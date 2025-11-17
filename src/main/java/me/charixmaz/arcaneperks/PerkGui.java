package me.charixmaz.arcaneperks;

import net.kyori.adventure.text.Component;
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

public class PerkGui {

    public static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Arcane Perks";

    private final ArcanePerks plugin;

    public PerkGui(ArcanePerks plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int size = 27;
        int count = PerkType.values().length;
        if (count > 27 && count <= 36) size = 36;
        else if (count > 36 && count <= 45) size = 45;
        else if (count > 45) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, GUI_TITLE);

        for (PerkType type : PerkType.values()) {
            inv.addItem(createItem(player, type));
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Player player, PerkType type) {
        Material mat = getMaterialFor(type);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String name = "§d" + type.getDisplayName();
        meta.setDisplayName(name);

        List<String> loreLines = new ArrayList<>();

        String desc = plugin.getConfig().getString(
                "descriptions." + type.getId(),
                "&7No description."
        );
        loreLines.add(ChatColor.translateAlternateColorCodes('&', desc));

        boolean hasAccess = player.hasPermission(type.getPermissionNode());
        boolean active = plugin.getPerkManager().hasPerk(player, type);

        loreLines.add("");
        if (!hasAccess) {
            loreLines.add("§cLocked §7(no permission)");
        } else if (active) {
            loreLines.add("§aActive");
            loreLines.add("§7Click to deactivate.");
        } else {
            loreLines.add("§eAvailable");
            loreLines.add("§7Click to activate.");
        }

// Convert List<String> -> List<Component> WITHOUT streams
        List<Component> compLore = new ArrayList<>();
        for (String line : loreLines) {
            compLore.add(Component.text(line));
        }
        meta.lore(compLore);

// These are the only flags that still exist in 1.21+
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;

    }

    private Material getMaterialFor(PerkType type) {
        return switch (type) {
            case FAST_DIGGING -> Material.IRON_PICKAXE;
            case NIGHT_VISION -> Material.ENDER_EYE;
            case STRENGTH -> Material.IRON_SWORD;
            case FLY -> Material.FEATHER;
            case KEEP_XP -> Material.EXPERIENCE_BOTTLE;
            case KEEP_INVENTORY -> Material.CHEST;
            case NO_FALL_DAMAGE -> Material.HAY_BLOCK;
            case DOUBLE_EXP -> Material.ENCHANTING_TABLE;
            case DOUBLE_MOB_DROPS -> Material.ROTTEN_FLESH;
            case GOD -> Material.NETHER_STAR;
            case VANISH -> Material.GLASS;
            case MOBS_IGNORE -> Material.ZOMBIE_HEAD;
            case GLOWING -> Material.GLOWSTONE_DUST;
            case TELEKINESIS -> Material.HOPPER;
            case INSTANT_SMELT -> Material.BLAST_FURNACE;
            case SPEED -> Material.LEATHER_BOOTS;
        };
    }
}
