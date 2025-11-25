package me.charixmaz.arcaneperks.passive;

import me.charixmaz.arcaneperks.ArcanePerks;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PassiveGuiListener implements Listener {

    private final ArcanePerks plugin;

    public PassiveGuiListener(ArcanePerks plugin) {
        this.plugin = plugin;
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof PassiveGuiHolder guiHolder)) return;

        e.setCancelled(true); // prevent taking items

        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (guiHolder.getType()) {
            case ROOT -> handleRootClick(guiHolder, p, clicked, e.getSlot());
            case CATEGORY -> handleCategoryClick(guiHolder, p, clicked, e.getSlot());
        }
    }

    private void handleRootClick(PassiveGuiHolder holder, Player p, ItemStack clicked, int slot) {
        // Open category based on slot
        PassiveGui gui = plugin.getPassiveGui();

        if (slot == 10) {
            gui.openCategory(p, PassiveCategory.HEAD);
        } else if (slot == 12) {
            gui.openCategory(p, PassiveCategory.TORSO);
        } else if (slot == 14) {
            gui.openCategory(p, PassiveCategory.LEGS);
        } else if (slot == 16) {
            gui.openCategory(p, PassiveCategory.FEET);
        }
    }

    private void handleCategoryClick(PassiveGuiHolder holder, Player p, ItemStack clicked, int slot) {
        PassiveCategory cat = holder.getCategory();
        PassiveGui gui = plugin.getPassiveGui();

        // Back button
        if (clicked.getType() == Material.ARROW && slot == 18) {
            gui.openRoot(p);
            return;
        }

        // For now, clicking perks does nothing functional
        // (unlock system later). We only show tooltip.
        p.sendMessage(cc("&dArcane &7> &fThis passive is managed by LuckPerms permissions."));
    }
}
