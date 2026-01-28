package com.extrascenes.visibility;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSessionManager;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SceneVisibilityController {
    private final ExtraScenesPlugin plugin;

    public SceneVisibilityController(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
    }

    public void hideEntityFromAllExcept(Entity entity, Player owner) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(owner.getUniqueId())) {
                online.hideEntity(plugin, entity);
            }
        }
    }

    public void hideEntitiesFromAllExcept(Collection<Entity> entities, Player owner) {
        for (Entity entity : entities) {
            hideEntityFromAllExcept(entity, owner);
        }
    }

    public void showEntityToPlayer(Entity entity, Player player) {
        player.showEntity(plugin, entity);
    }

    public void hideEntityFromPlayer(Entity entity, Player player) {
        player.hideEntity(plugin, entity);
    }

    public void hideAllSceneEntities(Player joiningPlayer) {
        SceneSessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) {
            return;
        }
        for (SceneSession session : sessionManager.getActiveSessions()) {
            for (Entity entity : session.getSceneEntities()) {
                joiningPlayer.hideEntity(plugin, entity);
            }
        }
    }
}
