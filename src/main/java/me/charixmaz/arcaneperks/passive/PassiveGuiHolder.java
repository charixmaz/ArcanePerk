package me.charixmaz.arcaneperks.passive;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PassiveGuiHolder implements InventoryHolder {

    public enum GuiType {
        ROOT,
        CATEGORY
    }

    private final GuiType type;
    private final PassiveCategory category; // null for ROOT

    public PassiveGuiHolder(GuiType type) {
        this(type, null);
    }

    public PassiveGuiHolder(GuiType type, PassiveCategory category) {
        this.type = type;
        this.category = category;
    }

    @Override
    public Inventory getInventory() {
        return null; // not used
    }

    public GuiType getType() {
        return type;
    }

    public PassiveCategory getCategory() {
        return category;
    }
}
