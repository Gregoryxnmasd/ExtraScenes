package com.extrascenes.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class SceneSerializer {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void write(Scene scene, Path destination) throws IOException {
        JsonObject root = toJson(scene);
        Path tempFile = destination.resolveSibling(destination.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            gson.toJson(root, writer);
        }
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public JsonObject toJson(Scene scene) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", scene.getFormatVersion());
        root.addProperty("name", scene.getName());
        root.addProperty("durationTicks", scene.getDurationTicks());
        root.addProperty("defaultSmoothing", scene.getDefaultSmoothing().name());
        root.addProperty("smoothingQuality", scene.getSmoothingQuality().name());
        root.addProperty("cameraMode", scene.getCameraMode());
        root.addProperty("freezePlayer", scene.isFreezePlayer());
        root.addProperty("allowGlobalCommands", scene.isAllowGlobalCommands());

        JsonObject tracks = new JsonObject();
        for (Map.Entry<SceneTrackType, Track<? extends Keyframe>> entry : scene.getTracks().entrySet()) {
            JsonArray keyframes = new JsonArray();
            for (Keyframe keyframe : entry.getValue().getKeyframes()) {
                keyframes.add(serializeKeyframe(keyframe));
            }
            tracks.add(entry.getKey().name(), keyframes);
        }
        root.add("tracks", tracks);
        return root;
    }

    private JsonObject serializeKeyframe(Keyframe keyframe) {
        JsonObject object = new JsonObject();
        object.addProperty("id", keyframe.getId().toString());
        object.addProperty("timeTicks", keyframe.getTimeTicks());
        object.addProperty("type", keyframe.getType());

        JsonObject payload = new JsonObject();
        if (keyframe instanceof CameraKeyframe camera) {
            payload.add("transform", serializeTransform(camera.getTransform()));
            payload.addProperty("smoothing", camera.getSmoothingMode().name());
            payload.addProperty("instant", camera.isInstant());
            payload.add("lookAt", serializeLookAt(camera.getLookAt()));
        } else if (keyframe instanceof CommandKeyframe command) {
            JsonArray commands = new JsonArray();
            for (String cmd : command.getCommands()) {
                commands.add(cmd);
            }
            payload.add("commands", commands);
            payload.addProperty("executor", command.getExecutorMode().name());
            payload.addProperty("allowGlobal", command.isAllowGlobal());
        } else if (keyframe instanceof ModelKeyframe model) {
            payload.addProperty("action", model.getAction().name());
            payload.addProperty("modelId", model.getModelId());
            payload.addProperty("entityRef", model.getEntityRef());
            payload.addProperty("animationId", model.getAnimationId());
            payload.addProperty("loop", model.isLoop());
            payload.addProperty("speed", model.getSpeed());
            payload.add("spawnTransform", serializeTransform(model.getSpawnTransform()));
        } else if (keyframe instanceof ParticleKeyframe particle) {
            payload.addProperty("particleId", particle.getParticleId());
            payload.add("transform", serializeTransform(particle.getTransform()));
        } else if (keyframe instanceof SoundKeyframe sound) {
            payload.addProperty("soundId", sound.getSoundId());
            payload.addProperty("volume", sound.getVolume());
            payload.addProperty("pitch", sound.getPitch());
            payload.add("transform", serializeTransform(sound.getTransform()));
        } else if (keyframe instanceof BlockIllusionKeyframe block) {
            payload.addProperty("material", block.getMaterial().name());
            payload.add("transform", serializeTransform(block.getTransform()));
        }
        object.add("payload", payload);
        return object;
    }

    private JsonObject serializeTransform(Transform transform) {
        if (transform == null) {
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("x", transform.getX());
        obj.addProperty("y", transform.getY());
        obj.addProperty("z", transform.getZ());
        obj.addProperty("yaw", transform.getYaw());
        obj.addProperty("pitch", transform.getPitch());
        return obj;
    }

    private JsonObject serializeLookAt(LookAtTarget target) {
        if (target == null) {
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("mode", target.getMode().name());
        if (target.getPosition() != null) {
            obj.add("position", serializeTransform(target.getPosition()));
        }
        if (target.getEntityId() != null) {
            obj.addProperty("entityId", target.getEntityId().toString());
        }
        return obj;
    }
}
