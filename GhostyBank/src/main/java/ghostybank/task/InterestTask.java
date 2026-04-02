package ghostybank.task;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import ghostybank.data.BankLevel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class InterestTask extends BukkitRunnable {

    private final GhostyBank plugin;

    public InterestTask(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Globales Zins-System: wenn deaktiviert gar nichts tun
        if (!plugin.getConfig().getBoolean("interest.enabled", true)) return;

        boolean capEnabled = plugin.getConfig().getBoolean("interest.max-total-interest-cap.enabled", false);

        for (BankData data : plugin.getBankManager().getAllData()) {
            // Zinsen nur für ONLINE Spieler
            Player online = Bukkit.getPlayer(data.getUuid());
            if (online == null || !online.isOnline()) continue;

            if (data.getBalance() <= 0) continue;

            BankLevel level = plugin.getBankManager().getLevel(data.getLevel());

            // Prüfen ob das Zins-Cap auf diesem Level aktiv ist und erreicht wurde
            if (capEnabled && level.hasInterestCap()) {
                if (data.getLevelInterestEarned() >= level.getMaxTotalInterest()) {
                    // Cap erreicht → keine Zinsen mehr, nur einmal informieren
                    if (!data.isInterestCapNotified()) {
                        online.sendMessage(plugin.getLangManager().msg("interest-cap-reached", Map.of(
                                "%level_name%", level.getDisplayName(),
                                "%max%", plugin.getBankManager().formatCurrency(level.getMaxTotalInterest())
                        )));
                        data.setInterestCapNotified(true);
                    }
                    continue;
                }
            }

            double interest = Math.round(data.getBalance() * level.getInterestRate() * 100.0) / 100.0;
            if (interest <= 0) continue;

            // Cap: wenn der berechnete Zins das Restlimit übersteigt, auf Rest kürzen
            if (capEnabled && level.hasInterestCap()) {
                double remaining = level.getMaxTotalInterest() - data.getLevelInterestEarned();
                if (interest > remaining) {
                    interest = Math.round(remaining * 100.0) / 100.0;
                }
            }

            if (interest <= 0) continue;

            data.addBalance(interest);
            data.addInterestEarned(interest);
            data.setInterestCapNotified(false); // Reset damit Meldung beim nächsten Cap wieder kommt

            online.sendMessage(plugin.getLangManager().msg("interest-earned", Map.of(
                    "%amount%", plugin.getBankManager().formatCurrency(interest),
                    "%level%", String.valueOf(data.getLevel())
            )));
        }
        plugin.getBankManager().saveData();
    }
}
