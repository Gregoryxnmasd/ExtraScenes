package com.extrascenes;

import com.extrascenes.visibility.SceneVisibilityController;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SceneModelTrackAdapter {
    private final ExtraScenesPlugin plugin;
    private final SceneVisibilityController visibilityController;
    private final boolean modelEngineAvailable;

    public SceneModelTrackAdapter(ExtraScenesPlugin plugin, SceneVisibilityController visibilityController) {
        this.plugin = plugin;
        this.visibilityController = visibilityController;
        this.modelEngineAvailable = plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine");
    }

    public boolean isModelEngineAvailable() {
        return modelEngineAvailable;
    }

    public Entity spawnModelBase(Player owner, Location location) {
        LivingEntity base = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        base.setAI(false);
        base.setSilent(true);
        base.setInvulnerable(true);
        base.setGravity(false);
        visibilityController.hideEntityFromAllExcept(base, owner);
        return base;
    }

    public void bindModel(Entity baseEntity, String modelId) {
        if (!modelEngineAvailable) {
            return;
        }
        // Placeholder for ModelEngine binding.
    }

    public void playAnimation(Entity baseEntity, String animationId, boolean loop, double speed) {
        if (!modelEngineAvailable) {
            return;
        }
        // Placeholder for ModelEngine animation control.
    }

    public void stopAnimation(Entity baseEntity, String animationId) {
        if (!modelEngineAvailable) {
            return;
        }
        // Placeholder for ModelEngine animation control.
    }
}
