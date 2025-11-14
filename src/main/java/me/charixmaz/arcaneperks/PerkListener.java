package me.charixmaz.arcaneperks;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerkListener implements Listener {

    private final ArcanePerks plugin;
    private final PerkManager manager;

    public PerkListener(ArcanePerks plugin) {
        this.plugin = plugin;
        this.manager = plugin.getPerkManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        if (manager.hasPerk(p, PerkType.FLY)) p.setAllowFlight(true);
        if (manager.hasPerk(p, PerkType.GLOWING)) p.setGlowing(true);

        if (manager.hasPerk(p, PerkType.VANISH)) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(p)) other.hidePlayer(plugin, p);
            }
        }

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            if (manager.hasPerk(other, PerkType.VANISH)) {
                p.hidePlayer(plugin, other);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() != GameMode.CREATIVE && !manager.hasPerk(p, PerkType.FLY)) {
            p.setAllowFlight(false);
        }
        if (!manager.hasPerk(p, PerkType.GLOWING)) {
            p.setGlowing(false);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (manager.hasPerk(p, PerkType.KEEP_XP)) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        if (manager.hasPerk(p, PerkType.KEEP_INVENTORY)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        if (manager.hasPerk(p, PerkType.GOD)) {
            event.setCancelled(true);
            p.setFireTicks(0);
            return;
        }

        if (manager.hasPerk(p, PerkType.NO_FALL_DAMAGE)
                && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player p)) return;
        if (manager.hasPerk(p, PerkType.MOBS_IGNORE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (manager.hasPerk(killer, PerkType.DOUBLE_EXP)) {
            event.setDroppedExp(event.getDroppedExp() * 2);
        }

        if (manager.hasPerk(killer, PerkType.DOUBLE_MOB_DROPS)) {
            List<ItemStack> drops = event.getDrops();
            List<ItemStack> extra = new ArrayList<>();
            for (ItemStack stack : drops) {
                extra.add(stack.clone());
            }
            drops.addAll(extra);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block block = event.getBlock();

        boolean telekinesis = manager.hasPerk(p, PerkType.TELEKINESIS);
        boolean instantSmelt = manager.hasPerk(p, PerkType.INSTANT_SMELT);

        if (!telekinesis && !instantSmelt) return;

        var drops = block.getDrops(p.getInventory().getItemInMainHand());
        List<ItemStack> finalDrops = new ArrayList<>();

        for (ItemStack drop : drops) {
            ItemStack result = drop;
            if (instantSmelt) {
                result = getSmelted(drop);
            }
            finalDrops.add(result);
        }

        event.setDropItems(false);
        block.setType(Material.AIR);

        PlayerInventory inv = p.getInventory();
        for (ItemStack item : finalDrops) {
            Map<Integer, ItemStack> leftover = inv.addItem(item);
            for (ItemStack l : leftover.values()) {
                block.getWorld().dropItemNaturally(block.getLocation(), l);
            }
        }
    }

    private ItemStack getSmelted(ItemStack in) {
        if (in == null) return null;
        Material m = in.getType();
        int amount = in.getAmount();

        return switch (m) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT, amount);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT, amount);
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.COPPER_INGOT, amount);
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP, amount);
            default -> in;
        };
    }
}
