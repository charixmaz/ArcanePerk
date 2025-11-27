package me.charixmaz.arcaneperks.unlock;

import me.charixmaz.arcaneperks.ArcanePerks;
import me.charixmaz.arcaneperks.PerkType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class UnlockArcanePerks {

    private final UnlockManager delegate;

    public UnlockArcanePerks(ArcanePerks plugin, Economy economy) {
        this.delegate = new UnlockManager(
                plugin,
                economy,
                "arcaneperks.unlock.arcane.",   // permission form: arcaneperks.unlock.arcane.fastdigging
                "unlocks.arcane"                // config root
        );
    }

    public boolean tryUnlock(Player p, PerkType type) {
        return delegate.tryUnlock(p, type.getId());
    }

    public boolean isUnlocked(Player p, PerkType type) {
        return delegate.isUnlocked(p, type.getId());
    }

    public UnlockCost getCost(PerkType type) {
        return delegate.getCost(type.getId());
    }
}
