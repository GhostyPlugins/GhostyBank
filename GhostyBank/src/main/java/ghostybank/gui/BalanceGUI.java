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

import java.util.Map;

public class BalanceGUI {

    private final GhostyBank plugin;
    private final BankManager bankManager;
    private final LangManager lang;

    public BalanceGUI(GhostyBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.lang = plugin.getLangManager();
    }

    public void open(Player viewer, BankData data) {
        BankLevel level = bankManager.getLevel(data.getLevel());
        int rows = plugin.getConfig().getInt("gui.balance-rows", 3);

        // Eigenes Konto → "Dein Konto", fremdes Konto → Spielername im Titel
        boolean isSelf = viewer.getUniqueId().equals(data.getUuid());
        String titleKey = isSelf ? "balance-gui.title-self" : "balance-gui.title";
        String title = lang.get(titleKey, Map.of("%player%", data.getPlayerName()));

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.BALANCE);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(inv);

        // Filler
        String fillerMat = lang.getString("balance-gui.filler-material");
        if (!fillerMat.isBlank()) {
            ItemStack filler = GuiUtil.createItem(fillerMat, lang.getString("balance-gui.filler-name"), null);
            for (int i = 0; i < rows * 9; i++) inv.setItem(i, filler);
        }

        // Balance
        inv.setItem(lang.getInt("balance-gui.balance-item-slot", 11), GuiUtil.createItem(
                lang.getString("balance-gui.balance-item-material"),
                lang.get("balance-gui.balance-item-name"),
                lang.getList("balance-gui.balance-item-lore", Map.of(
                        "%balance%", bankManager.formatCurrency(data.getBalance())
                ))
        ));

        // Total interest
        inv.setItem(lang.getInt("balance-gui.interest-item-slot", 13), GuiUtil.createItem(
                lang.getString("balance-gui.interest-item-material"),
                lang.get("balance-gui.interest-item-name"),
                lang.getList("balance-gui.interest-item-lore", Map.of(
                        "%total_interest%", bankManager.formatCurrency(data.getTotalInterestEarned())
                ))
        ));

        // Max deposit
        inv.setItem(lang.getInt("balance-gui.max-deposit-item-slot", 15), GuiUtil.createItem(
                lang.getString("balance-gui.max-deposit-item-material"),
                lang.get("balance-gui.max-deposit-item-name"),
                lang.getList("balance-gui.max-deposit-item-lore", Map.of(
                        "%max_deposit%", bankManager.formatMaxDeposit(level),
                        "%interest_rate%", String.format("%.1f", level.getInterestRatePercent())
                ))
        ));

        // Level
        inv.setItem(lang.getInt("balance-gui.level-item-slot", 22), GuiUtil.createItem(
                lang.getString("balance-gui.level-item-material"),
                lang.get("balance-gui.level-item-name"),
                lang.getList("balance-gui.level-item-lore", Map.of(
                        "%level%", String.valueOf(data.getLevel()),
                        "%level_name%", level.getDisplayName()
                ))
        ));

        // Close
        inv.setItem(lang.getInt("balance-gui.close-item-slot", 26), GuiUtil.createItem(
                lang.getString("balance-gui.close-item-material"),
                lang.get("balance-gui.close-item-name"),
                lang.getList("balance-gui.close-item-lore")
        ));

        viewer.openInventory(inv);
    }
}
