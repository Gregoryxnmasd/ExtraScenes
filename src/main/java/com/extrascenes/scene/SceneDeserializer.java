package com.extrascenes.scene;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;

public class SceneDeserializer {
    private final Gson gson = new Gson();

    public Scene read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return null;
            }
            return fromJson(element.getAsJsonObject());
        }
    }

    public Scene fromJson(JsonObject root) {
        int formatVersion = root.has("formatVersion") ? root.get("formatVersion").getAsInt() : 1;
        String name = root.has("name") ? root.get("name").getAsString() : "scene";
        int durationTicks = root.has("durationTicks") ? root.get("durationTicks").getAsInt() : 0;
        SmoothingMode defaultSmoothing = parseSmoothing(root.has("defaultSmoothing")
                ? root.get("defaultSmoothing").getAsString()
                : "EASE_IN_OUT");
        SmoothingQuality smoothingQuality = parseQuality(root.has("smoothingQuality")
                ? root.get("smoothingQuality").getAsString()
                : "NORMAL");
        String cameraMode = root.has("cameraMode") ? root.get("cameraMode").getAsString() : "SPECTATOR";
        boolean freezePlayer = !root.has("freezePlayer") || root.get("freezePlayer").getAsBoolean();
        boolean allowGlobalCommands = root.has("allowGlobalCommands") && root.get("allowGlobalCommands").getAsBoolean();

        Map<SceneTrackType, Track<? extends Keyframe>> tracks = new EnumMap<>(SceneTrackType.class);
        JsonObject tracksObject = root.has("tracks") ? root.getAsJsonObject("tracks") : new JsonObject();
        for (SceneTrackType type : SceneTrackType.values()) {
            if (!tracksObject.has(type.name())) {
                continue;
            }
            JsonArray array = tracksObject.getAsJsonArray(type.name());
            Track<Keyframe> track = new Track<>(type);
            for (JsonElement keyframeElement : array) {
                if (!keyframeElement.isJsonObject()) {
                    continue;
                }
                Keyframe keyframe = deserializeKeyframe(type, keyframeElement.getAsJsonObject());
                if (keyframe != null) {
                    track.addKeyframe(keyframe);
                }
            }
            tracks.put(type, track);
        }

        Scene scene = new Scene(name, durationTicks, formatVersion, tracks);
        scene.setDefaultSmoothing(defaultSmoothing);
        scene.setSmoothingQuality(smoothingQuality);
        scene.setCameraMode(cameraMode);
        scene.setFreezePlayer(freezePlayer);
        scene.setAllowGlobalCommands(allowGlobalCommands);
        return scene;
    }

    private SmoothingMode parseSmoothing(String value) {
        try {
            return SmoothingMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return SmoothingMode.EASE_IN_OUT_QUINT;
        }
    }

    private SmoothingQuality parseQuality(String value) {
        try {
            return SmoothingQuality.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return SmoothingQuality.NORMAL;
        }
    }

    private Keyframe deserializeKeyframe(SceneTrackType type, JsonObject object) {
        UUID id = object.has("id") ? UUID.fromString(object.get("id").getAsString()) : UUID.randomUUID();
        int timeTicks = object.has("timeTicks") ? object.get("timeTicks").getAsInt() : 0;
        JsonObject payload = object.has("payload") ? object.getAsJsonObject("payload") : new JsonObject();
        switch (type) {
            case CAMERA -> {
                Transform transform = deserializeTransform(payload.get("transform"));
                SmoothingMode smoothing = payload.has("smoothing")
                        ? SmoothingMode.valueOf(payload.get("smoothing").getAsString())
                        : SmoothingMode.LINEAR;
                boolean instant = payload.has("instant") && payload.get("instant").getAsBoolean();
                LookAtTarget lookAt = deserializeLookAt(payload.get("lookAt"));
                return new CameraKeyframe(id, timeTicks, transform, smoothing, instant, lookAt);
            }
            case COMMAND -> {
                List<String> commands = new ArrayList<>();
                if (payload.has("commands")) {
                    for (JsonElement cmd : payload.getAsJsonArray("commands")) {
                        commands.add(cmd.getAsString());
                    }
                }
                CommandKeyframe.ExecutorMode executor = payload.has("executor")
                        ? CommandKeyframe.ExecutorMode.valueOf(payload.get("executor").getAsString())
                        : CommandKeyframe.ExecutorMode.PLAYER;
                boolean allowGlobal = payload.has("allowGlobal") && payload.get("allowGlobal").getAsBoolean();
                return new CommandKeyframe(id, timeTicks, commands, executor, allowGlobal);
            }
            case MODEL -> {
                ModelKeyframe keyframe = new ModelKeyframe(id, timeTicks,
                        payload.has("action")
                                ? ModelKeyframe.Action.valueOf(payload.get("action").getAsString())
                                : ModelKeyframe.Action.SPAWN);
                keyframe.setModelId(getString(payload, "modelId"));
                keyframe.setEntityRef(getString(payload, "entityRef"));
                keyframe.setAnimationId(getString(payload, "animationId"));
                keyframe.setLoop(payload.has("loop") && payload.get("loop").getAsBoolean());
                keyframe.setSpeed(payload.has("speed") ? payload.get("speed").getAsDouble() : 1.0);
                keyframe.setSpawnTransform(deserializeTransform(payload.get("spawnTransform")));
                return keyframe;
            }
            case PARTICLE -> {
                String particleId = getString(payload, "particleId");
                return new ParticleKeyframe(id, timeTicks, particleId, deserializeTransform(payload.get("transform")));
            }
            case SOUND -> {
                String soundId = getString(payload, "soundId");
                float volume = payload.has("volume") ? payload.get("volume").getAsFloat() : 1.0f;
                float pitch = payload.has("pitch") ? payload.get("pitch").getAsFloat() : 1.0f;
                return new SoundKeyframe(id, timeTicks, soundId, volume, pitch,
                        deserializeTransform(payload.get("transform")));
            }
            case BLOCK_ILLUSION -> {
                Material material = payload.has("material")
                        ? Material.matchMaterial(payload.get("material").getAsString())
                        : Material.BARRIER;
                return new BlockIllusionKeyframe(id, timeTicks, material,
                        deserializeTransform(payload.get("transform")));
            }
            default -> {
                return null;
            }
        }
    }

    private Transform deserializeTransform(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        double x = obj.has("x") ? obj.get("x").getAsDouble() : 0.0;
        double y = obj.has("y") ? obj.get("y").getAsDouble() : 0.0;
        double z = obj.has("z") ? obj.get("z").getAsDouble() : 0.0;
        float yaw = obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0.0f;
        float pitch = obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0.0f;
        return new Transform(x, y, z, yaw, pitch);
    }

    private LookAtTarget deserializeLookAt(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return LookAtTarget.none();
        }
        JsonObject obj = element.getAsJsonObject();
        LookAtTarget.Mode mode = obj.has("mode")
                ? LookAtTarget.Mode.valueOf(obj.get("mode").getAsString())
                : LookAtTarget.Mode.NONE;
        Transform position = deserializeTransform(obj.get("position"));
        UUID entityId = obj.has("entityId") ? UUID.fromString(obj.get("entityId").getAsString()) : null;
        return new LookAtTarget(mode, position, entityId);
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }
}
