package me.charixmaz.arcaneperks.passive;

import me.charixmaz.arcaneperks.ArcanePerks;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.List;

public class PassiveHeadListener implements Listener {

    private final ArcanePerks plugin;
    private final PassivePerkManager manager;

    public PassiveHeadListener(ArcanePerks plugin, PassivePerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // ----- continuous effects (Eagle Sight, Sixth Sense, Night Instinct) -----

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        // avoid running when barely moved
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        // EAGLE SIGHT: glow nearby mobs
        if (manager.isEnabled(p, PassivePerkType.EAGLE_SIGHT)) {
            int level = manager.getLevel(p, PassivePerkType.EAGLE_SIGHT);
            double radius = 6 + level * 2;           // 8–26 blocks
            int maxTargets = 1 + level;              // 2–11 targets
            int durationTicks = 20 + level * 10;     // 30–120 ticks

            highlightNearbyHostiles(p, radius, maxTargets, durationTicks);
        }

        // SIXTH SENSE: show attackers count in actionbar
        if (manager.isEnabled(p, PassivePerkType.SIXTH_SENSE)) {
            int level = manager.getLevel(p, PassivePerkType.SIXTH_SENSE);
            double radius = 10 + level * 2;          // 12–30 blocks
            int attackers = countTargetingMobs(p, radius);

            if (attackers > 0) {
                String msg = color("&dSixth Sense &7» &c" + attackers + " &7hostiles focused on you");
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            }
        }

        // NIGHT INSTINCT: weak night vision only in darkness
        if (manager.isEnabled(p, PassivePerkType.NIGHT_INSTINCT)) {
            if (p.getLocation().getBlock().getLightLevel() <= 4) {
                // your existing infinite NV handling goes here if you want
            }
        }
    }

    // ----- combat effects (Critical Mind, Focus) -----

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;

        // CRITICAL MIND – bonus crit chance
        if (manager.isEnabled(p, PassivePerkType.CRITICAL_MIND)) {
            int level = manager.getLevel(p, PassivePerkType.CRITICAL_MIND);
            double extraChance = 0.02 * level; // 2% per level, up to 20%

            if (Math.random() < extraChance) {
                e.setDamage(e.getDamage() * 1.25); // +25% damage
                p.sendMessage(color("&cCritical Mind &7» &eCritical strike!"));
                p.playEffect(EntityEffect.HURT);
            }
        }

        // FOCUS: for bow or trident hits we already boosted projectile; here we can heal stamina etc.
        if (manager.isEnabled(p, PassivePerkType.FOCUS)) {
            // Example: small health restore on ranged hit scaling with level
            if (e.getDamager() instanceof Arrow || e.getDamager() instanceof Trident) {
                int level = manager.getLevel(p, PassivePerkType.FOCUS);
                double heal = 0.25 * level; // up to 2.5 hearts
                double max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                p.setHealth(Math.min(max, p.getHealth() + heal));
            }
        }
    }

    // ----- helpers -----

    private void highlightNearbyHostiles(Player p, double radius, int maxTargets, int durationTicks) {
        Location loc = p.getLocation();
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity e : p.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le instanceof Player) continue;
            if (!(le instanceof Monster) && !(le instanceof Slime) && !(le instanceof Phantom)) continue;

            targets.add(le);
            if (targets.size() >= maxTargets) break;
        }

        for (LivingEntity le : targets) {
            le.setGlowing(true);
            // schedule un-glow
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                le.setGlowing(false);
            }, durationTicks);
        }
    }

    private int countTargetingMobs(Player p, double radius) {
        int count = 0;
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
            if (!(e instanceof Mob mob)) continue;
            if (mob.getTarget() != null && mob.getTarget().getUniqueId().equals(p.getUniqueId())) {
                count++;
            }
        }
        return count;
    }
}
