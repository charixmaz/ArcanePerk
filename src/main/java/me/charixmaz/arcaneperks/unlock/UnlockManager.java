package me.charixmaz.arcaneperks.unlock;

import me.charixmaz.arcaneperks.ArcanePerks;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class UnlockManager {

    private final ArcanePerks plugin;
    private final Economy economy;
    private final String permissionPrefix; // e.g. "arcaneperks.perk."
    private final String configRoot;       // e.g. "unlocks.arcane"

    public UnlockManager(ArcanePerks plugin, Economy economy,
                         String permissionPrefix, String configRoot) {
        this.plugin = plugin;
        this.economy = economy;
        this.permissionPrefix = permissionPrefix;
        this.configRoot = configRoot;
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // ----------------------------------------------------------------
    // Read cost from config
    // unlcoks.arcane.<id>.xp
    // unlcoks.arcane.<id>.money
    // ----------------------------------------------------------------
    public UnlockCost getCost(String perkId) {
        ConfigurationSection sec =
                plugin.getConfig().getConfigurationSection(configRoot + "." + perkId);
        if (sec == null) {
            // default 70 levels, 0 money
            return new UnlockCost(70, 0);
        }
        int xp = sec.getInt("xp", 70);
        double money = sec.getDouble("money", 0);
        return new UnlockCost(xp, money);
    }

    public boolean isUnlocked(Player p, String perkId) {
        String perm = permissionPrefix + perkId;
        return p.hasPermission(perm);
    }

    // Grant perm for *base* level (for example level 1)
    public void grantUnlock(Player p, String perkId) {
        String perm = permissionPrefix + perkId;
        // This assumes LuckPerms is used and respects /lp user ... permission set
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                "lp user " + p.getName() + " permission set " + perm + " true"
        );
    }

    public boolean tryUnlock(Player p, String perkId) {
        if (isUnlocked(p, perkId)) {
            p.sendMessage(cc("&dArcane &7> &fYou already unlocked &b" + perkId + "&f."));
            return false;
        }

        UnlockCost cost = getCost(perkId);

        int xp = cost.getXpLevels();
        double money = cost.getMoney();

        if (p.getLevel() < xp) {
            p.sendMessage(cc("&cYou need at least &e" + xp + " &clevels."));
            return false;
        }

        if (money > 0 && (economy == null || economy.getBalance(p) < money)) {
            p.sendMessage(cc("&cYou need &e" + money + " &cmore money."));
            return false;
        }

        // charge
        p.setLevel(p.getLevel() - xp);
        if (money > 0 && economy != null) {
            economy.withdrawPlayer(p, money);
        }

        grantUnlock(p, perkId);

        p.sendMessage(cc("&dArcane &7> &aUnlocked &b" + perkId + "&a!"));
        return true;
    }
}
