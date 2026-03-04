package ghostybank.listener;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.data.BankLevel;
import ghostybank.gui.AdminGUI;
import ghostybank.gui.GuiHolder;
import ghostybank.gui.PayGUI;
import ghostybank.gui.TopGUI;
import ghostybank.manager.BankManager;
import ghostybank.manager.LangManager;
import ghostybank.manager.VaultManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final GhostyBank plugin;
    private final BankManager bankManager;
    private final LangManager lang;
    private final VaultManager vault;

    // UUID -> action string: "deposit", "withdraw", "pay:<uuid>:<name>",
    //                        "admin:add:<uuid>", "admin:remove:<uuid>",
    //                        "admin:set:<uuid>", "admin:setlevel:<uuid>"
    private final Map<UUID, String> chatInputAwait = new HashMap<>();

    public GUIListener(GhostyBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.lang = plugin.getLangManager();
        this.vault = plugin.getVaultManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        bankManager.getOrCreate(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        switch (holder.getType()) {
            case BANK               -> handleBankClick(player, slot);
            case BALANCE            -> handleBalanceClick(player, slot);
            case UPGRADE            -> handleUpgradeClick(player, slot);
            case PAY_PLAYER_SELECT  -> handlePayPlayerSelect(player, holder, slot);
            case PAY_AMOUNT_SELECT  -> handlePayAmountSelect(player, holder, slot);
            case TOP                -> handleTopClick(player, slot);
            case ADMIN              -> handleAdminClick(player, holder, slot);
            case ADMIN_CONFIRM      -> handleAdminConfirmClick(player, holder, slot);
        }
    }

    // ── BANK GUI ─────────────────────────────────

    private void handleBankClick(Player player, int slot) {
        int depositSlot  = lang.getInt("bank-gui.deposit-item-slot", 29);
        int withdrawSlot = lang.getInt("bank-gui.withdraw-item-slot", 33);
        int closeSlot    = lang.getInt("bank-gui.close-item-slot", 31);
        int upgradeSlot  = lang.getInt("bank-gui.upgrade-item-slot", 15);

        if (slot == closeSlot) {
            player.closeInventory();
        } else if (slot == depositSlot) {
            player.closeInventory();
            player.sendMessage(lang.msg("deposit-amount-prompt"));
            chatInputAwait.put(player.getUniqueId(), "deposit");
        } else if (slot == withdrawSlot) {
            player.closeInventory();
            player.sendMessage(lang.msg("withdraw-amount-prompt"));
            chatInputAwait.put(player.getUniqueId(), "withdraw");
        } else if (slot == upgradeSlot) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getUpgradeGUI().open(player), 1L);
        }
    }

    // ── BALANCE GUI ───────────────────────────────

    private void handleBalanceClick(Player player, int slot) {
        if (slot == lang.getInt("balance-gui.close-item-slot", 26)) player.closeInventory();
    }

    // ── UPGRADE GUI ───────────────────────────────

    private void handleUpgradeClick(Player player, int slot) {
        int confirmSlot = lang.getInt("upgrade-gui.confirm-item-slot", 29);
        int cancelSlot  = lang.getInt("upgrade-gui.cancel-item-slot", 33);

        if (slot == cancelSlot) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getBankGUI().open(player), 1L);
        } else if (slot == confirmSlot) {
            BankData data = bankManager.getOrCreate(player.getUniqueId(), player.getName());
            BankLevel current = bankManager.getLevel(data.getLevel());
            BankLevel next = bankManager.getNextLevel(data.getLevel());

            if (current.isMaxLevel() || next == null) {
                player.sendMessage(lang.msg("upgrade-max-level"));
                player.closeInventory();
                return;
            }

            double cost = current.getUpgradeCost();
            if (data.getBalance() < cost) {
                player.sendMessage(lang.msg("upgrade-not-enough-money",
                        Map.of("%upgrade_cost%", bankManager.formatCurrency(cost))));
                player.closeInventory();
                return;
            }

            data.removeBalance(cost);
            data.setLevel(next.getLevel());
            bankManager.saveData();
            player.closeInventory();
            player.sendMessage(lang.msg("upgrade-success",
                    Map.of("%level_name%", next.getDisplayName())));
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getBankGUI().open(player), 1L);
        }
    }

    // ── PAY – PLAYER SELECT ───────────────────────

    private void handlePayPlayerSelect(Player viewer, GuiHolder holder, int slot) {
        if (!(holder.getData() instanceof PayGUI.PaySelectData pageData)) return;

        int rows = plugin.getConfig().getInt("gui.pay-rows", 6);
        int navStart = (rows - 1) * 9;

        // Close button (slot 6 of nav row)
        if (slot == navStart + 6) {
            viewer.closeInventory();
            return;
        }

        // Prev page
        if (slot == navStart && pageData.page > 0) {
            final int newPage = pageData.page - 1;
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getPayGUI().openPlayerSelect(viewer, newPage));
            return;
        }

        // Next page
        if (slot == navStart + 8 && pageData.page < pageData.totalPages - 1) {
            final int newPage = pageData.page + 1;
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getPayGUI().openPlayerSelect(viewer, newPage));
            return;
        }

        // Player head click (slots 0 to navStart-1)
        if (slot < navStart) {
            UUID targetUUID = pageData.slotToUUID.get(slot);
            if (targetUUID == null) return;
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null) { viewer.sendMessage(lang.msg("player-not-online")); return; }
            final String targetName = target.getName();
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getPayGUI().openAmountSelect(viewer, targetUUID, targetName));
        }
    }

    // ── PAY – AMOUNT SELECT ───────────────────────

    private void handlePayAmountSelect(Player viewer, GuiHolder holder, int slot) {
        int backSlot   = lang.getInt("pay-gui.back-item-slot", 27);
        int closeSlot  = lang.getInt("pay-gui.close-item-slot", 35);
        int customSlot = lang.getInt("pay-gui.custom-amount-item-slot", 31);

        UUID targetUUID = (UUID) holder.getData();
        Player target = targetUUID != null ? Bukkit.getPlayer(targetUUID) : null;

        if (slot == closeSlot) { viewer.closeInventory(); return; }

        if (slot == backSlot) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getPayGUI().openPlayerSelect(viewer));
            return;
        }

        if (slot == customSlot) {
            if (target == null) { viewer.sendMessage(lang.msg("player-not-online")); viewer.closeInventory(); return; }
            viewer.closeInventory();
            viewer.sendMessage(lang.msg("pay-amount-prompt", Map.of("%player%", target.getName())));
            chatInputAwait.put(viewer.getUniqueId(), "pay:" + targetUUID + ":" + target.getName());
            return;
        }

        // Preset amounts
        ConfigurationSection section = lang.getRaw().getConfigurationSection("pay-gui.amount-items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int aSlot = section.getInt(key + ".slot", -1);
                double amount = section.getDouble(key + ".amount", 0);
                if (aSlot == slot && amount > 0) {
                    if (target == null) { viewer.sendMessage(lang.msg("player-not-online")); viewer.closeInventory(); return; }
                    viewer.closeInventory();
                    processPay(viewer, target, amount);
                    return;
                }
            }
        }
    }

    // ── TOP GUI ───────────────────────────────────

    private void handleTopClick(Player player, int slot) {
        int closeSlot    = lang.getInt("top-gui.close-item-slot", 40);
        int balanceSlot  = lang.getInt("top-gui.tab-balance-slot", 2);
        int interestSlot = lang.getInt("top-gui.tab-interest-slot", 4);
        int levelSlot    = lang.getInt("top-gui.tab-level-slot", 6);

        if (slot == closeSlot) {
            player.closeInventory();
        } else if (slot == balanceSlot) {
            // Must use runTask to defer openInventory past the click event
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTopGUI().open(player, TopGUI.TopType.BALANCE));
        } else if (slot == interestSlot) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTopGUI().open(player, TopGUI.TopType.INTEREST));
        } else if (slot == levelSlot) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTopGUI().open(player, TopGUI.TopType.LEVEL));
        }
    }

    // ── ADMIN GUI ─────────────────────────────────

    private void handleAdminClick(Player admin, GuiHolder holder, int slot) {
        if (!(holder.getData() instanceof UUID targetUUID)) return;
        BankData target = bankManager.getData(targetUUID);
        if (target == null) { admin.closeInventory(); return; }

        int addSlot    = lang.getInt("admin-gui.add-money-slot", 11);
        int setSlot    = lang.getInt("admin-gui.set-money-slot", 13);
        int removeSlot = lang.getInt("admin-gui.remove-money-slot", 15);
        int levelSlot  = lang.getInt("admin-gui.set-level-slot", 22);
        int resetSlot  = lang.getInt("admin-gui.reset-slot", 20);
        int deleteSlot = lang.getInt("admin-gui.delete-slot", 24);
        int closeSlot  = lang.getInt("admin-gui.close-slot", 31);

        if (slot == closeSlot) {
            admin.closeInventory();
        } else if (slot == addSlot) {
            admin.closeInventory();
            admin.sendMessage(lang.msg("admin-add-prompt",
                    Map.of("%player%", target.getPlayerName())));
            chatInputAwait.put(admin.getUniqueId(), "admin:add:" + targetUUID);
        } else if (slot == setSlot) {
            admin.closeInventory();
            admin.sendMessage(lang.msg("admin-set-prompt",
                    Map.of("%player%", target.getPlayerName())));
            chatInputAwait.put(admin.getUniqueId(), "admin:set:" + targetUUID);
        } else if (slot == removeSlot) {
            admin.closeInventory();
            admin.sendMessage(lang.msg("admin-remove-prompt",
                    Map.of("%player%", target.getPlayerName())));
            chatInputAwait.put(admin.getUniqueId(), "admin:remove:" + targetUUID);
        } else if (slot == levelSlot) {
            admin.closeInventory();
            admin.sendMessage(lang.msg("admin-setlevel-prompt", Map.of(
                    "%player%", target.getPlayerName(),
                    "%max_level%", String.valueOf(bankManager.getMaxLevel())
            )));
            chatInputAwait.put(admin.getUniqueId(), "admin:setlevel:" + targetUUID);
        } else if (slot == resetSlot) {
            // Öffne Bestätigungs-GUI
            Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getAdminGUI().openConfirm(admin, targetUUID, target.getPlayerName(), "reset"));
        } else if (slot == deleteSlot) {
            // Öffne Bestätigungs-GUI
            Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getAdminGUI().openConfirm(admin, targetUUID, target.getPlayerName(), "delete"));
        }
    }

    // ── ADMIN CONFIRM GUI ─────────────────────────

    private void handleAdminConfirmClick(Player admin, GuiHolder holder, int slot) {
        if (!(holder.getData() instanceof AdminGUI.AdminConfirmData confirmData)) return;

        if (slot == 15) {
            // NO → Zurück zur Admin-GUI
            BankData target = bankManager.getData(confirmData.targetUUID);
            if (target != null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getAdminGUI().open(admin, target));
            } else {
                admin.closeInventory();
            }
            return;
        }

        if (slot != 11) return; // Nur Slot 11 (YES) ausführen

        switch (confirmData.action) {
            case "reset" -> {
                BankData target = bankManager.getData(confirmData.targetUUID);
                if (target == null) { admin.closeInventory(); return; }
                target.resetToDefault();
                bankManager.saveData();
                admin.closeInventory();
                admin.sendMessage(lang.msg("admin-reset-success",
                        Map.of("%player%", confirmData.targetName)));
                // Online-Spieler benachrichtigen
                Player targetOnline = Bukkit.getPlayer(confirmData.targetUUID);
                if (targetOnline != null) {
                    targetOnline.sendMessage(lang.msg("admin-reset-notify"));
                }
            }
            case "delete" -> {
                bankManager.removePlayer(confirmData.targetUUID);
                admin.closeInventory();
                admin.sendMessage(lang.msg("admin-delete-success",
                        Map.of("%player%", confirmData.targetName)));
                // Online-Spieler benachrichtigen
                Player targetOnline = Bukkit.getPlayer(confirmData.targetUUID);
                if (targetOnline != null) {
                    targetOnline.sendMessage(lang.msg("admin-delete-notify"));
                }
            }
        }
    }

    // ── CHAT INPUT ────────────────────────────────

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String action = chatInputAwait.get(player.getUniqueId());
        if (action == null) return;

        event.setCancelled(true);
        chatInputAwait.remove(player.getUniqueId());

        String input = event.getMessage().trim();
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(lang.msg("action-cancelled"));
            return;
        }

        // ── Admin actions (non-numeric for setlevel) ─
        if (action.startsWith("admin:")) {
            String[] parts = action.split(":", 3);
            String adminAction = parts[1];
            UUID targetUUID = UUID.fromString(parts[2]);
            BankData target = bankManager.getData(targetUUID);

            if (target == null) { player.sendMessage(lang.msg("admin-player-not-found")); return; }

            if (adminAction.equals("setlevel")) {
                int level;
                try { level = Integer.parseInt(input); }
                catch (NumberFormatException e) {
                    player.sendMessage(lang.msg("admin-setlevel-invalid",
                            Map.of("%max_level%", String.valueOf(bankManager.getMaxLevel()))));
                    return;
                }
                int maxLevel = bankManager.getMaxLevel();
                if (level < 1 || level > maxLevel) {
                    player.sendMessage(lang.msg("admin-setlevel-invalid",
                            Map.of("%max_level%", String.valueOf(maxLevel))));
                    return;
                }
                final int finalLevel = level;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    target.setLevel(finalLevel);
                    bankManager.saveData();
                    player.sendMessage(lang.msg("admin-setlevel-success", Map.of(
                            "%player%", target.getPlayerName(),
                            "%level_name%", bankManager.getLevel(finalLevel).getDisplayName()
                    )));
                });
                return;
            }

            double amount;
            try { amount = Double.parseDouble(input.replace(",", ".")); }
            catch (NumberFormatException e) { player.sendMessage(lang.msg("invalid-amount")); return; }
            if (amount < 0) { player.sendMessage(lang.msg("amount-too-low")); return; }

            final double finalAmount = amount;
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (adminAction) {
                    case "add" -> {
                        target.addBalance(finalAmount);
                        bankManager.saveData();
                        player.sendMessage(lang.msg("admin-add-success", Map.of(
                                "%player%", target.getPlayerName(),
                                "%amount%", bankManager.formatCurrency(finalAmount)
                        )));
                    }
                    case "remove" -> {
                        if (target.getBalance() < finalAmount) target.setBalance(0);
                        else target.removeBalance(finalAmount);
                        bankManager.saveData();
                        player.sendMessage(lang.msg("admin-remove-success", Map.of(
                                "%player%", target.getPlayerName(),
                                "%amount%", bankManager.formatCurrency(finalAmount)
                        )));
                    }
                    case "set" -> {
                        target.setBalance(finalAmount);
                        bankManager.saveData();
                        player.sendMessage(lang.msg("admin-set-success", Map.of(
                                "%player%", target.getPlayerName(),
                                "%amount%", bankManager.formatCurrency(finalAmount)
                        )));
                    }
                }
            });
            return;
        }

        // ── Deposit / Withdraw / Pay ─────────────────
        double amount;
        try {
            amount = Double.parseDouble(input.replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.msg("invalid-amount"));
            return;
        }

        if (amount <= 0) { player.sendMessage(lang.msg("amount-too-low")); return; }

        final double finalAmount = amount;

        if (action.equals("deposit")) {
            Bukkit.getScheduler().runTask(plugin, () -> processDeposit(player, finalAmount));
        } else if (action.equals("withdraw")) {
            Bukkit.getScheduler().runTask(plugin, () -> processWithdraw(player, finalAmount));
        } else if (action.startsWith("pay:")) {
            String[] parts = action.split(":", 3);
            UUID targetUUID = UUID.fromString(parts[1]);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player target = Bukkit.getPlayer(targetUUID);
                if (target == null) { player.sendMessage(lang.msg("player-not-online")); return; }
                processPay(player, target, finalAmount);
            });
        }
    }

    // ── OPERATIONS ────────────────────────────────

    private void processDeposit(Player player, double amount) {
        BankData data = bankManager.getOrCreate(player.getUniqueId(), player.getName());
        BankLevel level = bankManager.getLevel(data.getLevel());

        if (!vault.has(player, amount)) { player.sendMessage(lang.msg("not-enough-wallet")); return; }

        BankManager.DepositResult result = bankManager.deposit(data, amount);
        switch (result) {
            case SUCCESS -> {
                vault.withdraw(player, amount);
                bankManager.saveData();
                player.sendMessage(lang.msg("deposit-success",
                        Map.of("%amount%", bankManager.formatCurrency(amount))));
            }
            case LIMIT_REACHED -> player.sendMessage(lang.msg("deposit-limit-reached",
                    Map.of("%max_deposit%", bankManager.formatMaxDeposit(level))));
            case INVALID_AMOUNT -> player.sendMessage(lang.msg("invalid-amount"));
        }
    }

    private void processWithdraw(Player player, double amount) {
        BankData data = bankManager.getOrCreate(player.getUniqueId(), player.getName());
        double fee = bankManager.getWithdrawalFee(amount);
        double playerReceives = amount - fee;

        if (data.getBalance() < amount) { player.sendMessage(lang.msg("not-enough-money")); return; }

        if (bankManager.withdraw(data, amount) == BankManager.WithdrawResult.SUCCESS) {
            vault.deposit(player, playerReceives);
            bankManager.saveData();
            player.sendMessage(lang.msg("withdraw-success", Map.of(
                    "%amount%", bankManager.formatCurrency(playerReceives),
                    "%fee%", bankManager.formatCurrency(fee)
            )));
        } else {
            player.sendMessage(lang.msg("not-enough-money"));
        }
    }

    private void processPay(Player sender, Player receiver, double amount) {
        if (sender.getUniqueId().equals(receiver.getUniqueId())) {
            sender.sendMessage(lang.msg("cannot-pay-self"));
            return;
        }

        BankData senderData = bankManager.getOrCreate(sender.getUniqueId(), sender.getName());
        BankData receiverData = bankManager.getOrCreate(receiver.getUniqueId(), receiver.getName());

        if (senderData.getBalance() < amount) { sender.sendMessage(lang.msg("not-enough-money")); return; }

        senderData.removeBalance(amount);
        BankManager.DepositResult result = bankManager.deposit(receiverData, amount);
        if (result != BankManager.DepositResult.SUCCESS) {
            senderData.addBalance(amount);
            sender.sendMessage(lang.msg("deposit-limit-reached", Map.of(
                    "%max_deposit%", bankManager.formatMaxDeposit(
                            bankManager.getLevel(receiverData.getLevel())))));
            return;
        }

        bankManager.saveData();
        sender.sendMessage(lang.msg("pay-success", Map.of(
                "%amount%", bankManager.formatCurrency(amount),
                "%receiver%", receiver.getName())));
        receiver.sendMessage(lang.msg("pay-received", Map.of(
                "%player%", sender.getName(),
                "%amount%", bankManager.formatCurrency(amount))));
    }

    public void clearChatInput(UUID uuid) {
        chatInputAwait.remove(uuid);
    }
}
