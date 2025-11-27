package me.charixmaz.arcaneperks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ArcaneHubGuiHolder implements InventoryHolder {

    public enum HubType {
        MAIN
    }

    private final HubType type;

    public ArcaneHubGuiHolder(HubType type) {
        this.type = type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public HubType getType() {
        return type;
    }
}
