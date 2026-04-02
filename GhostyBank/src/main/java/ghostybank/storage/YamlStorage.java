package ghostybank.storage;

import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * YAML-basierter Storage (Standard, keine externe Datenbank nötig).
 * Daten werden in plugins/GhostyBank/data.yml gespeichert.
 */
public class YamlStorage implements IStorage {

    private final GhostyBank plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public YamlStorage(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte data.yml nicht erstellen: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        plugin.getLogger().info("[Storage] YAML-Storage initialisiert.");
    }

    @Override
    public Collection<BankData> loadAll() {
        Collection<BankData> result = new ArrayList<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("players");
        if (section == null) return result;
        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name         = section.getString(uuidStr + ".name", "Unknown");
                double balance       = section.getDouble(uuidStr + ".balance", 0);
                double totalInterest = section.getDouble(uuidStr + ".totalInterest", 0);
                double levelInterest = section.getDouble(uuidStr + ".levelInterest", 0);
                int level            = section.getInt(uuidStr + ".level", 1);
                result.add(new BankData(uuid, name, balance, totalInterest, levelInterest, level));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[YamlStorage] Ungültige UUID übersprungen: " + uuidStr);
            }
        }
        return result;
    }

    @Override
    public void save(BankData data) {
        String path = "players." + data.getUuid();
        dataConfig.set(path + ".name",          data.getPlayerName());
        dataConfig.set(path + ".balance",        data.getBalance());
        dataConfig.set(path + ".totalInterest",  data.getTotalInterestEarned());
        dataConfig.set(path + ".levelInterest",  data.getLevelInterestEarned());
        dataConfig.set(path + ".level",          data.getLevel());
        flush();
    }

    @Override
    public void saveAll(Collection<BankData> all) {
        for (BankData data : all) {
            String path = "players." + data.getUuid();
            dataConfig.set(path + ".name",          data.getPlayerName());
            dataConfig.set(path + ".balance",        data.getBalance());
            dataConfig.set(path + ".totalInterest",  data.getTotalInterestEarned());
            dataConfig.set(path + ".levelInterest",  data.getLevelInterestEarned());
            dataConfig.set(path + ".level",          data.getLevel());
        }
        flush();
    }

    @Override
    public void remove(UUID uuid) {
        dataConfig.set("players." + uuid, null);
        flush();
    }

    @Override
    public void close() {
        // Nichts zu schließen bei YAML
    }

    private void flush() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[YamlStorage] Speicherfehler: " + e.getMessage());
        }
    }
}
