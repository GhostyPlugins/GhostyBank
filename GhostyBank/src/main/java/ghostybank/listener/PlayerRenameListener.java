package ghostybank.listener;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.manager.BankManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Erkennt automatisch wenn ein Spieler seinen Minecraft-Namen geändert hat
 * und aktualisiert den gespeicherten Namen in der Datenbank.
 */
public class PlayerRenameListener implements Listener {

    private final GhostyBank plugin;
    private final BankManager bankManager;

    public PlayerRenameListener(GhostyBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;

        String currentName = event.getPlayer().getName();
        java.util.UUID uuid = event.getPlayer().getUniqueId();

        BankData data = bankManager.getData(uuid);

        if (data == null) {
            // Neuer Spieler – wird beim Join angelegt
            return;
        }

        String storedName = data.getPlayerName();
        if (!storedName.equals(currentName)) {
            // Name hat sich geändert → aktualisieren und asynchron speichern
            data.setPlayerName(currentName);
            bankManager.savePlayerAsync(data);

            plugin.getLogger().info(
                "[GhostyBank] Spieler umbenannt: " + storedName + " → " + currentName +
                " (" + uuid + ")"
            );

            // Admin-Log Nachricht senden (online Admins)
            boolean notifyAdmins = plugin.getConfig().getBoolean("notify-admins-on-rename", true);
            if (notifyAdmins) {
                String lang = plugin.getLangManager().msg("rename-detected",
                    java.util.Map.of(
                        "%old_name%", storedName,
                        "%new_name%", currentName
                    )
                );
                plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("ghostybank.admin"))
                    .forEach(p -> p.sendMessage(lang));
            }
        }
    }
}
