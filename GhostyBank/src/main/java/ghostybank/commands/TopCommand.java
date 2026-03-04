package ghostybank.commands;

import ghostybank.GhostyBank;
import ghostybank.gui.TopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TopCommand implements CommandExecutor {

    private final GhostyBank plugin;

    public TopCommand(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("ghostybank.top")) {
            player.sendMessage(plugin.getLangManager().msg("no-permission"));
            return true;
        }
        plugin.getTopGUI().open(player, TopGUI.TopType.BALANCE);
        return true;
    }
}
