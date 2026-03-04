package ghostybank.manager;

import ghostybank.GhostyBank;
import ghostybank.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        File langFile = new File(plugin.getDataFolder(), "lang/" + langName + ".yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            InputStream stream = plugin.getResource("lang/" + langName + ".yml");
            if (stream != null) {
                plugin.saveResource("lang/" + langName + ".yml", false);
            } else {
                plugin.saveResource("lang/en_US.yml", false);
                langFile = new File(plugin.getDataFolder(), "lang/en_US.yml");
                plugin.getLogger().warning("Lang file '" + langName + ".yml' not found, using en_US.");
            }
        }

        this.lang = YamlConfiguration.loadConfiguration(langFile);

        InputStream defStream = plugin.getResource("lang/" + langName + ".yml");
        if (defStream == null) defStream = plugin.getResource("lang/en_US.yml");
        if (defStream != null) {
            YamlConfiguration defLang = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            lang.setDefaults(defLang);
        }
    }

    public String get(String key, Map<String, String> placeholders) {
        String raw = lang.getString(key, "&c[Missing: " + key + "]");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return ColorUtil.color(raw);
    }

    public String get(String key) {
        return get(key, Map.of());
    }

    public String msg(String key, Map<String, String> placeholders) {
        return get("prefix") + get(key, placeholders);
    }

    public String msg(String key) {
        return msg(key, Map.of());
    }

    public List<String> getList(String key, Map<String, String> placeholders) {
        List<String> raw = lang.getStringList(key);
        return raw.stream()
                .map(line -> {
                    for (Map.Entry<String, String> e : placeholders.entrySet()) {
                        line = line.replace(e.getKey(), e.getValue());
                    }
                    return ColorUtil.color(line);
                })
                .collect(Collectors.toList());
    }

    public List<String> getList(String key) {
        return getList(key, Map.of());
    }

    public String getString(String key) {
        return lang.getString(key, "");
    }

    public int getInt(String key, int def) {
        return lang.getInt(key, def);
    }

    public FileConfiguration getRaw() {
        return lang;
    }
}
