package me.charixmaz.arcaneperks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class UpgradeGuiHolder implements InventoryHolder {

    private final UpgradeGui gui;

    public UpgradeGuiHolder(UpgradeGui gui) {
        this.gui = gui;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public UpgradeGui getGui() {
        return gui;
    }
}
