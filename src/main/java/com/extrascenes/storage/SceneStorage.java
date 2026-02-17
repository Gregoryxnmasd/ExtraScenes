package com.extrascenes.storage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import com.extrascenes.core.Scene;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class SceneStorage {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SceneStorage(Path dataFolder) {
        this.file = dataFolder.resolve("scenes.json");
    }

    public Map<String, Scene> load() {
        if (!Files.exists(file)) return new HashMap<>();
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, Scene> loaded = gson.fromJson(reader, new TypeToken<Map<String, Scene>>(){}.getType());
            return loaded == null ? new HashMap<>() : loaded;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load scenes", e);
        }
    }

    public void save(Map<String, Scene> scenes) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("scenes.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                gson.toJson(scenes, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save scenes", e);
        }
    }

    public void deleteAll() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete scenes", e);
        }
    }
}
