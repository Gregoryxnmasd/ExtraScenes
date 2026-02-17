package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class CutscenePathRegistry {
    private final ExtraScenesPlugin plugin;
    private final File file;
    private final Map<String, CutscenePath> paths = new LinkedHashMap<>();

    public CutscenePathRegistry(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "paths.yml");
    }

    public void reload() {
        if (!file.exists()) {
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        paths.clear();
        ConfigurationSection section = yaml.getConfigurationSection("paths");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(id);
            if (node == null) {
                continue;
            }
            CutscenePath path = readPath(node);
            if (path != null) {
                paths.put(id.toLowerCase(Locale.ROOT), path);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("paths");
        for (Map.Entry<String, CutscenePath> entry : paths.entrySet()) {
            writePath(section.createSection(entry.getKey()), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save paths.yml: " + ex.getMessage());
        }
    }

    public CutscenePath getPath(String id) {
        if (id == null) {
            return null;
        }
        return paths.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<String> getIds() {
        return Collections.unmodifiableCollection(paths.keySet());
    }

    public void putPath(String id, CutscenePath path) {
        if (id == null || id.isBlank() || path == null) {
            return;
        }
        paths.put(id.toLowerCase(Locale.ROOT), path);
        save();
    }

    private CutscenePath readPath(ConfigurationSection node) {
        double durationSeconds = Math.max(0.05D, node.getDouble("duration-seconds", 6.0D));
        int durationTicks = Math.max(1, (int) Math.round(durationSeconds * 20.0D));
        double stepResolution = node.getDouble("step-resolution", 0.12D);

        List<CameraKeyframe> points = new ArrayList<>();
        List<Map<?, ?>> rawPoints = node.getMapList("points");
        for (int i = 0; i < rawPoints.size(); i++) {
            Map<?, ?> map = rawPoints.get(i);
            double x = asDouble(map.get("x"));
            double y = asDouble(map.get("y"));
            double z = asDouble(map.get("z"));
            float yaw = (float) asDouble(map.get("yaw"));
            float pitch = (float) asDouble(map.get("pitch"));
            points.add(new CameraKeyframe(UUID.randomUUID(), i, new Transform(x, y, z, yaw, pitch),
                    SmoothingMode.LINEAR, false, LookAtTarget.none()));
        }
        if (points.size() < 2) {
            return null;
        }

        Set<Integer> directPoints = new LinkedHashSet<>();
        for (Object value : node.getList("direct-points", Collections.emptyList())) {
            Integer parsed = asInt(value);
            if (parsed != null && parsed >= 0) {
                directPoints.add(parsed);
            }
        }

        List<CutscenePath.IntRange> playerSegments = new ArrayList<>();
        for (Map<?, ?> rawSegment : node.getMapList("player-segments")) {
            Integer start = asInt(rawSegment.get("start-point"));
            Integer end = asInt(rawSegment.get("end-point"));
            if (start == null || end == null) {
                continue;
            }
            playerSegments.add(new CutscenePath.IntRange(Math.min(start, end), Math.max(start, end)));
        }

        Map<Integer, List<String>> segmentCommands = new LinkedHashMap<>();
        for (Map<?, ?> rawSegment : node.getMapList("segment-commands")) {
            Integer start = asInt(rawSegment.get("start-point"));
            Integer end = asInt(rawSegment.get("end-point"));
            if (start == null || end == null) {
                continue;
            }
            Object rawCommands = rawSegment.get("commands");
            List<String> commands = new ArrayList<>();
            if (rawCommands instanceof List<?> list) {
                for (Object value : list) {
                    if (value != null && !value.toString().isBlank()) {
                        commands.add(value.toString());
                    }
                }
            }
            for (int segment = Math.min(start, end); segment <= Math.max(start, end); segment++) {
                segmentCommands.put(segment, commands);
            }
        }

        Particle particle = parseParticle(node.getString("particle-preview", "end_rod"));
        return new CutscenePath(durationTicks, stepResolution, SmoothingMode.LINEAR, points, playerSegments,
                directPoints, Collections.emptyList(), segmentCommands, particle);
    }

    private void writePath(ConfigurationSection node, CutscenePath path) {
        node.set("duration-seconds", path.getDurationTicks() / 20.0D);
        node.set("step-resolution", path.getStepResolution());
        List<Map<String, Object>> points = new ArrayList<>();
        for (CameraKeyframe keyframe : path.getPoints()) {
            Transform transform = keyframe.getTransform();
            if (transform == null) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("x", transform.getX());
            map.put("y", transform.getY());
            map.put("z", transform.getZ());
            map.put("yaw", transform.getYaw());
            map.put("pitch", transform.getPitch());
            points.add(map);
        }
        node.set("points", points);
        node.set("particle-preview", path.getPreviewParticle().name().toLowerCase(Locale.ROOT));
    }

    private Particle parseParticle(String value) {
        if (value == null || value.isBlank()) {
            return Particle.END_ROD;
        }
        try {
            return Particle.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Particle.END_ROD;
        }
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0D;
        }
    }

    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
