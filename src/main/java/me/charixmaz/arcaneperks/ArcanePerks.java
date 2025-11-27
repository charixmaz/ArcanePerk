package me.charixmaz.arcaneperks;

import me.charixmaz.arcaneperks.gui.ArcaneHubGui;
import me.charixmaz.arcaneperks.gui.ArcaneHubGuiListener;
import me.charixmaz.arcaneperks.gui.UpgradeGui;
import me.charixmaz.arcaneperks.gui.UpgradeGuiListener;
import me.charixmaz.arcaneperks.passive.*;
import me.charixmaz.arcaneperks.unlock.UnlockArcanePerks;
import me.charixmaz.arcaneperks.unlock.UnlockPassivePerks;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class ArcanePerks extends JavaPlugin {


    private static ArcanePerks instance;
    private PerkManager perkManager;
    private PerkGui perkGui;
    private PassivePerkManager passivePerkManager;
    private PassiveGui passiveGui;
    private ArcaneHubGui arcaneHubGui;
    private UpgradeGui upgradeGui;
    private UnlockArcanePerks unlockArcane;
    private UnlockPassivePerks unlockPassive;
    private Economy economy; // Vault

    public ArcanePerks() {
        // MUST be empty
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

    public PassivePerkManager getPassivePerkManager() {
        return passivePerkManager;
    }

    public PassiveGui getPassiveGui() {
        return passiveGui;
    }


    public PassiveGui getPassiveGui() {
        return passiveGui;
    }

    public ArcaneHubGui getArcaneHubGui() {
        return arcaneHubGui;
    }
    public UpgradeGui getUpgradeGui() {
        return upgradeGui;
    }
    public UnlockArcanePerks getUnlockArcane() {
        return unlockArcane;
    }
    public UnlockPassivePerks getUnlockPassive() {
        return unlockPassive;
    }

    public class ArcaneCommand implements CommandExecutor {

        private final ArcanePerks plugin;

        public ArcaneCommand(ArcanePerks plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players.");
                return true;
            }
            plugin.getArcaneHubGui().open(p);
            return true;
        }
    }


    @Override
    public void onEnable() {

        instance = this;

        // Vault / Economy setup first (so 'economy' is not null)
        if (!setupEconomy()) {
            getLogger().warning("No Vault economy found. Money unlock costs will be disabled.");
        }

        // configs
        saveDefaultConfig();
        try {
            saveResource("passiveconfig.yml", false);
        } catch (IllegalArgumentException ignored) {
            // file already exists
        }

        // managers / GUIs
        this.perkManager = new PerkManager(this);
        this.perkGui = new PerkGui(this);
        this.passivePerkManager = new PassivePerkManager(this);
        this.passiveGui = new PassiveGui(this, passivePerkManager);

        this.unlockArcane = new UnlockArcanePerks(this, economy);
        this.unlockPassive = new UnlockPassivePerks(this, economy);

        this.arcaneHubGui = new ArcaneHubGui(this);
        this.upgradeGui = new UpgradeGui(this, unlockArcane, unlockPassive);

        this.passivePerkManager = new PassivePerkManager(this);
        this.passiveGui = new PassiveGui(this, passivePerkManager);

        Bukkit.getPluginManager().registerEvents(
                new PassiveHeadListener(this, passivePerkManager), this);
        Bukkit.getPluginManager().registerEvents(
                new PassiveMovementListener(this, passivePerkManager), this); // your existing move logic
        Bukkit.getPluginManager().registerEvents(
                new PassiveGuiListener(this), this);
    

        // listeners
        Bukkit.getPluginManager().registerEvents(new ArcaneHubGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new UpgradeGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(
                new me.charixmaz.arcaneperks.passive.PassiveHeadListener(this, passivePerkManager), this);
        Bukkit.getPluginManager().registerEvents(new PerkListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new PassiveMovementListener(this, passivePerkManager), this);
        Bukkit.getPluginManager().registerEvents(
                new PassiveGuiListener(this), this);

        // main /arcane command
        PluginCommand arcaneCmd = getCommand("arcane");
        if (arcaneCmd != null) {
            arcaneCmd.setExecutor(new ArcaneCommand(this));
        } else {
            getLogger().severe("Command 'arcane' is missing from plugin.yml!");
        }

        // /ap command
        ApCommand ap = new ApCommand(this);
        PluginCommand apCmd = getCommand("ap");
        if (apCmd != null) {
            apCmd.setExecutor(ap);
            apCmd.setTabCompleter(ap);
        } else {
            getLogger().severe("Command 'ap' is missing from plugin.yml!");
        }



        // alias commands – register only if present in plugin.yml
        String[] aliasCommands = {
                "fd","nv","strp","flyp","kxp","kinv",
                "nfall","dexp","ddrops","godm","van","mig",
                "glow","tele","ismelt","spd"
        };

        for (String name : aliasCommands) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(ap);
            } else {
                // Not fatal; just log once so owners know why alias doesn’t work
                getLogger().warning("Alias command '" + name + "' is not defined in plugin.yml. Skipping.");
            }
        }

        // re-apply potion perks every 5 seconds
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyPassivePotionPerks(p);
            }
        }, 20L, 20L * 5);

        // active perk tick every second
        Bukkit.getScheduler().runTaskTimer(this,
                () -> perkManager.tick(), 20L, 20L);

        getLogger().info("ArcanePerks enabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    @Override
    public void onDisable() {
    }

    public void applyPassivePotionPerks(Player p) {
        // your existing haste/speed/strength/night vision logic here…
    }
}
