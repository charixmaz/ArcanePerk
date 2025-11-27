package me.charixmaz.arcaneperks.passive;

import org.jetbrains.annotations.NotNull;

public enum PassivePerkType {

    // ---------- HEAD ----------
    EAGLE_SIGHT(PassiveCategory.HEAD, "&eEagle Sight", 10),
    SIXTH_SENSE(PassiveCategory.HEAD, "&dSixth Sense", 10),
    CRITICAL_MIND(PassiveCategory.HEAD, "&cCritical Mind", 10),
    NIGHT_INSTINCT(PassiveCategory.HEAD, "&5Night Instinct", 10),
    FOCUS(PassiveCategory.HEAD, "&bFocus", 10),

    // ---------- TORSO ----------
    ADRENALINE(PassiveCategory.TORSO, "&cAdrenaline", 10),
    IRON_SKIN(PassiveCategory.TORSO, "&7Iron Skin", 10),
    FIRE_HEART(PassiveCategory.TORSO, "&6Fire Heart", 10),
    BLOOD_FURNACE(PassiveCategory.TORSO, "&4Blood Furnace", 10),
    METABOLIC_RECOVERY(PassiveCategory.TORSO, "&aMetabolic Recovery", 10),
    COLD_RESISTANCE(PassiveCategory.TORSO, "&bCold Resistance", 10),

    // ---------- LEGS ----------
    SOFT_LANDING(PassiveCategory.LEGS, "&aSoft Landing", 10),
    CLIMBERS_GRIP(PassiveCategory.LEGS, "&2Climber's Grip", 10),
    MOMENTUM(PassiveCategory.LEGS, "&eMomentum", 10),
    STEP_ASSIST(PassiveCategory.LEGS, "&fStep Assist", 10),
    DASH_INSTINCT(PassiveCategory.LEGS, "&cDash Instinct", 10),

    // ---------- FEET ----------
    SWIFT_SPEED(PassiveCategory.FEET, "&dSwift Speed", 10),
    SILENT_STEPS(PassiveCategory.FEET, "&7Silent Steps", 10),
    STABLE_BALANCE(PassiveCategory.FEET, "&aStable Balance", 10),
    PATHFINDER(PassiveCategory.FEET, "&2Pathfinder", 10),
    PREDATORS_TREAD(PassiveCategory.FEET, "&5Predator's Tread", 10),
    ROOTED(PassiveCategory.FEET, "&cRooted", 10);

    public static PassivePerkType SWIFT_STEP;
    private final PassiveCategory category;
    private final String displayName;
    private final int maxLevel;

    PassivePerkType(PassiveCategory category, String displayName, int maxLevel) {
        this.category = category;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
    }

    public PassiveCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public @NotNull String getMaxLevel() {
        return maxLevel;
    }

    public @NotNull String getPermPrefix() {
    }
}
