package me.charixmaz.arcaneperks.passive;

import me.charixmaz.arcaneperks.ArcanePerks;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PassivePerkManager {

    private final ArcanePerks plugin;

    // passiveconfig.yml
    private File passiveConfigFile;
    private FileConfiguration passiveConfig;

    // Movement perks state
    private final Map<UUID, SwiftData> swiftData = new HashMap<>();
    private final Map<UUID, Long> adrenalineCooldown = new HashMap<>();
    private final Map<UUID, Long> softCooldown = new HashMap<>();

    // NEW head perks state
    private final Map<UUID, Long> eagleCooldown = new HashMap<>();
    private final Map<UUID, Long> sixthCooldown = new HashMap<>();

    private final Random random = new Random();

    public PassivePerkManager(ArcanePerks plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ---------------------------------------------------------------
    // CONFIG LOAD / RELOAD (passiveconfig.yml)
    // ---------------------------------------------------------------

    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        passiveConfigFile = new File(plugin.getDataFolder(), "passiveconfig.yml");

        if (!passiveConfigFile.exists()) {
            try {
                plugin.saveResource("passiveconfig.yml", false);
            } catch (IllegalArgumentException ignored) {
                // no default in jar – fine
            }
        }

        passiveConfig = YamlConfiguration.loadConfiguration(passiveConfigFile);
    }

    public void reloadConfig() {
        loadConfig();
    }

    // ---------------------------------------------------------------
    // CONFIG HELPERS
    // ---------------------------------------------------------------

    private ConfigurationSection section(String id) {
        if (passiveConfig == null) return null;
        return passiveConfig.getConfigurationSection(id);
    }

    public int getInt(String id, String path, int def) {
        ConfigurationSection sec = section(id);
        return sec != null ? sec.getInt(path, def) : def;
    }

    public double getDouble(String id, String path, double def) {
        ConfigurationSection sec = section(id);
        return sec != null ? sec.getDouble(path, def) : def;
    }

    public String getString(String id, String path, String def) {
        ConfigurationSection sec = section(id);
        return sec != null ? sec.getString(path, def) : def;
    }

    // ---------------------------------------------------------------
    // PERMISSION-BASED LEVEL
    // ---------------------------------------------------------------

    public int getLevel(Player p, PassivePerkType type, int def) {
        int best = def;
        String prefix = (type.getPermPrefix() + ".").toLowerCase(Locale.ROOT);
        for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission().toLowerCase(Locale.ROOT);
            if (!perm.startsWith(prefix)) continue;
            String suffix = perm.substring(prefix.length());
            try {
                int lvl = Integer.parseInt(suffix);
                if (lvl > best) best = lvl;
            } catch (NumberFormatException ignored) {
            }
        }
        if (best < 1) best = 1;
        return best;
    }

    // ---------------------------------------------------------------
    // SIMPLE HAS-XXX HELPERS (used by GUI / listeners)
    // ---------------------------------------------------------------

    public boolean hasSwiftStep(Player p) {
        return p.hasPermission(PassivePerkType.SWIFT_STEP.getPermPrefix());
    }

    public boolean hasAdrenaline(Player p) {
        return p.hasPermission(PassivePerkType.ADRENALINE.getMaxLevel());
    }

    public boolean hasSoftLanding(Player p) {
        return p.hasPermission(PassivePerkType.SOFT_LANDING.getPermPrefix());
    }

    // head perks
    public boolean hasEagleSight(Player p) {
        return p.hasPermission(PassivePerkType.EAGLE_SIGHT.getPermPrefix());
    }

    public boolean hasSixthSense(Player p) {
        return p.hasPermission(PassivePerkType.SIXTH_SENSE.getPermPrefix());
    }

    public boolean hasCriticalMind(Player p) {
        return p.hasPermission(PassivePerkType.CRITICAL_MIND.getPermPrefix());
    }

    public boolean hasNightInstinct(Player p) {
        return p.hasPermission(PassivePerkType.NIGHT_INSTINCT.getPermPrefix());
    }

    public boolean hasFocus(Player p) {
        return p.hasPermission(PassivePerkType.FOCUS.getPermPrefix());
    }

    // ---------------------------------------------------------------
    // SWIFT STEP
    // ---------------------------------------------------------------

    public SwiftData getSwiftData(Player p) {
        return swiftData.computeIfAbsent(
                p.getUniqueId(),
                u -> new SwiftData(p.getLocation(), System.currentTimeMillis())
        );
    }

    public void recordSwiftTrigger(Player p) {
        SwiftData data = getSwiftData(p);
        data.setLastLocation(p.getLocation());
        data.setLastTrigger(System.currentTimeMillis());
    }

    public double getSwiftDistance(Player p) {
        int lvl = getLevel(p, PassivePerkType.SWIFT_STEP, 1);
        double base = getDouble("swiftstep", "min-distance", 6.0);
        return Math.max(1.0, base - (lvl - 1));
    }

    public long getSwiftCooldownMs(Player p) {
        int lvl = getLevel(p, PassivePerkType.SWIFT_STEP, 1);
        long base = (long) getDouble("swiftstep", "cooldown", 8); // seconds
        long cd = Math.max(1, base - (lvl - 1));
        return cd * 1000L;
    }

    // ---------------------------------------------------------------
    // ADRENALINE
    // ---------------------------------------------------------------

    public boolean canTriggerAdrenaline(Player p) {
        long now = System.currentTimeMillis();
        long until = adrenalineCooldown.getOrDefault(p.getUniqueId(), 0L);
        return now >= until;
    }

    public double getAdrenalineThresholdHearts() {
        return getDouble("adrenaline", "threshold-hearts", 1.0);
    }

    public void triggerAdrenaline(Player p) {
        int lvl = getLevel(p, PassivePerkType.ADRENALINE, 1);

        int hearts = Math.min(8, lvl / 2 + 1);
        double heal = hearts * 2.0;

        double max = p.getHealthScale() > 0 ? p.getHealthScale() : 20.0;
        double newHealth = Math.min(max, p.getHealth() + heal);
        p.setHealth(newHealth);

        p.getWorld().spawnParticle(
                org.bukkit.Particle.HEART,
                p.getLocation().add(0, 1.0, 0),
                25,
                0.5, 0.7, 0.5,
                0.01
        );
        p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        p.sendTitle("§6Adrenaline!", "§eYou recovered health!", 5, 40, 5);

        long base = (long) getDouble("adrenaline", "cooldown", 45);
        long cd = Math.max(5, base - (lvl - 1) * 2L);
        adrenalineCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000L);
    }

    // ---------------------------------------------------------------
    // SOFT LANDING
    // ---------------------------------------------------------------

    public boolean canTriggerSoftLanding(Player p) {
        long now = System.currentTimeMillis();
        long until = softCooldown.getOrDefault(p.getUniqueId(), 0L);
        return now >= until;
    }

    public double applySoftLanding(Player p, double originalDamage) {
        int lvl = getLevel(p, PassivePerkType.SOFT_LANDING, 1);
        double reductionBase = getDouble("softlanding", "reduction", 0.5); // 50% default
        double factor = Math.max(0.1, 1.0 - reductionBase - (lvl - 1) * 0.05);
        double newDamage = originalDamage * factor;

        p.playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 1.4f);
        p.spawnParticle(org.bukkit.Particle.CLOUD,
                p.getLocation(), 16, 0.5, 0.3, 0.5, 0.0);
        p.sendTitle("&bSoft Landing", "&7Fall damage reduced!", 5, 30, 5);

        long base = (long) getDouble("softlanding", "cooldown", 10);
        long cd = Math.max(2, base - (lvl - 1));
        softCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000L);

        return newDamage;
    }

    // ---------------------------------------------------------------
    // EAGLE SIGHT – highlight nearby entities
    // ---------------------------------------------------------------

    public boolean canTriggerEagleSight(Player p) {
        long now = System.currentTimeMillis();
        long until = eagleCooldown.getOrDefault(p.getUniqueId(), 0L);
        return now >= until;
    }

    public void triggerEagleSight(Player p) {
        int lvl = getLevel(p, PassivePerkType.EAGLE_SIGHT, 1);

        double radius = getDouble("eaglesight", "radius", 12.0) + (lvl - 1);
        int glowTicks = getInt("eaglesight", "glow-ticks", 40);

        for (Entity ent : p.getNearbyEntities(radius, radius, radius)) {
            if (ent instanceof LivingEntity living && ent != p) {
                living.setGlowing(true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (living.isValid()) living.setGlowing(false);
                }, glowTicks);
            }
        }

        String msg = getString("eaglesight", "message",
                "&dArcane&7» &fYour &nEagle Sight&f sharpens.");
        p.sendMessage(msg);

        long base = (long) getDouble("eaglesight", "cooldown", 15);
        long cd = Math.max(3, base - (lvl - 1));
        eagleCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000L);
    }

    // ---------------------------------------------------------------
    // SIXTH SENSE – warning when targeted
    // ---------------------------------------------------------------

    public boolean canTriggerSixthSense(Player p) {
        long now = System.currentTimeMillis();
        long until = sixthCooldown.getOrDefault(p.getUniqueId(), 0L);
        return now >= until;
    }

    public void triggerSixthSense(Player p, Entity mob) {
        String msg = getString("sixthsense", "message",
                "&dArcane&7» &fYou feel something watching you...");
        p.sendMessage(msg);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.4f);

        long base = (long) getDouble("sixthsense", "cooldown", 8);
        int lvl = getLevel(p, PassivePerkType.SIXTH_SENSE, 1);
        long cd = Math.max(2, base - (lvl - 1));
        sixthCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000L);
    }

    // ---------------------------------------------------------------
    // NIGHT INSTINCT – weak NV only in darkness
    // ---------------------------------------------------------------

    public int getNightInstinctLightLevel() {
        return getInt("nightinstinct", "light-level", 7);
    }

    public int getNightInstinctDurationSeconds() {
        return getInt("nightinstinct", "duration", 10);
    }

    public void maybeApplyNightInstinct(Player p) {
        int threshold = getNightInstinctLightLevel();
        if (p.getLocation().getBlock().getLightLevel() > threshold) return;

        int lvl = getLevel(p, PassivePerkType.NIGHT_INSTINCT, 1);
        int seconds = getNightInstinctDurationSeconds() + (lvl - 1) * 2;
        int newDur = seconds * 20;

        PotionEffect existing = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
        if (existing != null && existing.getDuration() > newDur / 2) return;

        p.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                newDur,
                0,
                true,
                false,
                false
        ));
    }

    // ---------------------------------------------------------------
    // CRITICAL MIND – chance to convert hit into “critical style” hit
    // ---------------------------------------------------------------

    public double getCriticalChance(Player p) {
        int lvl = getLevel(p, PassivePerkType.CRITICAL_MIND, 1);
        double base = getDouble("criticalmind", "base-chance", 0.05);
        double per = getDouble("criticalmind", "per-level", 0.02);
        double max = getDouble("criticalmind", "max-chance", 0.40);
        double chance = base + (lvl - 1) * per;
        if (chance > max) chance = max;
        return chance;
    }

    public boolean rollCriticalMind(Player p) {
        double chance = getCriticalChance(p);
        return random.nextDouble() < chance;
    }

    // ---------------------------------------------------------------
    // FOCUS – projectile speed multiplier
    // ---------------------------------------------------------------

    public double getFocusVelocityMultiplier(Player p) {
        int lvl = getLevel(p, PassivePerkType.FOCUS, 1);
        double base = getDouble("focus", "base-multiplier", 0.05);
        double per = getDouble("focus", "per-level", 0.02);
        double max = getDouble("focus", "max-multiplier", 0.50);
        double mult = base + (lvl - 1) * per;
        if (mult > max) mult = max;
        if (mult < 0) mult = 0;
        return mult;
    }

    public boolean isEnabled(Player p, PassivePerkType type) {

    }

    public void setEnabled(Player p, PassivePerkType target, boolean b) {
    }


    // ---------------------------------------------------------------
    // DATA CLASS
    // ---------------------------------------------------------------

    public static class SwiftData {
        private org.bukkit.Location lastLocation;
        private long lastTrigger;

        public SwiftData(org.bukkit.Location lastLocation, long lastTrigger) {
            this.lastLocation = lastLocation;
            this.lastTrigger = lastTrigger;
        }

        public org.bukkit.Location getLastLocation() {
            return lastLocation;
        }

        public void setLastLocation(org.bukkit.Location lastLocation) {
            this.lastLocation = lastLocation;
        }

        public long getLastTrigger() {
            return lastTrigger;
        }

        public void setLastTrigger(long lastTrigger) {
            this.lastTrigger = lastTrigger;
        }
    }
}
