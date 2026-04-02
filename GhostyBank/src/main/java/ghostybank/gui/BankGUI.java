package ghostybank.gui;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.data.BankLevel;
import ghostybank.manager.BankManager;
import ghostybank.manager.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class BankGUI {

    private final GhostyBank plugin;
    private final BankManager bankManager;
    private final LangManager lang;

    public BankGUI(GhostyBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.lang = plugin.getLangManager();
    }

    public void open(Player player) {
        BankData data = bankManager.getOrCreate(player.getUniqueId(), player.getName());
        BankLevel level = bankManager.getLevel(data.getLevel());
        BankLevel nextLevel = bankManager.getNextLevel(data.getLevel());

        // Default 5 rows: rows 0-2 content, row 3 buttons, row 4 separator
        int rows = plugin.getConfig().getInt("gui.bank-rows", 5);
        String title = lang.get("bank-gui.title", Map.of(
                "%level%", String.valueOf(data.getLevel()),
                "%level_name%", level.getDisplayName()
        ));

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.BANK);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(inv);

        // Filler
        String fillerMat = lang.getString("bank-gui.filler-material");
        String fillerName = lang.getString("bank-gui.filler-name");
        if (!fillerMat.isBlank()) {
            ItemStack filler = GuiUtil.createItem(fillerMat, fillerName, null);
            for (int i = 0; i < rows * 9; i++) inv.setItem(i, filler);
        }

        // Separator row: last row filled with dark glass panes (separates buttons from player inv)
        String sepMat = lang.getString("bank-gui.separator-material");
        if (!sepMat.isBlank()) {
            ItemStack sep = GuiUtil.createItem(sepMat, lang.getString("bank-gui.separator-name"), null);
            int sepStart = (rows - 1) * 9;
            for (int i = sepStart; i < rows * 9; i++) inv.setItem(i, sep);
        }

        // Balance item
        ItemStack balanceItem = GuiUtil.createItem(
                lang.getString("bank-gui.balance-item-material"),
                lang.get("bank-gui.balance-item-name"),
                lang.getList("bank-gui.balance-item-lore", Map.of(
                        "%balance%", bankManager.formatCurrency(data.getBalance()),
                        "%max_deposit%", bankManager.formatMaxDeposit(level)
                ))
        );
        inv.setItem(lang.getInt("bank-gui.balance-item-slot", 11), balanceItem);

        // Level item
        ItemStack levelItem = GuiUtil.createItem(
                lang.getString("bank-gui.level-item-material"),
                lang.get("bank-gui.level-item-name"),
                lang.getList("bank-gui.level-item-lore", Map.of(
                        "%level%", String.valueOf(data.getLevel()),
                        "%level_name%", level.getDisplayName(),
                        "%interest_rate%", String.format("%.1f", level.getInterestRatePercent())
                ))
        );
        inv.setItem(lang.getInt("bank-gui.level-item-slot", 13), levelItem);

        // Upgrade item
        List<String> upgradeLore = (level.isMaxLevel() || nextLevel == null)
                ? lang.getList("bank-gui.upgrade-item-maxlevel-lore")
                : lang.getList("bank-gui.upgrade-item-lore", Map.of(
                        "%upgrade_cost%", bankManager.formatCurrency(level.getUpgradeCost())));
        ItemStack upgradeItem = GuiUtil.createItem(
                lang.getString("bank-gui.upgrade-item-material"),
                lang.get("bank-gui.upgrade-item-name"),
                upgradeLore
        );
        inv.setItem(lang.getInt("bank-gui.upgrade-item-slot", 15), upgradeItem);

        // Deposit item
        ItemStack depositItem = GuiUtil.createItem(
                lang.getString("bank-gui.deposit-item-material"),
                lang.get("bank-gui.deposit-item-name"),
                lang.getList("bank-gui.deposit-item-lore")
        );
        inv.setItem(lang.getInt("bank-gui.deposit-item-slot", 29), depositItem);

        // Withdraw item
        ItemStack withdrawItem = GuiUtil.createItem(
                lang.getString("bank-gui.withdraw-item-material"),
                lang.get("bank-gui.withdraw-item-name"),
                lang.getList("bank-gui.withdraw-item-lore", Map.of(
                        "%fee%", String.format("%.1f", bankManager.getWithdrawalFeePercent())
                ))
        );
        inv.setItem(lang.getInt("bank-gui.withdraw-item-slot", 33), withdrawItem);

        // Close item
        ItemStack closeItem = GuiUtil.createItem(
                lang.getString("bank-gui.close-item-material"),
                lang.get("bank-gui.close-item-name"),
                lang.getList("bank-gui.close-item-lore")
        );
        inv.setItem(lang.getInt("bank-gui.close-item-slot", 31), closeItem);

        player.openInventory(inv);
    }
}
