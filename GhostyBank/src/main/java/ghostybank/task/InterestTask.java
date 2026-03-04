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
        for (BankData data : plugin.getBankManager().getAllData()) {
            if (data.getBalance() <= 0) continue;

            BankLevel level = plugin.getBankManager().getLevel(data.getLevel());
            double interest = Math.round(data.getBalance() * level.getInterestRate() * 100.0) / 100.0;
            if (interest <= 0) continue;

            data.addBalance(interest);
            data.addInterestEarned(interest);

            Player online = Bukkit.getPlayer(data.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(plugin.getLangManager().msg("interest-earned", Map.of(
                        "%amount%", plugin.getBankManager().formatCurrency(interest),
                        "%level%", String.valueOf(data.getLevel())
                )));
            }
        }
        plugin.getBankManager().saveData();
    }
}
