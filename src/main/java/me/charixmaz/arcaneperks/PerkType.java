package me.charixmaz.arcaneperks;

public enum PerkType {

    FAST_DIGGING("fastdigging", "Fast Digging"),
    NIGHT_VISION("nightvision", "Night Vision"),
    STRENGTH("strength", "Strength"),
    FLY("fly", "Fly"),
    KEEP_XP("keepxp", "Keep Exp"),
    KEEP_INVENTORY("keepinventory", "Keep Inventory"),
    NO_FALL_DAMAGE("nofalldamage", "Soft Landing"),
    DOUBLE_EXP("doubleexp", "Double Exp"),
    DOUBLE_MOB_DROPS("doublemobdrops", "Double Mob Drops"),
    GOD("god", "God Mode"),
    VANISH("vanish", "Ghost Mode"),
    MOBS_IGNORE("mobsignore", "Mobs Ignore"),
    GLOWING("glowing", "Glowing"),
    TELEKINESIS("telekinesis", "Telekinesis"),
    INSTANT_SMELT("instantsmelt", "Auto Smelt"),
    SPEED("speed", "Speed");

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
