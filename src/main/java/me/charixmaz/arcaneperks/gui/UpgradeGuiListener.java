package me.charixmaz.arcaneperks.gui;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class UpgradeGuiListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof UpgradeGuiHolder upHolder)) return;

        e.setCancelled(true);

        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        upHolder.getGui().click(p, e.getSlot(), clicked);
    }
}
