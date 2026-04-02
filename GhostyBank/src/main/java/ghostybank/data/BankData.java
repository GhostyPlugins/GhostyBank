package ghostybank.data;

import java.util.UUID;

public class BankData {

    private final UUID uuid;
    private String playerName;
    private double balance;
    private double totalInterestEarned;
    private double levelInterestEarned;   // Zinsen auf dem AKTUELLEN Level (wird bei Upgrade zurückgesetzt)
    private boolean interestCapNotified;    // Verhindert Spam-Nachrichten beim Cap
    private int level;

    public BankData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.balance = 0.0;
        this.totalInterestEarned = 0.0;
        this.levelInterestEarned = 0.0;
        this.interestCapNotified = false;
        this.level = 1;
    }

    public BankData(UUID uuid, String playerName, double balance,
                    double totalInterestEarned, double levelInterestEarned, int level) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.balance = balance;
        this.totalInterestEarned = totalInterestEarned;
        this.levelInterestEarned = levelInterestEarned;
        this.level = level;
    }

    // ── Getters / Setters ────────────────────────

    public UUID   getUuid()       { return uuid; }
    public String getPlayerName() { return playerName; }
    public void   setPlayerName(String name) { this.playerName = name; }

    public double getBalance()  { return balance; }
    public void   setBalance(double balance) { this.balance = Math.max(0, balance); }
    public void   addBalance(double amount)  { this.balance += amount; }
    public boolean removeBalance(double amount) {
        if (this.balance < amount) return false;
        this.balance -= amount;
        return true;
    }

    public double getTotalInterestEarned() { return totalInterestEarned; }
    public double getLevelInterestEarned() { return levelInterestEarned; }

    public void addInterestEarned(double amount) {
        this.totalInterestEarned += amount;
        this.levelInterestEarned += amount;
    }

    public int  getLevel() { return level; }
    public void setLevel(int level) {
        this.level = level;
        this.levelInterestEarned = 0.0; // Cap zurücksetzen beim Upgrade
    }

    public boolean isInterestCapNotified() { return interestCapNotified; }
    public void setInterestCapNotified(boolean v) { this.interestCapNotified = v; }

    public void resetToDefault() {
        this.balance = 0.0;
        this.totalInterestEarned = 0.0;
        this.levelInterestEarned = 0.0;
        this.interestCapNotified = false;
        this.level = 1;
    }

    public void resetInterestStats() {
        this.totalInterestEarned = 0.0;
        this.levelInterestEarned = 0.0;
    }
}
