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
import java.util.HashMap;
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
        root.addProperty("sceneId", scene.getSceneId());
        root.addProperty("name", scene.getName());
        root.addProperty("durationTicks", scene.getDurationTicks());
        root.addProperty("defaultSmoothing", scene.getDefaultSmoothing().name());
        root.addProperty("smoothingQuality", scene.getSmoothingQuality().name());
        root.addProperty("freezePlayer", scene.isFreezePlayer());
        root.addProperty("allowGlobalCommands", scene.isAllowGlobalCommands());
        root.addProperty("endTeleportMode", scene.getEndTeleportMode().name());
        if (scene.getEndLocation() != null) {
            root.add("endLocation", serializeLocation(scene.getEndLocation()));
        }
        root.add("modelLibrary", serializeModelLibrary(scene));
        root.add("actorTemplates", serializeActorTemplates(scene));

        JsonObject tracks = new JsonObject();
        for (Map.Entry<SceneTrackType, Track<? extends Keyframe>> entry : scene.getTracks().entrySet()) {
            JsonArray keyframes = new JsonArray();
            for (Keyframe keyframe : entry.getValue().getKeyframes()) {
                keyframes.add(serializeKeyframe(keyframe));
            }
            tracks.add(entry.getKey().name(), keyframes);
        }
        root.add("tracks", tracks);
        root.add("ticks", serializeTicks(scene));
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
        } else if (keyframe instanceof ActionBarKeyframe actionBar) {
            payload.addProperty("text", actionBar.getText());
            payload.addProperty("durationTicks", actionBar.getDurationTicks());
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
            payload.addProperty("modelEntry", model.getModelEntry());
            payload.addProperty("handle", model.getEntityRef());
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
        if (transform.getWorldName() != null) {
            obj.addProperty("world", transform.getWorldName());
        }
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

    private JsonArray serializeModelLibrary(Scene scene) {
        JsonArray array = new JsonArray();
        for (SceneModelEntry entry : scene.getModelLibrary().values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", entry.getName());
            obj.addProperty("modelId", entry.getModelId());
            obj.addProperty("defaultAnimation", entry.getDefaultAnimation());
            obj.add("spawnTransform", serializeTransform(entry.getSpawnTransform()));
            array.add(obj);
        }
        return array;
    }

    private JsonArray serializeActorTemplates(Scene scene) {
        JsonArray array = new JsonArray();
        for (SceneActorTemplate template : scene.getActorTemplates().values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("actorId", template.getActorId());
            obj.addProperty("entityType", template.getEntityType().name());
            obj.addProperty("displayName", template.getDisplayName());
            obj.addProperty("skin", template.getSkinName());
            obj.addProperty("skinSignature", template.getSkinSignature());
            obj.addProperty("skinTexture", template.getSkinTexture());
            obj.addProperty("skinCacheKey", template.getSkinCacheKey());
            obj.addProperty("scale", template.getScale());
            obj.addProperty("playbackMode", template.getPlaybackMode().name());
            obj.addProperty("previewEnabled", template.isPreviewEnabled());
            JsonArray ticks = new JsonArray();
            for (ActorTransformTick tick : template.getTransformTicks().values()) {
                JsonObject tickObject = new JsonObject();
                tickObject.addProperty("tick", tick.getTick());
                tickObject.add("transform", serializeTransform(tick.getTransform()));
                tickObject.addProperty("sneaking", tick.isSneaking());
                tickObject.addProperty("sprinting", tick.isSprinting());
                tickObject.addProperty("swimming", tick.isSwimming());
                tickObject.addProperty("gliding", tick.isGliding());
                ticks.add(tickObject);
            }
            obj.add("movement", ticks);
            JsonArray actions = new JsonArray();
            for (ActorTickAction action : template.getTickActions().values()) {
                JsonObject actionObj = new JsonObject();
                actionObj.addProperty("tick", action.getTick());
                actionObj.addProperty("spawn", action.isSpawn());
                actionObj.addProperty("despawn", action.isDespawn());
                actionObj.addProperty("manualTransform", action.isManualTransform());
                actionObj.addProperty("animation", action.getAnimation());
                actionObj.addProperty("stopAnimation", action.isStopAnimation());
                actionObj.add("lookAt", serializeLookAt(action.getLookAtTarget()));
                actionObj.addProperty("command", action.getCommand());
                actionObj.addProperty("scale", action.getScale());
                actionObj.addProperty("skin", action.getSkinName());
                actions.add(actionObj);
            }
            obj.add("actions", actions);
            array.add(obj);
        }
        return array;
    }

    private JsonObject serializeTicks(Scene scene) {
        Map<Integer, JsonObject> ticks = new HashMap<>();
        for (Track<? extends Keyframe> track : scene.getTracks().values()) {
            for (Keyframe keyframe : track.getKeyframes()) {
                JsonObject tickObject = ticks.computeIfAbsent(keyframe.getTimeTicks(), time -> new JsonObject());
                if (keyframe instanceof CameraKeyframe camera) {
                    JsonObject cameraPayload = new JsonObject();
                    cameraPayload.add("transform", serializeTransform(camera.getTransform()));
                    cameraPayload.addProperty("smoothing", camera.getSmoothingMode().name());
                    cameraPayload.addProperty("instant", camera.isInstant());
                    cameraPayload.add("lookAt", serializeLookAt(camera.getLookAt()));
                    tickObject.add("camera", cameraPayload);
                } else if (keyframe instanceof CommandKeyframe command) {
                    JsonObject commandPayload = new JsonObject();
                    JsonArray commands = new JsonArray();
                    for (String cmd : command.getCommands()) {
                        commands.add(cmd);
                    }
                    commandPayload.add("commands", commands);
                    commandPayload.addProperty("executor", command.getExecutorMode().name());
                    commandPayload.addProperty("allowGlobal", command.isAllowGlobal());
                    tickObject.add("commands", commandPayload);
                } else if (keyframe instanceof ActionBarKeyframe actionBar) {
                    JsonObject actionPayload = new JsonObject();
                    actionPayload.addProperty("text", actionBar.getText());
                    actionPayload.addProperty("durationTicks", actionBar.getDurationTicks());
                    tickObject.add("actionbar", actionPayload);
                } else if (keyframe instanceof ModelKeyframe model) {
                    JsonArray models = tickObject.has("models")
                            ? tickObject.getAsJsonArray("models")
                            : new JsonArray();
                    JsonObject modelPayload = new JsonObject();
                    modelPayload.addProperty("action", model.getAction().name());
                    modelPayload.addProperty("modelId", model.getModelId());
                    modelPayload.addProperty("modelEntry", model.getModelEntry());
                    modelPayload.addProperty("handle", model.getEntityRef());
                    modelPayload.addProperty("animationId", model.getAnimationId());
                    modelPayload.addProperty("loop", model.isLoop());
                    modelPayload.addProperty("speed", model.getSpeed());
                    modelPayload.add("spawnTransform", serializeTransform(model.getSpawnTransform()));
                    models.add(modelPayload);
                    tickObject.add("models", models);
                } else if (keyframe instanceof ParticleKeyframe particle) {
                    JsonObject effects = getEffectsObject(tickObject);
                    JsonArray particles = effects.has("particles") ? effects.getAsJsonArray("particles") : new JsonArray();
                    JsonObject payload = new JsonObject();
                    payload.addProperty("particleId", particle.getParticleId());
                    payload.add("transform", serializeTransform(particle.getTransform()));
                    particles.add(payload);
                    effects.add("particles", particles);
                } else if (keyframe instanceof SoundKeyframe sound) {
                    JsonObject effects = getEffectsObject(tickObject);
                    JsonArray sounds = effects.has("sounds") ? effects.getAsJsonArray("sounds") : new JsonArray();
                    JsonObject payload = new JsonObject();
                    payload.addProperty("soundId", sound.getSoundId());
                    payload.addProperty("volume", sound.getVolume());
                    payload.addProperty("pitch", sound.getPitch());
                    payload.add("transform", serializeTransform(sound.getTransform()));
                    sounds.add(payload);
                    effects.add("sounds", sounds);
                } else if (keyframe instanceof BlockIllusionKeyframe block) {
                    JsonObject effects = getEffectsObject(tickObject);
                    JsonArray blocks = effects.has("blocks") ? effects.getAsJsonArray("blocks") : new JsonArray();
                    JsonObject payload = new JsonObject();
                    payload.addProperty("material", block.getMaterial().name());
                    payload.add("transform", serializeTransform(block.getTransform()));
                    blocks.add(payload);
                    effects.add("blocks", blocks);
                }
            }
        }
        JsonObject ticksObject = new JsonObject();
        for (Map.Entry<Integer, JsonObject> entry : ticks.entrySet()) {
            ticksObject.add(String.valueOf(entry.getKey()), entry.getValue());
        }
        return ticksObject;
    }

    private JsonObject getEffectsObject(JsonObject tickObject) {
        if (tickObject.has("effects") && tickObject.get("effects").isJsonObject()) {
            return tickObject.getAsJsonObject("effects");
        }
        JsonObject effects = new JsonObject();
        tickObject.add("effects", effects);
        return effects;
    }

    private JsonObject serializeLocation(SceneLocation location) {
        if (location == null) {
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("world", location.getWorldName());
        obj.addProperty("x", location.getX());
        obj.addProperty("y", location.getY());
        obj.addProperty("z", location.getZ());
        obj.addProperty("yaw", location.getYaw());
        obj.addProperty("pitch", location.getPitch());
        return obj;
    }
}
