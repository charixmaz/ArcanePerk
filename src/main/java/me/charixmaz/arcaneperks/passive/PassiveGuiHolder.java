package me.charixmaz.arcaneperks.passive;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PassiveGuiHolder implements InventoryHolder {

    public PassiveGuiHolder(GuiType guiType, GuiType type, PassiveCategory category) {
        this.type = type;
        this.category = category;
    }

    public enum GuiType {
        ROOT,
        CATEGORY
    }

    private final GuiType type;
    private final PassiveCategory category;

    public PassiveGuiHolder(GuiType type) {
        this.type = type;
        this.category = category;
    }

    public GuiType getType() {
        return type;
    }

    public PassiveCategory getCategory() {
        return category;
    }

    @Override
    public Inventory getInventory() {
        return null; // Bukkit fills it, not used
    }
}
