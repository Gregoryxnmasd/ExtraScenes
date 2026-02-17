package com.extrascenes;

import com.extrascenes.visibility.SceneVisibilityController;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SceneModelTrackAdapter {
    private final ExtraScenesPlugin plugin;
    private final SceneVisibilityController visibilityController;
    private boolean modelEngineAvailable;
    private Method createActiveModelMethod;
    private Method getOrCreateModeledEntityMethod;
    private Method getModeledEntityMethod;
    private Method modeledEntityAddModelMethod;
    private Method modeledEntityRegisterMethod;
    private Method modeledEntityGetModelMethod;
    private Method modeledEntityGetModelsMethod;
    private Method activeModelGetAnimationHandlerMethod;
    private Method animationHandlerPlayMethod;
    private Method animationHandlerStopMethod;
    private Method animationHandlerStopAllMethod;
    private final Map<UUID, Set<UUID>> ownedModelBases = new java.util.concurrent.ConcurrentHashMap<>();

    public SceneModelTrackAdapter(ExtraScenesPlugin plugin, SceneVisibilityController visibilityController) {
        this.plugin = plugin;
        this.visibilityController = visibilityController;
        Plugin modelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        this.modelEngineAvailable = modelEngine != null && modelEngine.isEnabled();
        if (this.modelEngineAvailable) {
            initializeReflection();
        }
    }

    public boolean isModelEngineAvailable() {
        return modelEngineAvailable;
    }

    public Entity spawnModelBase(Player owner, Location location) {
        cleanupInvalidBases(owner);
        Entity base = spawnModelAnchor(location);
        if (base == null) {
            throw new IllegalStateException("Unable to spawn model anchor");
        }
        configureBaseEntity(base, owner);
        trackBase(owner, base);
        return base;
    }

    private Entity spawnModelAnchor(Location location) {
        Entity anchor = null;
        try {
            anchor = location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        } catch (Throwable ignored) {
            // fallback below
        }
        if (anchor == null) {
            anchor = location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        }
        return anchor;
    }

    private void configureBaseEntity(Entity entity, Player owner) {
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.setVisibleByDefault(false);
        if (entity instanceof org.bukkit.entity.Interaction interaction) {
            interaction.setResponsive(false);
            interaction.setInteractionWidth(0.05F);
            interaction.setInteractionHeight(0.05F);
        }
        if (entity instanceof ArmorStand base) {
            base.setAI(false);
            base.setInvisible(true);
            base.setMarker(true);
            base.setBasePlate(false);
            base.setArms(false);
            base.setCollidable(false);
            base.setSmall(true);
            base.setCanMove(false);
        }
        visibilityController.hideEntityFromAllExcept(entity, owner);
        visibilityController.showEntityToPlayer(entity, owner);
    }

    private void trackBase(Player owner, Entity base) {
        if (owner == null || base == null) {
            return;
        }
        ownedModelBases.computeIfAbsent(owner.getUniqueId(), ignored -> new HashSet<>())
                .add(base.getUniqueId());
    }

    private void cleanupInvalidBases(Player owner) {
        if (owner == null) {
            return;
        }
        Set<UUID> bases = ownedModelBases.get(owner.getUniqueId());
        if (bases == null || bases.isEmpty()) {
            return;
        }
        bases.removeIf(id -> {
            Entity entity = Bukkit.getEntity(id);
            return entity == null || !entity.isValid();
        });
        if (bases.isEmpty()) {
            ownedModelBases.remove(owner.getUniqueId());
        }
    }

    public void unregisterModelBase(Player owner, Entity baseEntity) {
        if (owner == null || baseEntity == null) {
            return;
        }
        Set<UUID> bases = ownedModelBases.get(owner.getUniqueId());
        if (bases == null) {
            return;
        }
        bases.remove(baseEntity.getUniqueId());
        if (bases.isEmpty()) {
            ownedModelBases.remove(owner.getUniqueId());
        }
    }

    public void bindModel(Entity baseEntity, String modelId) {
        if (!modelEngineAvailable) {
            return;
        }
        if (modelId == null || modelId.isBlank()) {
            return;
        }
        try {
            Object modeledEntity = getOrCreateModeledEntityMethod.invoke(null, baseEntity);
            Object activeModel = createActiveModelMethod.invoke(null, modelId);
            modeledEntityAddModelMethod.invoke(modeledEntity, activeModel, true);
            modeledEntityRegisterMethod.invoke(modeledEntity);
        } catch (Exception ex) {
            modelEngineAvailable = false;
        }
    }

    public void playAnimation(Entity baseEntity, String modelId, String animationId, boolean loop, double speed) {
        if (!modelEngineAvailable) {
            return;
        }
        if (animationId == null || animationId.isBlank()) {
            return;
        }
        try {
            Object modeledEntity = getModeledEntityMethod.invoke(null, baseEntity);
            Object activeModel = resolveActiveModel(modeledEntity, modelId);
            if (activeModel == null) {
                return;
            }
            Object handler = activeModelGetAnimationHandlerMethod.invoke(activeModel);
            animationHandlerPlayMethod.invoke(handler, animationId, 0.0, 0.0, speed, loop);
        } catch (Exception ex) {
            modelEngineAvailable = false;
        }
    }

    public void stopAnimation(Entity baseEntity, String modelId, String animationId) {
        if (!modelEngineAvailable) {
            return;
        }
        try {
            Object modeledEntity = getModeledEntityMethod.invoke(null, baseEntity);
            Object activeModel = resolveActiveModel(modeledEntity, modelId);
            if (activeModel == null) {
                return;
            }
            Object handler = activeModelGetAnimationHandlerMethod.invoke(activeModel);
            if (animationId == null || animationId.isBlank()) {
                animationHandlerStopAllMethod.invoke(handler);
            } else {
                animationHandlerStopMethod.invoke(handler, animationId);
            }
        } catch (Exception ex) {
            modelEngineAvailable = false;
        }
    }

    private Object resolveActiveModel(Object modeledEntity, String modelId) throws Exception {
        if (modeledEntity == null) {
            return null;
        }
        if (modelId != null) {
            Object optional = modeledEntityGetModelMethod.invoke(modeledEntity, modelId);
            if (optional instanceof Optional<?> opt && opt.isPresent()) {
                return opt.orElse(null);
            }
        }
        Object models = modeledEntityGetModelsMethod.invoke(modeledEntity);
        if (models instanceof Map<?, ?> map && !map.isEmpty()) {
            return map.values().iterator().next();
        }
        return null;
    }

    private void initializeReflection() {
        try {
            Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            createActiveModelMethod = api.getMethod("createActiveModel", String.class);
            getOrCreateModeledEntityMethod = api.getMethod("getOrCreateModeledEntity", Entity.class);
            getModeledEntityMethod = api.getMethod("getModeledEntity", Entity.class);

            Class<?> modeledEntityClass = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            modeledEntityAddModelMethod = modeledEntityClass.getMethod("addModel",
                    Class.forName("com.ticxo.modelengine.api.model.ActiveModel"), boolean.class);
            modeledEntityRegisterMethod = modeledEntityClass.getMethod("registerSelf");
            modeledEntityGetModelMethod = modeledEntityClass.getMethod("getModel", String.class);
            modeledEntityGetModelsMethod = modeledEntityClass.getMethod("getModels");

            Class<?> activeModelClass = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
            activeModelGetAnimationHandlerMethod = activeModelClass.getMethod("getAnimationHandler");

            Class<?> animationHandlerClass = Class.forName("com.ticxo.modelengine.api.animation.handler.AnimationHandler");
            animationHandlerPlayMethod = animationHandlerClass.getMethod("playAnimation",
                    String.class, double.class, double.class, double.class, boolean.class);
            animationHandlerStopMethod = animationHandlerClass.getMethod("stopAnimation", String.class);
            animationHandlerStopAllMethod = animationHandlerClass.getMethod("forceStopAllAnimations");
        } catch (Exception ex) {
            modelEngineAvailable = false;
        }
    }
}
