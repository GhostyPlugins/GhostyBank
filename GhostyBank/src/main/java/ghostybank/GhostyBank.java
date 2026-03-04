package ghostybank;

import ghostybank.commands.*;
import ghostybank.gui.*;
import ghostybank.listener.GUIListener;
import ghostybank.manager.BankManager;
import ghostybank.manager.LangManager;
import ghostybank.manager.VaultManager;
import ghostybank.task.InterestTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;

public class GhostyBank extends JavaPlugin {

    private static GhostyBank instance;

    private BankManager bankManager;
    private VaultManager vaultManager;
    private LangManager langManager;
    private GUIListener guiListener;

    private BankGUI bankGUI;
    private BalanceGUI balanceGUI;
    private UpgradeGUI upgradeGUI;
    private PayGUI payGUI;
    private TopGUI topGUI;
    private AdminGUI adminGUI;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveLangIfAbsent("lang/de_DE.yml");
        saveLangIfAbsent("lang/en_US.yml");

        langManager  = new LangManager(this);
        vaultManager = new VaultManager(this);
        bankManager  = new BankManager(this);

        if (!vaultManager.isEnabled()) {
            getLogger().severe("No Vault economy found! Disabling GhostyBank.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        bankGUI    = new BankGUI(this);
        balanceGUI = new BalanceGUI(this);
        upgradeGUI = new UpgradeGUI(this);
        payGUI     = new PayGUI(this);
        topGUI     = new TopGUI(this);
        adminGUI   = new AdminGUI(this);

        guiListener = new GUIListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        getCommand("bank").setExecutor(new BankCommand(this));
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("top").setExecutor(new TopCommand(this));
        getCommand("gadmin").setExecutor(new AdminCommand(this));
        getCommand("greload").setExecutor(new ReloadCommand(this));

        startInterestTask();

        getLogger().info("GhostyBank enabled! Language: " + getConfig().getString("language", "de_DE"));
    }

    private void saveLangIfAbsent(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            InputStream stream = getResource(path);
            if (stream != null) saveResource(path, false);
        }
    }

    private void startInterestTask() {
        int intervalMinutes = getConfig().getInt("interest-interval-minutes", 60);
        long intervalTicks  = intervalMinutes * 60L * 20L;
        new InterestTask(this).runTaskTimer(this, intervalTicks, intervalTicks);
        getLogger().info("Interest task started – interval: " + intervalMinutes + " min.");
    }

    @Override
    public void onDisable() {
        if (bankManager != null) bankManager.saveData();
        getLogger().info("GhostyBank disabled. Data saved.");
    }

    // ── Getters ──────────────────────────────────

    public static GhostyBank getInstance() { return instance; }
    public BankManager  getBankManager()  { return bankManager; }
    public VaultManager getVaultManager() { return vaultManager; }
    public LangManager  getLangManager()  { return langManager; }
    public GUIListener  getGuiListener()  { return guiListener; }
    public BankGUI      getBankGUI()      { return bankGUI; }
    public BalanceGUI   getBalanceGUI()   { return balanceGUI; }
    public UpgradeGUI   getUpgradeGUI()   { return upgradeGUI; }
    public PayGUI       getPayGUI()       { return payGUI; }
    public TopGUI       getTopGUI()       { return topGUI; }
    public AdminGUI     getAdminGUI()     { return adminGUI; }
}
