package ghostybank.manager;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.data.BankLevel;
import ghostybank.storage.IStorage;
import ghostybank.storage.MySQLStorage;
import ghostybank.storage.YamlStorage;
import org.bukkit.configuration.ConfigurationSection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class BankManager {

    private final GhostyBank plugin;
    private final Map<UUID, BankData> bankDataMap = new HashMap<>();
    private final Map<Integer, BankLevel> bankLevels = new TreeMap<>();
    private IStorage storage;
    private DecimalFormat currencyFormat;

    public BankManager(GhostyBank plugin) {
        this.plugin = plugin;
        loadLevels();
        initStorage();
        loadData();
        setupCurrencyFormat();
    }

    // ── Storage ──────────────────────────────────

    private void initStorage() {
        String type = plugin.getConfig().getString("storage.type", "yaml").toLowerCase();
        if (type.equals("mysql")) {
            storage = new MySQLStorage(plugin);
        } else {
            storage = new YamlStorage(plugin);
        }
        storage.init();
    }

    private void loadData() {
        bankDataMap.clear();
        for (BankData data : storage.loadAll()) {
            bankDataMap.put(data.getUuid(), data);
        }
    }

    public void saveData() {
        storage.saveAll(bankDataMap.values());
    }

    /** Einzelnen Spieler speichern (z.B. nach Rename oder Admin-Aktion) */
    public void savePlayer(BankData data) {
        storage.save(data);
    }

    /** Speichert einen einzelnen Spieler asynchron */
    public void savePlayerAsync(BankData data) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> storage.save(data));
    }

    public void closeStorage() {
        storage.close();
    }

    public void reload() {
        saveData();
        loadLevels();
        // Storage-Typ könnte sich geändert haben → neu initialisieren
        if (storage != null) storage.close();
        initStorage();
        loadData();
        setupCurrencyFormat();
    }

    // ── Levels ───────────────────────────────────

    private void loadLevels() {
        bankLevels.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bank-levels");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            int level         = Integer.parseInt(key);
            String name       = section.getString(key + ".display-name", "&7Level " + level);
            double maxDeposit = section.getDouble(key + ".max-deposit", 10000);
            double rate       = section.getDouble(key + ".interest-rate", 0.01);
            double cost       = section.getDouble(key + ".upgrade-cost", -1);
            // max-total-interest: -1 = unbegrenzt, sonst max. Gesamt-Zinsen auf diesem Level
            double maxTotalInterest = section.getDouble(key + ".max-total-interest", -1);
            bankLevels.put(level, new BankLevel(level, name, maxDeposit, rate, cost, maxTotalInterest));
        }
    }

    private void setupCurrencyFormat() {
        String pattern = plugin.getConfig().getString("currency-format", "#,##0.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        currencyFormat = new DecimalFormat(pattern, symbols);
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

    public boolean removePlayer(UUID uuid) {
        if (!bankDataMap.containsKey(uuid)) return false;
        bankDataMap.remove(uuid);
        storage.remove(uuid);
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

    public enum DepositResult  { SUCCESS, LIMIT_REACHED, INVALID_AMOUNT }
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
        return amount * (plugin.getConfig().getDouble("withdrawal-fee", 5.0) / 100.0);
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
            return cmp != 0 ? cmp : Double.compare(b.getBalance(), a.getBalance());
        });
        return list.subList(0, Math.min(limit, list.size()));
    }

    // ── Formatting ───────────────────────────────

    public String formatCurrency(double amount) {
        return plugin.getConfig().getString("currency-symbol", "$") + currencyFormat.format(amount);
    }

    public String formatMaxDeposit(BankLevel level) {
        return level.isUnlimitedDeposit() ? "∞" : formatCurrency(level.getMaxDeposit());
    }
}
