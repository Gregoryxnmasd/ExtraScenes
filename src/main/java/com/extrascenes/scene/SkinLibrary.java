package com.extrascenes.scene;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class SkinLibrary {
    private final File file;
    private final Map<String, SkinEntry> entries = new LinkedHashMap<>();

    public SkinLibrary(File dataFolder) {
        this.file = new File(dataFolder, "skins.yml");
        reload();
    }

    public void reload() {
        entries.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("skins")) {
            return;
        }
        for (String key : yaml.getConfigurationSection("skins").getKeys(false)) {
            String path = "skins." + key;
            entries.put(key.toLowerCase(), new SkinEntry(
                    yaml.getString(path + ".name", key),
                    yaml.getString(path + ".cacheKey", key),
                    yaml.getString(path + ".signature", ""),
                    yaml.getString(path + ".texture", "")
            ));
        }
    }

    public SkinEntry get(String key) {
        return key == null ? null : entries.get(key.toLowerCase());
    }

    public SkinEntry firstEntry() {
        return entries.values().stream().findFirst().orElse(null);
    }

    public record SkinEntry(String name, String cacheKey, String signature, String texture) {
    }
}
