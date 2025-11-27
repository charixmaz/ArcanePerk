package me.charixmaz.arcaneperks;

import me.charixmaz.arcaneperks.passive.PassiveGui;
import me.charixmaz.arcaneperks.passive.PassiveGuiListener;
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

    private PassivePerkManager passivePerkManager;
    private PassiveGui passiveGui;

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

    public PassiveGui getPassiveGui() {
        return passiveGui;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        try {
            saveResource("passiveconfig.yml", false);
        } catch (IllegalArgumentException ignored) {}

        this.perkManager = new PerkManager(this);
        this.perkGui = new PerkGui(this);

        this.passivePerkManager = new PassivePerkManager(this);
        this.passiveGui = new PassiveGui(this, passivePerkManager);

        Bukkit.getPluginManager().registerEvents(new PerkListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new PassiveMovementListener(this, passivePerkManager), this);
        Bukkit.getPluginManager().registerEvents(
                new PassiveGuiListener(this), this);

        ApCommand ap = new ApCommand(this);
        Objects.requireNonNull(getCommand("ap")).setExecutor(ap);
        Objects.requireNonNull(getCommand("ap")).setTabCompleter(ap);

        String[] aliasCommands = {
                "fd","nv","strp","flyp","kxp","kinv",
                "nfall","dexp","ddrops","godm","van","mig",
                "glow","tele","ismelt","spd"
        };
        for (String name : aliasCommands) {
            Objects.requireNonNull(getCommand(name)).setExecutor(ap);
        }

        // re-apply potion perks
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyPassivePotionPerks(p);
            }
        }, 20L, 20L * 5);

        // active perk tick
        Bukkit.getScheduler().runTaskTimer(this, () -> perkManager.tick(), 20L, 20L);
    }

    @Override
    public void onDisable() {
    }

    public void applyPassivePotionPerks(Player p) {
        // your existing haste/speed/strength/nightvision logic here…
    }
}
