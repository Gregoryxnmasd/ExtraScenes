package com.extrascenes.visibility;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSessionManager;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SceneVisibilityController {
    private final ExtraScenesPlugin plugin;
    private final Map<UUID, Set<UUID>> hiddenPlayersByEntity = new HashMap<>();
    private final Map<UUID, Set<UUID>> shownPlayersByEntity = new HashMap<>();

    public SceneVisibilityController(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
    }

    public void hideEntityFromAllExcept(Entity entity, Player owner) {
        if (entity == null || owner == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(owner.getUniqueId())) {
                online.hideEntity(plugin, entity);
                hiddenPlayersByEntity.computeIfAbsent(entity.getUniqueId(), ignored -> new HashSet<>())
                        .add(online.getUniqueId());
            }
        }
    }

    public void hideEntitiesFromAllExcept(Collection<Entity> entities, Player owner) {
        for (Entity entity : entities) {
            hideEntityFromAllExcept(entity, owner);
        }
    }

    public void showEntityToPlayer(Entity entity, Player player) {
        if (entity == null || player == null) {
            return;
        }
        player.showEntity(plugin, entity);
        shownPlayersByEntity.computeIfAbsent(entity.getUniqueId(), ignored -> new HashSet<>())
                .add(player.getUniqueId());
        Set<UUID> hidden = hiddenPlayersByEntity.get(entity.getUniqueId());
        if (hidden != null) {
            hidden.remove(player.getUniqueId());
        }
    }

    public void hideEntityFromPlayer(Entity entity, Player player) {
        if (entity == null || player == null) {
            return;
        }
        player.hideEntity(plugin, entity);
        hiddenPlayersByEntity.computeIfAbsent(entity.getUniqueId(), ignored -> new HashSet<>())
                .add(player.getUniqueId());
    }

    public void hideAllSceneEntities(Player joiningPlayer) {
        SceneSessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) {
            return;
        }
        for (SceneSession session : sessionManager.getActiveSessions()) {
            for (Entity entity : session.getSceneEntities()) {
                joiningPlayer.hideEntity(plugin, entity);
                hiddenPlayersByEntity.computeIfAbsent(entity.getUniqueId(), ignored -> new HashSet<>())
                        .add(joiningPlayer.getUniqueId());
            }
        }
    }

    public Set<UUID> getHiddenPlayers(UUID entityId) {
        return hiddenPlayersByEntity.containsKey(entityId)
                ? Collections.unmodifiableSet(hiddenPlayersByEntity.get(entityId))
                : Collections.emptySet();
    }

    public Set<UUID> getShownPlayers(UUID entityId) {
        return shownPlayersByEntity.containsKey(entityId)
                ? Collections.unmodifiableSet(shownPlayersByEntity.get(entityId))
                : Collections.emptySet();
    }

    public void clearEntity(UUID entityId) {
        hiddenPlayersByEntity.remove(entityId);
        shownPlayersByEntity.remove(entityId);
    }
}
