package me.charixmaz.arcaneperks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    private final ArcanePerks plugin;

    public GuiListener(ArcanePerks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!ChatColor.stripColor(event.getView().getTitle())
                .equals(ChatColor.stripColor(PerkGui.GUI_TITLE))) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        PerkType target = null;
        for (PerkType type : PerkType.values()) {
            if (name.equalsIgnoreCase(type.getDisplayName())) {
                target = type;
                break;
            }
        }
        if (target == null) return;

        PerkManager manager = plugin.getPerkManager();

        if (!player.hasPermission(target.getPermissionNode())) {
            player.sendMessage("§cYou do not have permission for this perk.");
            manager.playError(player);
            return;
        }

        manager.toggle(target, player);

        // refresh GUI item
        new PerkGui(plugin).open(player);
    }
}
