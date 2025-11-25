package me.charixmaz.arcaneperks.passive;

public enum PassivePerkType {

    SWIFT_STEP("swiftstep", "arcaneperks.passive.swiftstep"),
    ADRENALINE("adrenaline", "arcaneperks.passive.adrenaline"),
    SOFT_LANDING("softlanding", "arcaneperks.passive.softlanding");

    private final String id;
    private final String permPrefix;

    PassivePerkType(String id, String permPrefix) {
        this.id = id;
        this.permPrefix = permPrefix;
    }

    public String getId() {
        return id;
    }

    public String getPermPrefix() {
        return permPrefix;
    }
}
