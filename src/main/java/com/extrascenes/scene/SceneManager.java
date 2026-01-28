package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SceneManager {
    private static final int FORMAT_VERSION = 1;
    private final ExtraScenesPlugin plugin;
    private final File scenesFolder;
    private final SceneSerializer serializer = new SceneSerializer();
    private final SceneDeserializer deserializer = new SceneDeserializer();

    public SceneManager(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        this.scenesFolder = new File(plugin.getDataFolder(), "scenes");
        if (!scenesFolder.exists()) {
            scenesFolder.mkdirs();
        }
    }

    public Scene createScene(String name, int durationTicks) {
        Map<SceneTrackType, Track<? extends Keyframe>> tracks = new EnumMap<>(SceneTrackType.class);
        tracks.put(SceneTrackType.CAMERA, new Track<>(SceneTrackType.CAMERA));
        tracks.put(SceneTrackType.COMMAND, new Track<>(SceneTrackType.COMMAND));
        tracks.put(SceneTrackType.MODEL, new Track<>(SceneTrackType.MODEL));
        tracks.put(SceneTrackType.PARTICLE, new Track<>(SceneTrackType.PARTICLE));
        tracks.put(SceneTrackType.SOUND, new Track<>(SceneTrackType.SOUND));
        tracks.put(SceneTrackType.BLOCK_ILLUSION, new Track<>(SceneTrackType.BLOCK_ILLUSION));
        Scene scene = new Scene(name, durationTicks, FORMAT_VERSION, tracks);
        scene.setDefaultSmoothing(readSmoothing());
        scene.setCameraMode(plugin.getConfig().getString("camera.mode", "SPECTATOR"));
        scene.setFreezePlayer(plugin.getConfig().getBoolean("player.freeze", true));
        scene.setAllowGlobalCommands(plugin.getConfig().getBoolean("commands.allowGlobalDefault", false));
        return scene;
    }

    private SmoothingMode readSmoothing() {
        String smoothing = plugin.getConfig().getString("smoothing.default", "EASE_IN_OUT");
        try {
            return SmoothingMode.valueOf(smoothing);
        } catch (IllegalArgumentException ex) {
            return SmoothingMode.EASE_IN_OUT;
        }
    }

    public void saveScene(Scene scene) throws IOException {
        Path file = new File(scenesFolder, scene.getName() + ".json").toPath();
        serializer.write(scene, file);
    }

    public Scene loadScene(String name) {
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
            return scene;
        } catch (IOException ex) {
            return null;
        }
    }

    public boolean deleteScene(String name) {
        File file = new File(scenesFolder, name + ".json");
        return file.delete();
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
}
