package ghostybank.commands;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.manager.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class PayCommand implements CommandExecutor {

    private final GhostyBank plugin;

    public PayCommand(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("ghostybank.pay")) {
            player.sendMessage(plugin.getLangManager().msg("no-permission"));
            return true;
        }

        // Direct: /pay <player> <amount>
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { player.sendMessage(plugin.getLangManager().msg("player-not-online")); return true; }
            if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(plugin.getLangManager().msg("cannot-pay-self")); return true; }

            double amount;
            try { amount = Double.parseDouble(args[1].replace(",", ".")); }
            catch (NumberFormatException e) { player.sendMessage(plugin.getLangManager().msg("invalid-amount")); return true; }

            if (amount <= 0) { player.sendMessage(plugin.getLangManager().msg("amount-too-low")); return true; }

            BankData senderData = plugin.getBankManager().getOrCreate(player.getUniqueId(), player.getName());
            BankData receiverData = plugin.getBankManager().getOrCreate(target.getUniqueId(), target.getName());

            if (senderData.getBalance() < amount) { player.sendMessage(plugin.getLangManager().msg("not-enough-money")); return true; }

            senderData.removeBalance(amount);
            BankManager.DepositResult result = plugin.getBankManager().deposit(receiverData, amount);
            if (result != BankManager.DepositResult.SUCCESS) {
                senderData.addBalance(amount);
                player.sendMessage(plugin.getLangManager().msg("deposit-limit-reached", Map.of(
                        "%max_deposit%", plugin.getBankManager().formatMaxDeposit(
                                plugin.getBankManager().getLevel(receiverData.getLevel())))));
                return true;
            }

            plugin.getBankManager().saveData();
            player.sendMessage(plugin.getLangManager().msg("pay-success", Map.of(
                    "%amount%", plugin.getBankManager().formatCurrency(amount),
                    "%receiver%", target.getName())));
            target.sendMessage(plugin.getLangManager().msg("pay-received", Map.of(
                    "%player%", player.getName(),
                    "%amount%", plugin.getBankManager().formatCurrency(amount))));
            return true;
        }

        // Open GUI
        plugin.getPayGUI().openPlayerSelect(player);
        return true;
    }
}
