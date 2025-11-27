package me.charixmaz.arcaneperks.unlock;

public class UnlockCost {
    private final int xpLevels;
    private final double money;

    public UnlockCost(int xpLevels, double money) {
        this.xpLevels = xpLevels;
        this.money = money;
    }

    public int getXpLevels() {
        return xpLevels;
    }

    public double getMoney() {
        return money;
    }
}
