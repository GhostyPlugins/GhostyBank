package ghostybank.data;

import ghostybank.util.ColorUtil;

/**
 * Represents a bank level with its properties.
 */
public class BankLevel {

    private final int level;
    private final String displayName;
    private final double maxDeposit;
    private final double interestRate;
    private final double upgradeCost;

    public BankLevel(int level, String displayName, double maxDeposit, double interestRate, double upgradeCost) {
        this.level = level;
        this.displayName = displayName;
        this.maxDeposit = maxDeposit;
        this.interestRate = interestRate;
        this.upgradeCost = upgradeCost;
    }

    public int getLevel() { return level; }

    public String getDisplayName() {
        return ColorUtil.color(displayName);
    }

    public double getMaxDeposit() { return maxDeposit; }

    public boolean isUnlimitedDeposit() { return maxDeposit < 0; }

    public double getInterestRate() { return interestRate; }

    public double getInterestRatePercent() { return interestRate * 100; }

    public double getUpgradeCost() { return upgradeCost; }

    public boolean isMaxLevel() { return upgradeCost < 0; }
}
