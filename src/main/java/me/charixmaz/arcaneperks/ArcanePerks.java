package me.charixmaz.arcaneperks;

import me.charixmaz.arcaneperks.passive.PassiveMovementListener;
import me.charixmaz.arcaneperks.passive.PassivePerkManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class ArcanePerks extends JavaPlugin {

    private static ArcanePerks instance;

    private PerkManager perkManager;
    private PerkGui perkGui;

    // passive system
    private PassivePerkManager passivePerkManager;

    public static ArcanePerks getInstance() {
        return instance;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    public PerkGui getPerkGui() {
        return perkGui;
    }

    public PassivePerkManager getPassivePerkManager() {
        return passivePerkManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        // passive config is optional – if missing in jar, this will throw and be ignored
        try {
            saveResource("passiveconfig.yml", false);
        } catch (IllegalArgumentException ignored) {
        }

        // managers
        this.perkManager = new PerkManager(this);
        this.perkGui = new PerkGui(this);
        this.passivePerkManager = new PassivePerkManager(this);

        // listeners
        Bukkit.getPluginManager().registerEvents(new PerkListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new PassiveMovementListener(this, passivePerkManager), this);

        // /ap and aliases
        ApCommand ap = new ApCommand(this);
        Objects.requireNonNull(getCommand("ap"), "Command 'ap' not defined").setExecutor(ap);
        Objects.requireNonNull(getCommand("ap")).setTabCompleter(ap);

        String[] aliasCommands = {
                "fd", "nv", "strp", "flyp", "kxp", "kinv",
                "nfall", "dexp", "ddrops", "godm", "van", "mig",
                "glow", "tele", "ismelt", "spd"
        };
        for (String name : aliasCommands) {
            Objects.requireNonNull(getCommand(name), "Command '" + name + "' not defined")
                    .setExecutor(ap);
        }

        // tick: re-apply potion effects for active perks
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyPassivePotionPerks(p);
            }
        }, 20L, 20L * 5);

        // tick: active perk durations + actionbar
        Bukkit.getScheduler().runTaskTimer(this, () -> perkManager.tick(), 20L, 20L);

        getLogger().info("ArcanePerks enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ArcanePerks disabled.");
    }

    /**
     * Potion effects for ACTIVE perks (haste, speed, strength, nightvision).
     */
    public void applyPassivePotionPerks(Player p) {
        if (perkManager == null) return;

        // Fast Digging -> Haste
        if (perkManager.hasPerk(p, PerkType.FAST_DIGGING)) {
            int level = perkManager.getEffectLevel(PerkType.FAST_DIGGING, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, 20 * 12, level - 1, true, false, false));
        }

        // Speed
        if (perkManager.hasPerk(p, PerkType.SPEED)) {
            int level = perkManager.getEffectLevel(PerkType.SPEED, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 20 * 12, level - 1, true, false, false));
        }

        // Strength
        if (perkManager.hasPerk(p, PerkType.STRENGTH)) {
            int level = perkManager.getEffectLevel(PerkType.STRENGTH, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH, 20 * 12, level - 1, true, false, false));
        }

        // Night Vision – very long duration, refreshed only when needed
        if (perkManager.hasPerk(p, PerkType.NIGHT_VISION)) {
            PotionEffect current = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (current == null || current.getDuration() < 20 * 30) {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION,
                        20 * 60 * 60,
                        0,
                        true,
                        false,
                        false
                ));
            }
        }
    }
}
