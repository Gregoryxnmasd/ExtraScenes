package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class SceneManager {
    private static final int FORMAT_VERSION = 1;
    private final ExtraScenesPlugin plugin;
    private final File scenesFolder;
    private final File exportsFolder;
    private final SceneSerializer serializer = new SceneSerializer();
    private final SceneDeserializer deserializer = new SceneDeserializer();
    private final Map<String, Scene> cache = new HashMap<>();
    private final Map<String, BukkitTask> pendingSaves = new HashMap<>();

    public SceneManager(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        this.scenesFolder = new File(plugin.getDataFolder(), "scenes");
        this.exportsFolder = new File(plugin.getDataFolder(), "exports");
        if (!scenesFolder.exists()) {
            scenesFolder.mkdirs();
        }
        if (!exportsFolder.exists()) {
            exportsFolder.mkdirs();
        }
    }

    public Scene createScene(String name, int durationTicks) {
        Map<SceneTrackType, Track<? extends Keyframe>> tracks = new EnumMap<>(SceneTrackType.class);
        tracks.put(SceneTrackType.CAMERA, new Track<>(SceneTrackType.CAMERA));
        tracks.put(SceneTrackType.COMMAND, new Track<>(SceneTrackType.COMMAND));
        tracks.put(SceneTrackType.MODEL, new Track<>(SceneTrackType.MODEL));
        tracks.put(SceneTrackType.ACTIONBAR, new Track<>(SceneTrackType.ACTIONBAR));
        tracks.put(SceneTrackType.PARTICLE, new Track<>(SceneTrackType.PARTICLE));
        tracks.put(SceneTrackType.SOUND, new Track<>(SceneTrackType.SOUND));
        tracks.put(SceneTrackType.BLOCK_ILLUSION, new Track<>(SceneTrackType.BLOCK_ILLUSION));
        Scene scene = new Scene(java.util.UUID.randomUUID().toString(), name, durationTicks, FORMAT_VERSION, tracks);
        scene.setDefaultSmoothing(readSmoothing());
        scene.setSmoothingQuality(readSmoothingQuality());
        scene.setFreezePlayer(plugin.getConfig().getBoolean("player.freeze", true));
        scene.setAllowGlobalCommands(plugin.getConfig().getBoolean("commands.allowGlobalDefault", false));
        cache.put(name.toLowerCase(), scene);
        return scene;
    }

    private SmoothingMode readSmoothing() {
        String smoothing = plugin.getConfig().getString("smoothing.default", "SMOOTH");
        try {
            return SmoothingMode.valueOf(smoothing);
        } catch (IllegalArgumentException ex) {
            return SmoothingMode.SMOOTH;
        }
    }

    private SmoothingQuality readSmoothingQuality() {
        String quality = plugin.getConfig().getString("smoothing.quality", "SMOOTH");
        try {
            return SmoothingQuality.valueOf(quality);
        } catch (IllegalArgumentException ex) {
            return SmoothingQuality.SMOOTH;
        }
    }

    public void saveScene(Scene scene) throws IOException {
        Path file = new File(scenesFolder, scene.getName() + ".json").toPath();
        serializer.write(scene, file);
        scene.setDirty(false);
    }

    public Scene loadScene(String name) {
        if (name == null) {
            return null;
        }
        String key = name.toLowerCase();
        if (cache.containsKey(key)) {
            File cachedFile = new File(scenesFolder, cache.get(key).getName() + ".json");
            if (!cachedFile.exists()) {
                cache.remove(key);
                return null;
            }
            return cache.get(key);
        }
        File file = new File(scenesFolder, name + ".json");
        if (!file.exists()) {
            return null;
        }
        try {
            Scene scene = deserializer.read(file.toPath());
            if (scene == null) {
                return null;
            }
            ensureTracks(scene);
            cache.put(key, scene);
            return scene;
        } catch (IOException ex) {
            return null;
        }
    }

    public void cacheScene(Scene scene) {
        if (scene != null) {
            cache.put(scene.getName().toLowerCase(), scene);
        }
    }

    public void markDirty(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.setDirty(true);
        scheduleSave(scene, 20L);
    }

    public void saveSceneImmediate(Scene scene) {
        if (scene == null) {
            return;
        }
        BukkitTask pending = pendingSaves.remove(scene.getName().toLowerCase());
        if (pending != null) {
            pending.cancel();
        }
        try {
            saveScene(scene);
        } catch (IOException ex) {
            // ignore save failure
        }
    }

    public void saveAllDirty() {
        for (Scene scene : cache.values()) {
            if (scene.isDirty()) {
                saveSceneImmediate(scene);
            }
        }
    }

    public void reloadAll() {
        saveAllDirty();
        cache.clear();
        pendingSaves.values().forEach(BukkitTask::cancel);
        pendingSaves.clear();
    }

    public boolean deleteScene(String name) {
        if (name == null) {
            return false;
        }
        String key = name.toLowerCase();
        BukkitTask pending = pendingSaves.remove(key);
        if (pending != null) {
            pending.cancel();
        }
        cache.remove(key);
        File file = new File(scenesFolder, name + ".json");
        return !file.exists() || file.delete();
    }

    public boolean exportScene(String name) throws IOException {
        File source = new File(scenesFolder, name + ".json");
        if (!source.exists()) {
            return false;
        }
        File destination = new File(exportsFolder, name + ".json");
        java.nio.file.Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    public boolean importScene(String name) throws IOException {
        File source = new File(exportsFolder, name + ".json");
        if (!source.exists()) {
            return false;
        }
        File destination = new File(scenesFolder, name + ".json");
        java.nio.file.Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return true;
    }


    public long getSceneLastModified(String name) {
        if (name == null) {
            return 0L;
        }
        File file = new File(scenesFolder, name + ".json");
        return file.exists() ? file.lastModified() : 0L;
    }

    public boolean sceneExists(String name) {
        if (name == null) {
            return false;
        }
        return new File(scenesFolder, name + ".json").exists();
    }
    public List<String> listScenes() {
        File[] files = scenesFolder.listFiles((dir, fileName) -> fileName.endsWith(".json"));
        if (files == null) {
            return List.of();
        }
        return java.util.Arrays.stream(files)
                .map(file -> file.getName().replace(".json", ""))
                .collect(Collectors.toList());
    }

    private void ensureTracks(Scene scene) {
        for (SceneTrackType type : SceneTrackType.values()) {
            if (!scene.getTracks().containsKey(type)) {
                scene.getTracks().put(type, new Track<>(type));
            }
        }
    }

    private void scheduleSave(Scene scene, long delayTicks) {
        String key = scene.getName().toLowerCase();
        BukkitTask pending = pendingSaves.get(key);
        if (pending != null) {
            pending.cancel();
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> saveSceneImmediate(scene), delayTicks);
        pendingSaves.put(key, task);
    }
}
