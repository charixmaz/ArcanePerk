package me.charixmaz.arcaneperks.passive;

import org.bukkit.Material;

public enum PassiveCategory {
    HEAD("Head", "Passive perks for vision / awareness", Material.CARVED_PUMPKIN),
    TORSO("Torso", "Passive perks for survivability / armor", Material.IRON_CHESTPLATE),
    LEGS("Legs", "Passive perks for mobility / jumps", Material.IRON_LEGGINGS),
    FEET("Feet", "Passive perks for movement / walking", Material.IRON_BOOTS);

    private final String displayName;
    private final String description;
    private final Material icon;

    PassiveCategory(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }
}
