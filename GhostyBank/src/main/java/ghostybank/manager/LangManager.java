package ghostybank.manager;

import ghostybank.GhostyBank;
import ghostybank.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LangManager {

    private final GhostyBank plugin;
    private FileConfiguration lang;

    public LangManager(GhostyBank plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String langName = plugin.getConfig().getString("language", "de_DE");
        File langFile   = new File(plugin.getDataFolder(), "lang/" + langName + ".yml");

        // Sicherstellen dass das lang-Verzeichnis existiert
        langFile.getParentFile().mkdirs();

        // Datei aus JAR extrahieren wenn sie noch nicht existiert
        if (!langFile.exists()) {
            boolean saved = extractResource("lang/" + langName + ".yml", langFile);
            if (!saved) {
                // Fallback auf en_US
                plugin.getLogger().warning("[LangManager] '" + langName + ".yml' nicht in der JAR gefunden, nutze en_US.");
                langFile = new File(plugin.getDataFolder(), "lang/en_US.yml");
                extractResource("lang/en_US.yml", langFile);
            }
        }

        // Aus Datei laden (falls Datei immer noch fehlt → leere Config, kein Crash)
        if (langFile.exists()) {
            this.lang = YamlConfiguration.loadConfiguration(langFile);
        } else {
            plugin.getLogger().severe("[LangManager] Keine Lang-Datei gefunden! Plugin nutzt leere Konfiguration.");
            this.lang = new YamlConfiguration();
        }

        // Defaults aus eingebetteter Ressource setzen (für neue Keys nach Updates)
        InputStream defStream = plugin.getResource("lang/" + langName + ".yml");
        if (defStream == null) defStream = plugin.getResource("lang/en_US.yml");
        if (defStream != null) {
            try {
                YamlConfiguration defLang = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                lang.setDefaults(defLang);
            } catch (Exception e) {
                plugin.getLogger().warning("[LangManager] Konnte Default-Lang nicht laden: " + e.getMessage());
            }
        }
    }

    /**
     * Extrahiert eine Ressource aus der JAR in das Plugin-Verzeichnis.
     * Sicherer als saveResource() — wirft keine Exception wenn die Datei fehlt.
     * @return true wenn erfolgreich
     */
    private boolean extractResource(String resourcePath, File destination) {
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) return false;
        try {
            Files.copy(stream, destination.toPath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[LangManager] Konnte " + resourcePath + " nicht extrahieren: " + e.getMessage());
            return false;
        }
    }

    // ── Getters ──────────────────────────────────

    public String get(String key, Map<String, String> placeholders) {
        String raw = lang.getString(key, "&c[Missing: " + key + "]");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return ColorUtil.color(raw);
    }

    public String get(String key) { return get(key, Map.of()); }

    public String msg(String key, Map<String, String> placeholders) {
        return get("prefix") + get(key, placeholders);
    }

    public String msg(String key) { return msg(key, Map.of()); }

    public List<String> getList(String key, Map<String, String> placeholders) {
        return lang.getStringList(key).stream()
                .map(line -> {
                    for (Map.Entry<String, String> e : placeholders.entrySet()) {
                        line = line.replace(e.getKey(), e.getValue());
                    }
                    return ColorUtil.color(line);
                })
                .collect(Collectors.toList());
    }

    public List<String> getList(String key)          { return getList(key, Map.of()); }
    public String       getString(String key)         { return lang.getString(key, ""); }
    public int          getInt(String key, int def)   { return lang.getInt(key, def); }
    public FileConfiguration getRaw()                 { return lang; }
}
