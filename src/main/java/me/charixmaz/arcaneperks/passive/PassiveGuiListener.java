package me.charixmaz.arcaneperks.passive;

import me.charixmaz.arcaneperks.ArcanePerks;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class PassiveGuiListener implements Listener {

    private final ArcanePerks plugin;

    public PassiveGuiListener(ArcanePerks plugin) {
        this.plugin = plugin;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof PassiveGuiHolder holder)) return;

        e.setCancelled(true);

        PassivePerkManager manager = plugin.getPassivePerkManager();
        PassiveGui gui = plugin.getPassiveGui();

        // ROOT: open category GUI
        if (holder.getType() == PassiveGuiHolder.GuiType.ROOT) {
            switch (e.getRawSlot()) {
                case 10 -> gui.openCategory(p, PassiveCategory.HEAD);
                case 12 -> gui.openCategory(p, PassiveCategory.TORSO);
                case 14 -> gui.openCategory(p, PassiveCategory.LEGS);
                case 16 -> gui.openCategory(p, PassiveCategory.FEET);
            }
            return;
        }

        // CATEGORY: toggle perk
        if (holder.getType() == PassiveGuiHolder.GuiType.CATEGORY) {
            PassiveCategory cat = holder.getCategory();
            if (cat == null) return;

            PassivePerkType target = null;

            // slot → perk mapping (same as in PassiveGui)
            switch (cat) {
                case HEAD -> {
                    if (e.getRawSlot() == 10) target = PassivePerkType.EAGLE_SIGHT;
                    if (e.getRawSlot() == 12) target = PassivePerkType.SIXTH_SENSE;
                    if (e.getRawSlot() == 14) target = PassivePerkType.CRITICAL_MIND;
                    if (e.getRawSlot() == 16) target = PassivePerkType.FOCUS;
                }
                case TORSO -> {
                    if (e.getRawSlot() == 10) target = PassivePerkType.ADRENALINE;
                    if (e.getRawSlot() == 12) target = PassivePerkType.IRON_SKIN;
                    if (e.getRawSlot() == 14) target = PassivePerkType.FIRE_HEART;
                    if (e.getRawSlot() == 16) target = PassivePerkType.METABOLIC_RECOVERY;
                }
                case LEGS -> {
                    if (e.getRawSlot() == 10) target = PassivePerkType.SOFT_LANDING;
                    if (e.getRawSlot() == 12) target = PassivePerkType.CLIMBERS_GRIP;
                    if (e.getRawSlot() == 14) target = PassivePerkType.MOMENTUM;
                    if (e.getRawSlot() == 16) target = PassivePerkType.STEP_ASSIST;
                }
                case FEET -> {
                    if (e.getRawSlot() == 10) target = PassivePerkType.SWIFT_SPEED;
                    if (e.getRawSlot() == 12) target = PassivePerkType.SILENT_STEPS;
                    if (e.getRawSlot() == 14) target = PassivePerkType.PATHFINDER;
                    if (e.getRawSlot() == 16) target = PassivePerkType.PREDATORS_TREAD;
                }
            }

            if (target == null) return;

            boolean enabled = manager.isEnabled(p, target);
            manager.setEnabled(p, target, !enabled);

            p.sendMessage(color("&5Arcane &7» " +
                    (enabled ? "&cDisabled " : "&aEnabled ") +
                    target.getDisplayName()));

            // refresh item
            plugin.getPassiveGui().openCategory(p, cat);
        }
    }
}
