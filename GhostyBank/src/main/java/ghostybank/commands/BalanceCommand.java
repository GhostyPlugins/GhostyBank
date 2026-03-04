package ghostybank.commands;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BalanceCommand implements CommandExecutor {

    private final GhostyBank plugin;

    public BalanceCommand(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("ghostybank.balance")) {
            player.sendMessage(plugin.getLangManager().msg("no-permission"));
            return true;
        }

        BankData data;
        if (args.length > 0) {
            String targetName = args[0];

            // Try online player first, then search saved BankData by name
            Player online = Bukkit.getPlayerExact(targetName);
            UUID targetUUID = null;
            if (online != null) {
                targetUUID = online.getUniqueId();
            } else {
                Optional<BankData> found = plugin.getBankManager().getAllData().stream()
                        .filter(d -> d.getPlayerName().equalsIgnoreCase(targetName))
                        .findFirst();
                if (found.isPresent()) targetUUID = found.get().getUuid();
            }

            if (targetUUID == null || !plugin.getBankManager().hasData(targetUUID)) {
                player.sendMessage(plugin.getLangManager().msg("player-not-found",
                        Map.of("%player%", targetName)));
                return true;
            }
            data = plugin.getBankManager().getData(targetUUID);
        } else {
            data = plugin.getBankManager().getOrCreate(player.getUniqueId(), player.getName());
        }

        plugin.getBalanceGUI().open(player, data);
        return true;
    }
}
