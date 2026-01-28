package com.extrascenes.scene;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SceneSession {
    private final UUID playerId;
    private final Scene scene;
    private final PlayerStateSnapshot snapshot;
    private final Set<Entity> sceneEntities = new HashSet<>();
    private final Map<String, UUID> modelRefs = new HashMap<>();
    private SceneState state;
    private int timeTicks;
    private boolean blockingInventory;
    private boolean restorePending;
    private Location lastCameraLocation;
    private UUID cameraRigId;
    private boolean preview;

    public SceneSession(Player player, Scene scene, boolean preview) {
        this.playerId = player.getUniqueId();
        this.scene = scene;
        this.snapshot = new PlayerStateSnapshot(player);
        this.state = SceneState.PLAYING;
        this.timeTicks = 0;
        this.preview = preview;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Scene getScene() {
        return scene;
    }

    public PlayerStateSnapshot getSnapshot() {
        return snapshot;
    }

    public SceneState getState() {
        return state;
    }

    public void setState(SceneState state) {
        this.state = state;
    }

    public int getTimeTicks() {
        return timeTicks;
    }

    public void setTimeTicks(int timeTicks) {
        this.timeTicks = timeTicks;
    }

    public void incrementTime() {
        this.timeTicks++;
    }

    public Set<Entity> getSceneEntities() {
        return Collections.unmodifiableSet(sceneEntities);
    }

    public void registerEntity(Entity entity) {
        sceneEntities.add(entity);
    }

    public void unregisterEntity(Entity entity) {
        sceneEntities.remove(entity);
    }

    public void clearSceneEntities() {
        sceneEntities.clear();
    }

    public Map<String, UUID> getModelRefs() {
        return Collections.unmodifiableMap(modelRefs);
    }

    public void registerModelRef(String ref, UUID entityId) {
        if (ref != null && entityId != null) {
            modelRefs.put(ref, entityId);
        }
    }

    public UUID getModelEntityId(String ref) {
        return modelRefs.get(ref);
    }

    public boolean isBlockingInventory() {
        return blockingInventory;
    }

    public void setBlockingInventory(boolean blockingInventory) {
        this.blockingInventory = blockingInventory;
    }

    public boolean isRestorePending() {
        return restorePending;
    }

    public void setRestorePending(boolean restorePending) {
        this.restorePending = restorePending;
    }

    public Location getLastCameraLocation() {
        return lastCameraLocation;
    }

    public void setLastCameraLocation(Location lastCameraLocation) {
        this.lastCameraLocation = lastCameraLocation;
    }

    public UUID getCameraRigId() {
        return cameraRigId;
    }

    public void setCameraRigId(UUID cameraRigId) {
        this.cameraRigId = cameraRigId;
    }

    public boolean isPreview() {
        return preview;
    }
}
