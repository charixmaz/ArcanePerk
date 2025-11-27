package me.charixmaz.arcaneperks.unlock;

import me.charixmaz.arcaneperks.ArcanePerks;
import me.charixmaz.arcaneperks.passive.PassivePerkType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class UnlockPassivePerks {

    private final UnlockManager delegate;

    public UnlockPassivePerks(ArcanePerks plugin, Economy economy) {
        this.delegate = new UnlockManager(
                plugin,
                economy,
                "arcaneperks.unlock.passive.",   // arcaneperks.unlock.passive.eaglesight
                "unlocks.passive"
        );
    }

    public boolean tryUnlock(Player p, PassivePerkType type) {
        return delegate.tryUnlock(p, type.getId());
    }

    public boolean isUnlocked(Player p, PassivePerkType type) {
        return delegate.isUnlocked(p, type.getId());
    }

    public UnlockCost getCost(PassivePerkType type) {
        return delegate.getCost(type.getId());
    }
}
