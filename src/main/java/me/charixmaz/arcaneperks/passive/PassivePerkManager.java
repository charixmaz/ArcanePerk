package me.charixmaz.arcaneperks.passive;

import me.charixmaz.arcaneperks.ArcanePerks;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PassivePerkManager {

    private final ArcanePerks plugin;

    // passiveconfig.yml
    private File passiveConfigFile;
    private FileConfiguration passiveConfig;

    // for SwiftStep – last position + last trigger time per player
    private final Map<UUID, SwiftData> swiftData = new HashMap<>();
    // cooldown for Adrenaline and SoftLanding
    private final Map<UUID, Long> adrenalineCooldown = new HashMap<>();
    private final Map<UUID, Long> softCooldown = new HashMap<>();

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
            // copy default from jar if present
            try {
                plugin.saveResource("passiveconfig.yml", false);
            } catch (IllegalArgumentException ignored) {
                // no default in jar; will create empty file in memory
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

    public ConfigurationSection section(String id) {
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

    public boolean hasSwiftStep(Player p) {
        return p.hasPermission(PassivePerkType.SWIFT_STEP.getPermPrefix());
    }

    public boolean hasAdrenaline(Player p) {
        return p.hasPermission(PassivePerkType.ADRENALINE.getPermPrefix());
    }

    public boolean hasSoftLanding(Player p) {
        return p.hasPermission(PassivePerkType.SOFT_LANDING.getPermPrefix());
    }

    // ---------------------------------------------------------------
    // SWIFT STEP
    // ---------------------------------------------------------------

    public SwiftData getSwiftData(Player p) {
        return swiftData.computeIfAbsent(p.getUniqueId(), u -> new SwiftData(p.getLocation(), 0L));
    }

    public void recordSwiftTrigger(Player p) {
        SwiftData data = getSwiftData(p);
        data.setLastLocation(p.getLocation());
        data.setLastTrigger(System.currentTimeMillis());
    }

    /** cooldown in ms, from passiveconfig.yml: swiftstep.cooldown */
    public long getSwiftCooldownMs(Player p) {
        int lvl = getLevel(p, PassivePerkType.SWIFT_STEP, 1);
        long base = (long) getDouble("swiftstep", "cooldown", 10); // seconds
        long cd = Math.max(1, base - (lvl - 1));                  // higher level = shorter cd
        return cd * 1000L;
    }

    /** distance in blocks, from passiveconfig.yml: swiftstep.min-distance */
    public double getSwiftDistance(Player p) {
        int lvl = getLevel(p, PassivePerkType.SWIFT_STEP, 1);
        double base = getDouble("swiftstep", "min-distance", 6.0);
        return Math.max(1.0, base - (lvl - 1));
    }

    // ---------------------------------------------------------------
    // ADRENALINE
    // ---------------------------------------------------------------

    public boolean canTriggerAdrenaline(Player p) {
        long now = System.currentTimeMillis();
        long until = adrenalineCooldown.getOrDefault(p.getUniqueId(), 0L);
        return now >= until;
    }

    /** threshold in hearts (not HP), from passiveconfig.yml: adrenaline.threshold-hearts */
    public double getAdrenalineThresholdHearts() {
        return getDouble("adrenaline", "threshold-hearts", 1.0); // default 1 heart
    }

    public void triggerAdrenaline(Player p) {
        int lvl = getLevel(p, PassivePerkType.ADRENALINE, 1);

        // heal scaling by level (max ~4 hearts)
        int hearts = Math.min(8, lvl / 2 + 1); // 1,1,2,2,3,3,... up to 4 hearts
        double heal = hearts * 2.0;

        double max = p.getHealthScale() > 0 ? p.getHealthScale() : 20.0;
        double newHealth = Math.min(max, p.getHealth() + heal);
        p.setHealth(newHealth);

        // visual + audio feedback
        p.getWorld().spawnParticle(
                org.bukkit.Particle.HEART,
                p.getLocation().add(0, 1.0, 0),
                25,
                0.5, 0.7, 0.5,
                0.01
        );
        p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        p.sendTitle("§6Adrenaline!", "§eYou recovered health!", 5, 40, 5);

        long base = (long) getDouble("adrenaline", "cooldown", 45); // seconds
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

    /** damage reduction factor from passiveconfig.yml: softlanding.reduction (0.3 = 30%) */
    public double getSoftLandingReductionBase() {
        return getDouble("softlanding", "reduction", 0.3);
    }

    public double applySoftLanding(Player p, double originalDamage) {
        int lvl = getLevel(p, PassivePerkType.SOFT_LANDING, 1);
        double reduction = getSoftLandingReductionBase(); // 0.3 default
        double factor = Math.max(0.0, 1.0 - reduction - (lvl - 1) * 0.03);
        double newDamage = originalDamage * factor;

        // feedback
        p.playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 1.4f);
        p.spawnParticle(org.bukkit.Particle.CLOUD,
                p.getLocation(), 12, 0.4, 0.2, 0.4, 0.0);

        long base = (long) getDouble("softlanding", "cooldown", 10);
        long cd = Math.max(2, base - (lvl - 1));
        softCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000L);

        return newDamage;
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
