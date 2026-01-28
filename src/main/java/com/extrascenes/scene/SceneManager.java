package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class SceneManager {
    private static final int FORMAT_VERSION = 1;
    private final ExtraScenesPlugin plugin;
    private final File scenesFolder;

    public SceneManager(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        this.scenesFolder = new File(plugin.getDataFolder(), "scenes");
        if (!scenesFolder.exists()) {
            scenesFolder.mkdirs();
        }
    }

    public Scene createScene(String name, int durationTicks) {
        SceneTimeline timeline = new SceneTimeline(durationTicks, List.of(new SceneCameraTrack(), new SceneCommandTrack()));
        return new Scene(name, timeline, FORMAT_VERSION);
    }

    public void saveScene(Scene scene) throws IOException {
        File file = new File(scenesFolder, scene.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("formatVersion", scene.getFormatVersion());
        config.set("durationTicks", scene.getTimeline().getDurationTicks());

        ConfigurationSection tracksSection = config.createSection("tracks");
        for (SceneTrack track : scene.getTimeline().getTracks()) {
            ConfigurationSection trackSection = tracksSection.createSection(track.getName());
            int index = 0;
            for (SceneKeyframe keyframe : track.getKeyframes()) {
                ConfigurationSection keySection = trackSection.createSection(String.valueOf(index));
                keySection.set("timeTicks", keyframe.getTimeTicks());
                keySection.set("type", keyframe.getType());
                keySection.set("smoothing", keyframe.getSmoothingMode().name());
                keySection.set("instant", keyframe.isInstant());
                keySection.set("lookAt", keyframe.getLookAt());
                keySection.set("commands", keyframe.getCommands());
                keySection.set("refId", keyframe.getRefId() == null ? null : keyframe.getRefId().toString());
                index++;
            }
        }

        config.save(file);
    }

    public Scene loadScene(String name) {
        File file = new File(scenesFolder, name + ".yml");
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int formatVersion = config.getInt("formatVersion", FORMAT_VERSION);
        int durationTicks = config.getInt("durationTicks", 0);
        List<SceneTrack> tracks = new ArrayList<>();

        ConfigurationSection tracksSection = config.getConfigurationSection("tracks");
        if (tracksSection != null) {
            for (String trackName : tracksSection.getKeys(false)) {
                SceneTrack track = createTrackByName(trackName);
                ConfigurationSection trackSection = tracksSection.getConfigurationSection(trackName);
                if (trackSection != null && track instanceof AbstractSceneTrack abstractTrack) {
                    for (String key : trackSection.getKeys(false)) {
                        ConfigurationSection keySection = trackSection.getConfigurationSection(key);
                        if (keySection == null) {
                            continue;
                        }
                        int timeTicks = keySection.getInt("timeTicks", 0);
                        String type = keySection.getString("type", trackName);
                        SmoothingMode smoothing = SmoothingMode.valueOf(keySection.getString("smoothing", "LINEAR"));
                        boolean instant = keySection.getBoolean("instant", false);
                        String lookAt = keySection.getString("lookAt");
                        List<String> commands = keySection.getStringList("commands");
                        String refIdString = keySection.getString("refId");
                        UUID refId = refIdString == null ? null : UUID.fromString(refIdString);
                        abstractTrack.addKeyframe(new SceneKeyframe(timeTicks, type, smoothing, instant, lookAt, commands, refId));
                    }
                }
                tracks.add(track);
            }
        }

        SceneTimeline timeline = new SceneTimeline(durationTicks, tracks);
        return new Scene(name, timeline, formatVersion);
    }

    public boolean deleteScene(String name) {
        File file = new File(scenesFolder, name + ".yml");
        return file.delete();
    }

    public List<String> listScenes() {
        List<String> scenes = new ArrayList<>();
        File[] files = scenesFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                scenes.add(file.getName().replace(".yml", ""));
            }
        }
        return scenes;
    }

    private SceneTrack createTrackByName(String name) {
        return switch (name) {
            case "camera" -> new SceneCameraTrack();
            case "commands" -> new SceneCommandTrack();
            case "models" -> new SceneModelTrack();
            case "particles" -> new SceneParticleTrack();
            case "sounds" -> new SceneSoundTrack();
            case "block_illusions" -> new SceneBlockIllusionTrack();
            default -> new SceneCommandTrack();
        };
    }
}
