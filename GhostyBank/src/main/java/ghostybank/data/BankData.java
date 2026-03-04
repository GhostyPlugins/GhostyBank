package ghostybank.data;

import java.util.UUID;

/**
 * Holds all bank-related data for a single player.
 */
public class BankData {

    private final UUID uuid;
    private String playerName;
    private double balance;
    private double totalInterestEarned;
    private int level;

    public BankData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.balance = 0.0;
        this.totalInterestEarned = 0.0;
        this.level = 1;
    }

    public BankData(UUID uuid, String playerName, double balance, double totalInterestEarned, int level) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.balance = balance;
        this.totalInterestEarned = totalInterestEarned;
        this.level = level;
    }

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = Math.max(0, balance); }
    public void addBalance(double amount) { this.balance += amount; }
    public boolean removeBalance(double amount) {
        if (this.balance < amount) return false;
        this.balance -= amount;
        return true;
    }

    public double getTotalInterestEarned() { return totalInterestEarned; }
    public void addInterestEarned(double amount) { this.totalInterestEarned += amount; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    /** Setzt Guthaben, Zinsen und Level auf Standardwerte zurück. */
    public void resetToDefault() {
        this.balance = 0.0;
        this.totalInterestEarned = 0.0;
        this.level = 1;
    }

    /** Setzt nur Zinsen-Statistik zurück. */
    public void resetInterestStats() {
        this.totalInterestEarned = 0.0;
    }

}
