package ghostybank.commands;

import ghostybank.GhostyBank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BankCommand implements CommandExecutor {

    private final GhostyBank plugin;

    public BankCommand(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("ghostybank.bank")) {
            player.sendMessage(plugin.getLangManager().msg("no-permission"));
            return true;
        }
        plugin.getBankGUI().open(player);
        return true;
    }
}
