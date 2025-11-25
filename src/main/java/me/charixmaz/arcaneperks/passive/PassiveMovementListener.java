package me.charixmaz.arcaneperks.passive;

import me.charixmaz.arcaneperks.ArcanePerks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PassiveMovementListener implements Listener {

    private final ArcanePerks plugin;
    private final PassivePerkManager manager;

    public PassiveMovementListener(ArcanePerks plugin, PassivePerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ------------------------------------------------------------
    // SwiftStep – small speed boost after walking some distance
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!manager.hasSwiftStep(p)) return;

        PassivePerkManager.SwiftData data = manager.getSwiftData(p);

        if (e.getFrom().getWorld() != e.getTo().getWorld()) {
            data.setLastLocation(e.getTo());
            return;
        }

        double dist = e.getTo().distance(data.getLastLocation());
        long now = System.currentTimeMillis();

        if (dist >= manager.getSwiftDistance(p) &&
                now - data.getLastTrigger() >= manager.getSwiftCooldownMs(p)) {

            int lvl = manager.getLevel(p, PassivePerkType.SWIFT_STEP, 1);
            int amplifier = Math.max(0, Math.min(4, lvl - 1));
            int durationTicks = 20 * 5; // 5 sec

            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, durationTicks, amplifier, true, false, false));

            p.spawnParticle(
                    org.bukkit.Particle.SWEEP_ATTACK,
                    p.getLocation().add(0, 1.0, 0),
                    8,
                    0.3, 0.2, 0.3,
                    0.0
            );

            manager.recordSwiftTrigger(p);
        }
    }

    // ------------------------------------------------------------
    // Adrenaline & SoftLanding – damage event
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        // Adrenaline: triggers when HP goes below threshold
        if (manager.hasAdrenaline(p) && manager.canTriggerAdrenaline(p)) {
            double thresholdHearts = manager.getAdrenalineThresholdHearts(); // from passiveconfig.yml
            double threshold = thresholdHearts * 2.0; // convert hearts -> HP

            double hpAfter = p.getHealth() - e.getFinalDamage();
            if (hpAfter > 0 && hpAfter <= threshold) {
                manager.triggerAdrenaline(p);
            }
        }

        // SoftLanding: fall damage reduction
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL
                && manager.hasSoftLanding(p)
                && manager.canTriggerSoftLanding(p)) {

            double original = e.getDamage();
            double reduced = manager.applySoftLanding(p, original);
            e.setDamage(reduced);
        }
    }
}
