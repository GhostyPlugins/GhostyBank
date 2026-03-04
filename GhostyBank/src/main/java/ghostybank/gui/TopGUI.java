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

public class TopGUI {

    public enum TopType { BALANCE, INTEREST, LEVEL }

    private final GhostyBank plugin;
    private final BankManager bankManager;
    private final LangManager lang;

    public TopGUI(GhostyBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.lang = plugin.getLangManager();
    }

    public void open(Player player, TopType type) {
        int rows = plugin.getConfig().getInt("gui.top-rows", 5);
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.TOP, type);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, lang.get("top-gui.title"));
        holder.setInventory(inv);

        // Filler
        String fillerMat = lang.getString("top-gui.filler-material");
        if (!fillerMat.isBlank()) {
            ItemStack filler = GuiUtil.createItem(fillerMat, lang.getString("top-gui.filler-name"), null);
            for (int i = 0; i < rows * 9; i++) inv.setItem(i, filler);
        }

        // Tab buttons
        inv.setItem(lang.getInt("top-gui.tab-balance-slot", 2), GuiUtil.createItem(
                lang.getString("top-gui.tab-balance-material"),
                lang.get("top-gui.tab-balance-name"),
                lang.getList("top-gui.tab-balance-lore")
        ));
        inv.setItem(lang.getInt("top-gui.tab-interest-slot", 4), GuiUtil.createItem(
                lang.getString("top-gui.tab-interest-material"),
                lang.get("top-gui.tab-interest-name"),
                lang.getList("top-gui.tab-interest-lore")
        ));
        inv.setItem(lang.getInt("top-gui.tab-level-slot", 6), GuiUtil.createItem(
                lang.getString("top-gui.tab-level-material"),
                lang.get("top-gui.tab-level-name"),
                lang.getList("top-gui.tab-level-lore")
        ));

        // Entries starting at slot 9
        List<BankData> entries = switch (type) {
            case BALANCE  -> bankManager.getTopByBalance(rows * 9 - 18);
            case INTEREST -> bankManager.getTopByInterest(rows * 9 - 18);
            case LEVEL    -> bankManager.getTopByLevel(rows * 9 - 18);
        };

        int slot = 9;
        int rank = 1;
        for (BankData entry : entries) {
            if (slot >= rows * 9 - 9) break;
            BankLevel lvl = bankManager.getLevel(entry.getLevel());
            String entryName = lang.get("top-gui.entry-name", Map.of(
                    "%rank%", String.valueOf(rank),
                    "%player%", entry.getPlayerName()
            ));
            List<String> entryLore = switch (type) {
                case BALANCE  -> lang.getList("top-gui.entry-balance-lore", Map.of(
                        "%balance%", bankManager.formatCurrency(entry.getBalance()),
                        "%level_name%", lvl.getDisplayName()));
                case INTEREST -> lang.getList("top-gui.entry-interest-lore", Map.of(
                        "%total_interest%", bankManager.formatCurrency(entry.getTotalInterestEarned()),
                        "%level_name%", lvl.getDisplayName()));
                case LEVEL    -> lang.getList("top-gui.entry-level-lore", Map.of(
                        "%level%", String.valueOf(entry.getLevel()),
                        "%level_name%", lvl.getDisplayName(),
                        "%balance%", bankManager.formatCurrency(entry.getBalance())));
            };
            inv.setItem(slot, GuiUtil.createSkull(entry.getPlayerName(), entryName, entryLore));
            slot++;
            rank++;
        }

        // Close
        inv.setItem(lang.getInt("top-gui.close-item-slot", 40), GuiUtil.createItem(
                lang.getString("top-gui.close-item-material"),
                lang.get("top-gui.close-item-name"),
                lang.getList("top-gui.close-item-lore")
        ));

        player.openInventory(inv);
    }
}
