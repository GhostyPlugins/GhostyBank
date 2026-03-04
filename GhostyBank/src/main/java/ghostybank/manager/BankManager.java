package ghostybank.manager;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.data.BankLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class BankManager {

    private final GhostyBank plugin;
    private final Map<UUID, BankData> bankDataMap = new HashMap<>();
    private final Map<Integer, BankLevel> bankLevels = new TreeMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private DecimalFormat currencyFormat;

    public BankManager(GhostyBank plugin) {
        this.plugin = plugin;
        loadLevels();
        loadData();
        setupCurrencyFormat();
    }

    private void setupCurrencyFormat() {
        String pattern = plugin.getConfig().getString("currency-format", "#,##0.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        currencyFormat = new DecimalFormat(pattern, symbols);
    }

    private void loadLevels() {
        bankLevels.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bank-levels");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            int level = Integer.parseInt(key);
            String name = section.getString(key + ".display-name", "&7Level " + level);
            double maxDeposit = section.getDouble(key + ".max-deposit", 10000);
            double interestRate = section.getDouble(key + ".interest-rate", 0.01);
            double upgradeCost = section.getDouble(key + ".upgrade-cost", -1);
            bankLevels.put(level, new BankLevel(level, name, maxDeposit, interestRate, upgradeCost));
        }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection section = dataConfig.getConfigurationSection("players");
        if (section == null) return;
        for (String uuidStr : section.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String name = section.getString(uuidStr + ".name", "Unknown");
            double balance = section.getDouble(uuidStr + ".balance", 0);
            double totalInterest = section.getDouble(uuidStr + ".totalInterest", 0);
            int level = section.getInt(uuidStr + ".level", 1);
            bankDataMap.put(uuid, new BankData(uuid, name, balance, totalInterest, level));
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, BankData> entry : bankDataMap.entrySet()) {
            String path = "players." + entry.getKey();
            BankData data = entry.getValue();
            dataConfig.set(path + ".name", data.getPlayerName());
            dataConfig.set(path + ".balance", data.getBalance());
            dataConfig.set(path + ".totalInterest", data.getTotalInterestEarned());
            dataConfig.set(path + ".level", data.getLevel());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        saveData();
        loadLevels();
        loadData();
        setupCurrencyFormat();
    }

    // ── Player Data ──────────────────────────────

    public BankData getOrCreate(UUID uuid, String name) {
        return bankDataMap.computeIfAbsent(uuid, id -> new BankData(id, name));
    }

    public BankData getData(UUID uuid) {
        return bankDataMap.get(uuid);
    }

    public boolean hasData(UUID uuid) {
        return bankDataMap.containsKey(uuid);
    }

    public Collection<BankData> getAllData() {
        return bankDataMap.values();
    }

    /**
     * Entfernt einen Spieler komplett aus der Datenbank und den Rankings.
     * Beim nächsten Login wird ein neues leeres Konto erstellt.
     */
    public boolean removePlayer(UUID uuid) {
        if (!bankDataMap.containsKey(uuid)) return false;
        bankDataMap.remove(uuid);
        // Auch aus data.yml entfernen
        dataConfig.set("players." + uuid, null);
        try {
            dataConfig.save(dataFile);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    // ── Bank Levels ──────────────────────────────

    public BankLevel getLevel(int level) {
        return bankLevels.getOrDefault(level, bankLevels.get(1));
    }

    public BankLevel getNextLevel(int currentLevel) {
        return bankLevels.get(currentLevel + 1);
    }

    public int getMaxLevel() {
        return bankLevels.isEmpty() ? 1 : Collections.max(bankLevels.keySet());
    }

    public Map<Integer, BankLevel> getAllLevels() {
        return Collections.unmodifiableMap(bankLevels);
    }

    // ── Operations ───────────────────────────────

    public enum DepositResult { SUCCESS, LIMIT_REACHED, INVALID_AMOUNT }
    public enum WithdrawResult { SUCCESS, NOT_ENOUGH, INVALID_AMOUNT }

    public DepositResult deposit(BankData data, double amount) {
        if (amount <= 0) return DepositResult.INVALID_AMOUNT;
        BankLevel level = getLevel(data.getLevel());
        if (!level.isUnlimitedDeposit() && data.getBalance() + amount > level.getMaxDeposit()) {
            return DepositResult.LIMIT_REACHED;
        }
        data.addBalance(amount);
        return DepositResult.SUCCESS;
    }

    public WithdrawResult withdraw(BankData data, double amount) {
        if (amount <= 0) return WithdrawResult.INVALID_AMOUNT;
        if (data.getBalance() < amount) return WithdrawResult.NOT_ENOUGH;
        data.removeBalance(amount);
        return WithdrawResult.SUCCESS;
    }

    public double getWithdrawalFee(double amount) {
        double feePercent = plugin.getConfig().getDouble("withdrawal-fee", 5.0);
        return amount * (feePercent / 100.0);
    }

    public double getWithdrawalFeePercent() {
        return plugin.getConfig().getDouble("withdrawal-fee", 5.0);
    }

    // ── Interest ─────────────────────────────────

    public void payInterest() {
        for (BankData data : bankDataMap.values()) {
            if (data.getBalance() <= 0) continue;
            BankLevel level = getLevel(data.getLevel());
            double interest = Math.round(data.getBalance() * level.getInterestRate() * 100.0) / 100.0;
            if (interest <= 0) continue;
            data.addBalance(interest);
            data.addInterestEarned(interest);
        }
        saveData();
    }

    // ── Top Lists ────────────────────────────────

    public List<BankData> getTopByBalance(int limit) {
        List<BankData> list = new ArrayList<>(bankDataMap.values());
        list.sort((a, b) -> Double.compare(b.getBalance(), a.getBalance()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<BankData> getTopByInterest(int limit) {
        List<BankData> list = new ArrayList<>(bankDataMap.values());
        list.sort((a, b) -> Double.compare(b.getTotalInterestEarned(), a.getTotalInterestEarned()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<BankData> getTopByLevel(int limit) {
        List<BankData> list = new ArrayList<>(bankDataMap.values());
        list.sort((a, b) -> {
            int cmp = Integer.compare(b.getLevel(), a.getLevel());
            if (cmp != 0) return cmp;
            return Double.compare(b.getBalance(), a.getBalance());
        });
        return list.subList(0, Math.min(limit, list.size()));
    }

    // ── Formatting ───────────────────────────────

    public String formatCurrency(double amount) {
        String symbol = plugin.getConfig().getString("currency-symbol", "$");
        return symbol + currencyFormat.format(amount);
    }

    public String formatMaxDeposit(BankLevel level) {
        if (level.isUnlimitedDeposit()) return "∞";
        return formatCurrency(level.getMaxDeposit());
    }
}
