package ghostybank.commands;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.data.BankLevel;
import ghostybank.manager.BankManager;
import ghostybank.manager.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class AdminCommand implements CommandExecutor {

    private final GhostyBank plugin;
    private final BankManager bankManager;
    private final LangManager lang;

    public AdminCommand(GhostyBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.lang = plugin.getLangManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ghostybank.admin")) {
            sender.sendMessage(lang.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(lang.msg("admin-usage"));
            return true;
        }

        // Find target player by name in saved data
        String targetName = args[0];
        BankData target = findPlayer(targetName);

        if (target == null) {
            sender.sendMessage(lang.msg("admin-player-not-found"));
            return true;
        }

        // /gadmin <player> → open GUI
        if (args.length == 1) {
            if (!(sender instanceof Player admin)) {
                sender.sendMessage("§cOnly players can open the admin GUI. Use subcommands instead.");
                return true;
            }
            plugin.getAdminGUI().open(admin, target);
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "info" -> {
                BankLevel level = bankManager.getLevel(target.getLevel());
                sender.sendMessage(lang.msg("admin-info", Map.of(
                        "%player%", target.getPlayerName(),
                        "%balance%", bankManager.formatCurrency(target.getBalance()),
                        "%level%", String.valueOf(target.getLevel()),
                        "%level_name%", level.getDisplayName(),
                        "%total_interest%", bankManager.formatCurrency(target.getTotalInterestEarned())
                )));
            }
            case "addmoney" -> {
                if (args.length < 3) { sender.sendMessage(lang.msg("admin-usage")); return true; }
                double amount = parseAmount(sender, args[2]);
                if (amount < 0) return true;
                target.addBalance(amount);
                bankManager.saveData();
                sender.sendMessage(lang.msg("admin-add-success", Map.of(
                        "%player%", target.getPlayerName(),
                        "%amount%", bankManager.formatCurrency(amount)
                )));
            }
            case "removemoney" -> {
                if (args.length < 3) { sender.sendMessage(lang.msg("admin-usage")); return true; }
                double amount = parseAmount(sender, args[2]);
                if (amount < 0) return true;
                if (target.getBalance() < amount) {
                    target.setBalance(0);
                } else {
                    target.removeBalance(amount);
                }
                bankManager.saveData();
                sender.sendMessage(lang.msg("admin-remove-success", Map.of(
                        "%player%", target.getPlayerName(),
                        "%amount%", bankManager.formatCurrency(amount)
                )));
            }
            case "setmoney" -> {
                if (args.length < 3) { sender.sendMessage(lang.msg("admin-usage")); return true; }
                double amount = parseAmount(sender, args[2]);
                if (amount < 0) return true;
                target.setBalance(amount);
                bankManager.saveData();
                sender.sendMessage(lang.msg("admin-set-success", Map.of(
                        "%player%", target.getPlayerName(),
                        "%amount%", bankManager.formatCurrency(amount)
                )));
            }
            case "setlevel" -> {
                if (args.length < 3) { sender.sendMessage(lang.msg("admin-usage")); return true; }
                int maxLevel = bankManager.getMaxLevel();
                int level;
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(lang.msg("admin-setlevel-invalid",
                            Map.of("%max_level%", String.valueOf(maxLevel))));
                    return true;
                }
                if (level < 1 || level > maxLevel) {
                    sender.sendMessage(lang.msg("admin-setlevel-invalid",
                            Map.of("%max_level%", String.valueOf(maxLevel))));
                    return true;
                }
                target.setLevel(level);
                bankManager.saveData();
                BankLevel newLevel = bankManager.getLevel(level);
                sender.sendMessage(lang.msg("admin-setlevel-success", Map.of(
                        "%player%", target.getPlayerName(),
                        "%level_name%", newLevel.getDisplayName()
                )));
            }
            case "reset" -> {
                target.resetToDefault();
                bankManager.saveData();
                sender.sendMessage(lang.msg("admin-reset-success", Map.of(
                        "%player%", target.getPlayerName()
                )));
                // Online-Spieler informieren
                org.bukkit.entity.Player targetOnline = org.bukkit.Bukkit.getPlayer(target.getUuid());
                if (targetOnline != null) targetOnline.sendMessage(lang.msg("admin-reset-notify"));
            }
            case "delete" -> {
                String name = target.getPlayerName();
                org.bukkit.entity.Player targetOnline = org.bukkit.Bukkit.getPlayer(target.getUuid());
                bankManager.removePlayer(target.getUuid());
                sender.sendMessage(lang.msg("admin-delete-success", Map.of("%player%", name)));
                if (targetOnline != null) targetOnline.sendMessage(lang.msg("admin-delete-notify"));
            }
            default -> sender.sendMessage(lang.msg("admin-usage"));
        }

        return true;
    }

    private double parseAmount(CommandSender sender, String input) {
        try {
            double val = Double.parseDouble(input.replace(",", "."));
            if (val < 0) {
                sender.sendMessage(lang.msg("amount-too-low"));
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            sender.sendMessage(lang.msg("invalid-amount"));
            return -1;
        }
    }

    public BankData findPlayer(String name) {
        // Check online players first
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && bankManager.hasData(online.getUniqueId())) {
            return bankManager.getData(online.getUniqueId());
        }
        // Search saved data by name
        Optional<BankData> found = bankManager.getAllData().stream()
                .filter(d -> d.getPlayerName().equalsIgnoreCase(name))
                .findFirst();
        return found.orElse(null);
    }
}
