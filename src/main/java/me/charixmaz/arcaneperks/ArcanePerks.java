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
    private PerkGui perkGui;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.perkManager = new PerkManager(this);
        this.perkGui = new PerkGui(this);

        Bukkit.getPluginManager().registerEvents(new PerkListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);

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

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyPassivePotionPerks(p);
            }
        }, 20L, 20L * 5);

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

    public PerkGui getPerkGui() {
        return perkGui;
    }

    public void applyPassivePotionPerks(Player p) {

        if (perkManager.hasPerk(p, PerkType.FAST_DIGGING)) {
            int level = perkManager.getEffectLevel(PerkType.FAST_DIGGING, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, 20 * 12, level - 1, true, false, false));
        }

        if (perkManager.hasPerk(p, PerkType.SPEED)) {
            int level = perkManager.getEffectLevel(PerkType.SPEED, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 20 * 12, level - 1, true, false, false));
        }

        if (perkManager.hasPerk(p, PerkType.NIGHT_VISION)) {
            PotionEffect current = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (current == null || current.getDuration() < 20 * 30) {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 20 * 60 * 60, 0, true, false, false));
            }
        }

        if (perkManager.hasPerk(p, PerkType.STRENGTH)) {
            int level = perkManager.getEffectLevel(PerkType.STRENGTH, p, 1);
            if (level < 1) level = 1;
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH, 20 * 12, level - 1, true, false, false));
        }
    }
}
