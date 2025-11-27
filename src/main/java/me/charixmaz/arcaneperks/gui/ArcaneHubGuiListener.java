package me.charixmaz.arcaneperks.gui;

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

public class ArcaneHubGuiListener implements Listener {

    private final ArcanePerks plugin;

    public ArcaneHubGuiListener(ArcanePerks plugin) {
        this.plugin = plugin;
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof ArcaneHubGuiHolder hub)) return;
        e.setCancelled(true);

        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getSlot();

        if (hub.getType() == ArcaneHubGuiHolder.HubType.MAIN) {
            if (slot == 20) {
                plugin.getPerkGui().open(p);
            } else if (slot == 24) {
                plugin.getPassiveGui().openRoot(p);
            } else if (slot == 22) {
                plugin.getUpgradeGui().open(p); // see next section
            }
        }
    }
}
