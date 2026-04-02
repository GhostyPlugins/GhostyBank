package ghostybank.commands;

import ghostybank.GhostyBank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final GhostyBank plugin;

    public ReloadCommand(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ghostybank.admin")) {
            sender.sendMessage(plugin.getLangManager().msg("no-permission"));
            return true;
        }

        try {
            // 1. Save current data first
            plugin.getBankManager().saveData();

            // 2. Reload config.yml
            plugin.reloadConfig();

            // 3. Reload lang + bank levels
            plugin.getLangManager().reload();
            plugin.getBankManager().reload();

            sender.sendMessage(plugin.getLangManager().msg("reload-success"));
        } catch (Exception e) {
            sender.sendMessage("§c[GhostyBank] Reload failed: " + e.getMessage());
            plugin.getLogger().severe("Reload error: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
