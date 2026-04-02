package ghostybank.data;

import ghostybank.util.ColorUtil;

public class BankLevel {

    private final int level;
    private final String displayName;
    private final double maxDeposit;
    private final double interestRate;
    private final double upgradeCost;
    private final double maxTotalInterest; // -1 = unbegrenzt

    public BankLevel(int level, String displayName, double maxDeposit,
                     double interestRate, double upgradeCost, double maxTotalInterest) {
        this.level            = level;
        this.displayName      = displayName;
        this.maxDeposit       = maxDeposit;
        this.interestRate     = interestRate;
        this.upgradeCost      = upgradeCost;
        this.maxTotalInterest = maxTotalInterest;
    }

    public int     getLevel()               { return level; }
    public String  getDisplayName()         { return ColorUtil.color(displayName); }
    public double  getMaxDeposit()          { return maxDeposit; }
    public boolean isUnlimitedDeposit()     { return maxDeposit < 0; }
    public double  getInterestRate()        { return interestRate; }
    public double  getInterestRatePercent() { return interestRate * 100; }
    public double  getUpgradeCost()         { return upgradeCost; }
    public boolean isMaxLevel()             { return upgradeCost < 0; }

    /** Maximale Gesamt-Zinsen die auf diesem Level verdient werden können. -1 = unbegrenzt. */
    public double  getMaxTotalInterest()    { return maxTotalInterest; }
    public boolean hasInterestCap()         { return maxTotalInterest >= 0; }
}
