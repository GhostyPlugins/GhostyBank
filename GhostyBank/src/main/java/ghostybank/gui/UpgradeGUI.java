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

public class UpgradeGUI {

    private final BankManager bankManager;
    private final LangManager lang;

    public UpgradeGUI(GhostyBank plugin) {
        this.bankManager = plugin.getBankManager();
        this.lang = plugin.getLangManager();
    }

    public void open(Player player) {
        BankData data = bankManager.getOrCreate(player.getUniqueId(), player.getName());
        BankLevel current = bankManager.getLevel(data.getLevel());
        BankLevel next = bankManager.getNextLevel(data.getLevel());

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.UPGRADE);
        Inventory inv = Bukkit.createInventory(holder, 36, lang.get("upgrade-gui.title"));
        holder.setInventory(inv);

        // Filler
        String fillerMat = lang.getString("upgrade-gui.filler-material");
        if (!fillerMat.isBlank()) {
            ItemStack filler = GuiUtil.createItem(fillerMat, lang.getString("upgrade-gui.filler-name"), null);
            for (int i = 0; i < 36; i++) inv.setItem(i, filler);
        }

        // Current level
        inv.setItem(lang.getInt("upgrade-gui.current-level-item-slot", 11), GuiUtil.createItem(
                lang.getString("upgrade-gui.current-level-item-material"),
                lang.get("upgrade-gui.current-level-item-name"),
                lang.getList("upgrade-gui.current-level-item-lore", Map.of(
                        "%level%", String.valueOf(current.getLevel()),
                        "%level_name%", current.getDisplayName(),
                        "%max_deposit%", bankManager.formatMaxDeposit(current),
                        "%interest_rate%", String.format("%.1f", current.getInterestRatePercent())
                ))
        ));

        if (next != null) {
            // Next level preview
            inv.setItem(lang.getInt("upgrade-gui.next-level-item-slot", 15), GuiUtil.createItem(
                    lang.getString("upgrade-gui.next-level-item-material"),
                    lang.get("upgrade-gui.next-level-item-name"),
                    lang.getList("upgrade-gui.next-level-item-lore", Map.of(
                            "%next_level%", String.valueOf(next.getLevel()),
                            "%next_level_name%", next.getDisplayName(),
                            "%next_max_deposit%", bankManager.formatMaxDeposit(next),
                            "%next_interest_rate%", String.format("%.1f", next.getInterestRatePercent()),
                            "%upgrade_cost%", bankManager.formatCurrency(current.getUpgradeCost())
                    ))
            ));

            // Confirm
            inv.setItem(lang.getInt("upgrade-gui.confirm-item-slot", 29), GuiUtil.createItem(
                    lang.getString("upgrade-gui.confirm-item-material"),
                    lang.get("upgrade-gui.confirm-item-name"),
                    lang.getList("upgrade-gui.confirm-item-lore", Map.of(
                            "%upgrade_cost%", bankManager.formatCurrency(current.getUpgradeCost())
                    ))
            ));
        }

        // Cancel
        inv.setItem(lang.getInt("upgrade-gui.cancel-item-slot", 33), GuiUtil.createItem(
                lang.getString("upgrade-gui.cancel-item-material"),
                lang.get("upgrade-gui.cancel-item-name"),
                lang.getList("upgrade-gui.cancel-item-lore")
        ));

        player.openInventory(inv);
    }
}
