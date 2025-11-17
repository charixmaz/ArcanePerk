package me.charixmaz.arcaneperks;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PerkListener implements Listener {

    private final ArcanePerks plugin;
    private final PerkManager perkManager;

    public PerkListener(ArcanePerks plugin) {
        this.plugin = plugin;
        this.perkManager = plugin.getPerkManager();
    }

    // ------------------------------------------------------
    // GOD, NO-FALL, FLY / VANISH restrictions
    // ------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        // God mode – no damage at all
        if (perkManager.hasPerk(p, PerkType.GOD)) {
            event.setCancelled(true);
            p.setFireTicks(0);
            return;
        }

        // Soft landing
        if (perkManager.hasPerk(p, PerkType.NO_FALL_DAMAGE)
                && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }

        // Combat restrictions: disable fly / vanish on hit
        if (plugin.getConfig().getBoolean("restrictions.combat.disable-fly-on-damage", true)
                && perkManager.hasPerk(p, PerkType.FLY)) {
            perkManager.deactivate(PerkType.FLY, p);
        }
        if (plugin.getConfig().getBoolean("restrictions.combat.disable-vanish-on-damage", true)
                && perkManager.hasPerk(p, PerkType.VANISH)) {
            perkManager.deactivate(PerkType.VANISH, p);
        }
    }

    // ------------------------------------------------------
    // TELEKINESIS + INSTANT SMELT – BLOCK DROPS
    // ------------------------------------------------------

    /**
     * Handles block drops after fortune / silk etc. are already applied.
     * Telekinesis: move drops to inventory.
     * InstantSmelt: convert ores → ingots, sand → glass, etc.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;

        boolean telek = perkManager.hasPerk(p, PerkType.TELEKINESIS);
        boolean smelt = perkManager.hasPerk(p, PerkType.INSTANT_SMELT);

        if (!telek && !smelt) return;

        List<Item> items = event.getItems();
        if (items.isEmpty()) return;

        for (Item itemEntity : new ArrayList<>(items)) {
            ItemStack stack = itemEntity.getItemStack();

            // InstantSmelt for ores / blocks
            if (smelt) {
                stack = applyInstantSmelt(stack);
            }

            if (telek) {
                // Try to put into player inventory
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
                if (leftover.isEmpty()) {
                    itemEntity.remove();
                } else {
                    // Something did not fit, keep the remainder on the ground
                    ItemStack rest = null;
                    for (ItemStack v : leftover.values()) {
                        rest = v;
                        break;
                    }
                    if (rest != null) {
                        itemEntity.setItemStack(rest);
                    } else {
                        itemEntity.remove();
                    }
                }
            } else {
                // not telekinesis, but maybe smelting changed the stack
                itemEntity.setItemStack(stack);
            }
        }
    }

    private ItemStack applyInstantSmelt(ItemStack original) {
        Material type = original.getType();
        Material result = null;

        switch (type) {
            // iron
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case RAW_IRON:
                result = Material.IRON_INGOT;
                break;

            // gold
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case RAW_GOLD:
                result = Material.GOLD_INGOT;
                break;

            // copper
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case RAW_COPPER:
                result = Material.COPPER_INGOT;
                break;

            // netherite scrap from ancient debris
            case ANCIENT_DEBRIS:
                result = Material.NETHERITE_SCRAP;
                break;

            // glass from sand
            case SAND:
                result = Material.GLASS;
                break;

            default:
                break;
        }

        if (result == null) {
            return original;
        }

        ItemStack out = new ItemStack(result, original.getAmount());
        if (original.hasItemMeta()) {
            out.setItemMeta(original.getItemMeta());
        }
        return out;
    }

    // ------------------------------------------------------
    // TELEKINESIS + INSTANT SMELT – MOB DROPS (food)
    // ------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity e = event.getEntity();
        Player killer = ((org.bukkit.entity.LivingEntity) e).getKiller();
        if (killer == null) return;

        boolean telek = perkManager.hasPerk(killer, PerkType.TELEKINESIS);
        boolean smelt = perkManager.hasPerk(killer, PerkType.INSTANT_SMELT);

        if (!telek && !smelt) return;

        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) return;

        List<ItemStack> newDrops = new ArrayList<>();

        for (ItemStack stack : drops) {
            ItemStack current = stack;

            if (smelt) {
                current = applyCookFood(current);
            }

            if (telek) {
                HashMap<Integer, ItemStack> leftover = killer.getInventory().addItem(current);
                if (!leftover.isEmpty()) {
                    for (ItemStack rest : leftover.values()) {
                        newDrops.add(rest);
                    }
                }
            } else {
                newDrops.add(current);
            }
        }

        drops.clear();
        drops.addAll(newDrops);
    }

    private ItemStack applyCookFood(ItemStack original) {
        Material type = original.getType();
        Material result = null;

        switch (type) {
            case BEEF:
                result = Material.COOKED_BEEF;
                break;
            case PORKCHOP:
                result = Material.COOKED_PORKCHOP;
                break;
            case CHICKEN:
                result = Material.COOKED_CHICKEN;
                break;
            case MUTTON:
                result = Material.COOKED_MUTTON;
                break;
            case RABBIT:
                result = Material.COOKED_RABBIT;
                break;
            case COD:
                result = Material.COOKED_COD;
                break;
            case SALMON:
                result = Material.COOKED_SALMON;
                break;
            default:
                break;
        }

        if (result == null) {
            return original;
        }

        ItemStack out = new ItemStack(result, original.getAmount());
        if (original.hasItemMeta()) {
            out.setItemMeta(original.getItemMeta());
        }
        return out;
    }
}
