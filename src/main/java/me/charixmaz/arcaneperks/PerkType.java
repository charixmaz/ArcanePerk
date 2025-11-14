package me.charixmaz.arcaneperks;

public enum PerkType {

    FAST_DIGGING("fastdigging", "Fast Digging"),
    NIGHT_VISION("nightvision", "Night Vision"),
    WATER_BREATHING("waterbreathing", "Water Breathing"),
    STRENGTH("strength", "Strength"),
    FLY("fly", "Fly"),
    NO_HUNGER("nohunger", "No Hunger"),
    KEEP_XP("keepxp", "Keep Exp"),
    KEEP_INVENTORY("keepinventory", "Keep Inventory"),
    NO_FIRE_DAMAGE("nofiredamage", "No Fire Damage"),
    NO_FALL_DAMAGE("nofalldamage", "No Fall Damage"),
    DOUBLE_EXP("doubleexp", "Double Exp"),
    DOUBLE_MOB_DROPS("doublemobdrops", "Double Mob Drops"),
    GOD("god", "God Mode"),
    VANISH("vanish", "Vanish"),
    MOBS_IGNORE("mobsignore", "Mobs Ignore"),
    GLOWING("glowing", "Glowing"),
    TELEKINESIS("telekinesis", "Telekinesis"),
    INSTANT_SMELT("instantsmelt", "Instant Smelt");

    private final String id;
    private final String displayName;

    PerkType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermissionNode() {
        return "arcaneperks." + id;
    }

    public static PerkType byId(String id) {
        for (PerkType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
