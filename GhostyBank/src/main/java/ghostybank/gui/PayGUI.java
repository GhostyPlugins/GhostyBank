package ghostybank.gui;

import ghostybank.GhostyBank;
import ghostybank.manager.LangManager;
import ghostybank.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class PayGUI {

    private final GhostyBank plugin;
    private final LangManager lang;

    public PayGUI(GhostyBank plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangManager();
    }

    public void openPlayerSelect(Player viewer) {
        openPlayerSelect(viewer, 0);
    }

    public void openPlayerSelect(Player viewer, int page) {
        int rows = plugin.getConfig().getInt("gui.pay-rows", 6);
        int playersPerPage = (rows - 1) * 9; // last row = navigation

        List<Player> others = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(viewer.getUniqueId()))
                .sorted(Comparator.comparing(Player::getName))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) others.size() / playersPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Map<Integer, UUID> slotMap = new HashMap<>();

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.PAY_PLAYER_SELECT);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, lang.get("pay-gui.player-select-title"));
        holder.setInventory(inv);

        // Filler
        String fillerMat = lang.getString("pay-gui.filler-material");
        if (!fillerMat.isBlank()) {
            ItemStack filler = GuiUtil.createItem(fillerMat, lang.getString("pay-gui.filler-name"), null);
            for (int i = 0; i < rows * 9; i++) inv.setItem(i, filler);
        }

        // Player heads
        int start = page * playersPerPage;
        for (int i = 0; i < playersPerPage && start + i < others.size(); i++) {
            Player target = others.get(start + i);
            slotMap.put(i, target.getUniqueId());
            inv.setItem(i, GuiUtil.createSkull(
                    target.getName(),
                    lang.get("pay-gui.player-head-name", Map.of("%player%", target.getName())),
                    lang.getList("pay-gui.player-head-lore", Map.of("%player%", target.getName()))
            ));
        }

        // Store page data
        holder.setData(new PaySelectData(page, totalPages, slotMap));

        // Navigation row (last row)
        int navStart = (rows - 1) * 9;

        // Prev button (slot 0 of nav row)
        if (page > 0) {
            inv.setItem(navStart, GuiUtil.createItem(
                    lang.getString("pay-gui.prev-page-material"),
                    lang.get("pay-gui.prev-page-name"),
                    lang.getList("pay-gui.prev-page-lore", Map.of(
                            "%page%", String.valueOf(page),
                            "%total%", String.valueOf(totalPages)
                    ))
            ));
        }

        // Page indicator (center)
        inv.setItem(navStart + 4, GuiUtil.createItem(
                lang.getString("pay-gui.page-info-material"),
                lang.get("pay-gui.page-info-name", Map.of(
                        "%page%", String.valueOf(page + 1),
                        "%total%", String.valueOf(totalPages)
                )),
                lang.getList("pay-gui.page-info-lore")
        ));

        // Next button (slot 8 of nav row)
        if (page < totalPages - 1) {
            inv.setItem(navStart + 8, GuiUtil.createItem(
                    lang.getString("pay-gui.next-page-material"),
                    lang.get("pay-gui.next-page-name"),
                    lang.getList("pay-gui.next-page-lore", Map.of(
                            "%page%", String.valueOf(page + 2),
                            "%total%", String.valueOf(totalPages)
                    ))
            ));
        }

        // Close button (slot 6 of nav row)
        inv.setItem(navStart + 6, GuiUtil.createItem(
                lang.getString("pay-gui.close-item-material"),
                lang.get("pay-gui.close-item-name"),
                lang.getList("pay-gui.close-item-lore")
        ));

        viewer.openInventory(inv);
    }

    public void openAmountSelect(Player viewer, UUID targetUUID, String targetName) {
        int rows = plugin.getConfig().getInt("gui.pay-rows", 6);
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.PAY_AMOUNT_SELECT, targetUUID);
        Inventory inv = Bukkit.createInventory(holder, rows * 9,
                lang.get("pay-gui.amount-select-title", Map.of("%player%", targetName)));
        holder.setInventory(inv);

        // Filler
        String fillerMat = lang.getString("pay-gui.filler-material");
        if (!fillerMat.isBlank()) {
            ItemStack filler = GuiUtil.createItem(fillerMat, lang.getString("pay-gui.filler-name"), null);
            for (int i = 0; i < rows * 9; i++) inv.setItem(i, filler);
        }

        // Preset amounts
        ConfigurationSection section = lang.getRaw().getConfigurationSection("pay-gui.amount-items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int itemSlot = section.getInt(key + ".slot", 0);
                double amount = section.getDouble(key + ".amount", 100);
                String matName = section.getString(key + ".material", "GOLD_NUGGET");
                String name = ColorUtil.color(section.getString(key + ".name", "&a" + amount));
                inv.setItem(itemSlot, GuiUtil.createItem(matName, name,
                        List.of(ColorUtil.color("&7" + plugin.getBankManager().formatCurrency(amount)))));
            }
        }

        // Custom amount
        inv.setItem(lang.getInt("pay-gui.custom-amount-item-slot", 31), GuiUtil.createItem(
                lang.getString("pay-gui.custom-amount-item-material"),
                lang.get("pay-gui.custom-amount-item-name"),
                lang.getList("pay-gui.custom-amount-item-lore")
        ));

        // Back
        inv.setItem(lang.getInt("pay-gui.back-item-slot", 27), GuiUtil.createItem(
                lang.getString("pay-gui.back-item-material"),
                lang.get("pay-gui.back-item-name"),
                lang.getList("pay-gui.back-item-lore")
        ));

        // Close
        inv.setItem(lang.getInt("pay-gui.close-item-slot", 35), GuiUtil.createItem(
                lang.getString("pay-gui.close-item-material"),
                lang.get("pay-gui.close-item-name"),
                lang.getList("pay-gui.close-item-lore")
        ));

        viewer.openInventory(inv);
    }

    // ── Page data helper ─────────────────────────
    public static class PaySelectData {
        public final int page;
        public final int totalPages;
        public final Map<Integer, UUID> slotToUUID;

        public PaySelectData(int page, int totalPages, Map<Integer, UUID> slotToUUID) {
            this.page = page;
            this.totalPages = totalPages;
            this.slotToUUID = slotToUUID;
        }
    }
}
