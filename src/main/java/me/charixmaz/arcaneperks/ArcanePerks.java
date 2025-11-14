package me.charixmaz.arcaneperks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class ArcanePerks extends JavaPlugin {

    private static ArcanePerks instance;
    private PerkManager perkManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.perkManager = new PerkManager(this);

        // listeners
        Bukkit.getPluginManager().registerEvents(new PerkListener(this), this);

        // /ap command + tabcomplete
        ApCommand ap = new ApCommand(this);
        Objects.requireNonNull(getCommand("ap"), "Command 'ap' not defined").setExecutor(ap);
        Objects.requireNonNull(getCommand("ap")).setTabCompleter(ap);

        // all alias commands -> toggle perks
        String[] aliasCommands = {
                "fd", "nv", "wb", "strp", "flyp", "noh", "kxp", "kinv",
                "nfd", "nfall", "dexp", "ddrops", "godm", "van", "mig",
                "glow", "tele", "ismelt"
        };
        for (String name : aliasCommands) {
            Objects.requireNonNull(getCommand(name), "Command '" + name + "' not defined")
                    .setExecutor(ap);
        }

        // apply potion-type perks every 5 seconds
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyPassivePotionPerks(p);
            }
        }, 20L, 20L * 5);

        // tick: timers + active duration actionbar
        Bukkit.getScheduler().runTaskTimer(this, () -> perkManager.tick(), 20L, 20L);

        getLogger().info("ArcanePerks enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ArcanePerks disabled.");
    }

    public static ArcanePerks getInstance() {
        return instance;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    // Passive potion effects – re-applied smoothly
    public void applyPassivePotionPerks(Player p) {

        // FAST DIGGING – Haste level from config (1–10)
        if (perkManager.hasPerk(p, PerkType.FAST_DIGGING)) {
            int level = perkManager.getEffectLevel(PerkType.FAST_DIGGING, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, 20 * 12, level - 1, true, false, false));
        }

        // NIGHT VISION – long effect, refreshed only when needed
        if (perkManager.hasPerk(p, PerkType.NIGHT_VISION)) {
            PotionEffect current = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (current == null || current.getDuration() < 20 * 30) {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 20 * 60 * 60, 0, true, false, false));
            }
        }

        // WATER BREATHING
        if (perkManager.hasPerk(p, PerkType.WATER_BREATHING)) {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.WATER_BREATHING, 20 * 12, 0, true, false, false));
        }

        // STRENGTH – level can also come from config (default 1)
        if (perkManager.hasPerk(p, PerkType.STRENGTH)) {
            int level = perkManager.getEffectLevel(PerkType.STRENGTH, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH, 20 * 12, level - 1, true, false, false));
        }
    }
}
