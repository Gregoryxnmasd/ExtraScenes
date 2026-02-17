package com.extrascenes.scene;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class SceneSession {
    private final UUID sessionId;
    private final UUID playerId;
    private final Scene scene;
    private final PlayerStateSnapshot snapshot;
    private final Set<Entity> sceneEntities = new HashSet<>();
    private final Map<String, UUID> modelRefs = new HashMap<>();
    private final Map<String, String> lastModelHandleByEntry = new HashMap<>();
    private final Map<String, SessionActorHandle> actorHandles = new HashMap<>();
    private SceneState state;
    private int timeTicks;
    private int startTick;
    private int endTick;
    private boolean blockingInventory;
    private boolean restorePending;
    private Location lastCameraLocation;
    private UUID cameraRigId;
    private String cameraRigWorld;
    private boolean preview;
    private Location startLocation;
    private int actionBarUntilTick;
    private String activeActionBarText;
    private String lastModelHandle;
    private BukkitTask runtimeTask;
    private final List<BukkitTask> ownedTasks = new ArrayList<>();
    private int spectatorRecoveryCooldownUntilTick;
    private String forcedChunkWorld;
    private int forcedChunkX;
    private int forcedChunkZ;
    private org.bukkit.inventory.ItemStack originalHelmet;
    private final SessionEntityTracker entityTracker = new SessionEntityTracker();
    private int playbackTeleportCount;
    private String lastPlaybackTeleportCaller;
    private boolean spectatorHandshakeComplete;
    private int spectatorHandshakeAttempts;
    private java.util.List<CutsceneFrame> cameraTimeline = java.util.Collections.emptyList();
    private CutscenePath cutscenePath;
    private int lastAppliedSegmentIndex = -1;
    private CutscenePath cutscenePath;
    private final Set<Integer> executedSegmentCommands = new LinkedHashSet<>();

    public SceneSession(Player player, Scene scene, boolean preview) {
        this(player, scene, preview, 0, scene.getDurationTicks() <= 0 ? Integer.MAX_VALUE : scene.getDurationTicks());
    }

    public SceneSession(Player player, Scene scene, boolean preview, int startTick, int endTick) {
        this.playerId = player.getUniqueId();
        this.sessionId = UUID.randomUUID();
        this.scene = scene;
        this.snapshot = new PlayerStateSnapshot(player);
        this.state = SceneState.PLAYING;
        this.startTick = Math.max(0, startTick);
        this.endTick = endTick <= 0 ? Integer.MAX_VALUE : endTick;
        this.timeTicks = this.startTick;
        this.preview = preview;
        this.spectatorRecoveryCooldownUntilTick = 0;
        this.playbackTeleportCount = 0;
        this.lastPlaybackTeleportCaller = null;
        this.spectatorHandshakeComplete = false;
        this.spectatorHandshakeAttempts = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getSessionId() {
        return sessionId;
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

    public int getStartTick() {
        return startTick;
    }

    public void setStartTick(int startTick) {
        this.startTick = Math.max(0, startTick);
    }

    public int getEndTick() {
        return endTick;
    }

    public void setEndTick(int endTick) {
        this.endTick = endTick <= 0 ? Integer.MAX_VALUE : endTick;
    }

    public Set<Entity> getSceneEntities() {
        return Collections.unmodifiableSet(sceneEntities);
    }

    public void registerEntity(Entity entity) {
        sceneEntities.add(entity);
        entityTracker.register(entity);
    }

    public void unregisterEntity(Entity entity) {
        sceneEntities.remove(entity);
        entityTracker.unregister(entity);
    }

    public void clearSceneEntities() {
        sceneEntities.clear();
        entityTracker.clear();
    }

    public SessionEntityTracker getEntityTracker() {
        return entityTracker;
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

    public String getCameraRigWorld() {
        return cameraRigWorld;
    }

    public void setCameraRigWorld(String cameraRigWorld) {
        this.cameraRigWorld = cameraRigWorld;
    }

    public boolean isPreview() {
        return preview;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public int getActionBarUntilTick() {
        return actionBarUntilTick;
    }

    public void setActionBarUntilTick(int actionBarUntilTick) {
        this.actionBarUntilTick = actionBarUntilTick;
    }

    public String getActiveActionBarText() {
        return activeActionBarText;
    }

    public void setActiveActionBarText(String activeActionBarText) {
        this.activeActionBarText = activeActionBarText;
    }

    public String getLastModelHandle() {
        return lastModelHandle;
    }

    public void setLastModelHandle(String lastModelHandle) {
        this.lastModelHandle = lastModelHandle;
    }

    public String getLastModelHandleForEntry(String entryName) {
        if (entryName == null) {
            return null;
        }
        return lastModelHandleByEntry.get(entryName.toLowerCase());
    }

    public void setLastModelHandleForEntry(String entryName, String handle) {
        if (entryName == null || handle == null) {
            return;
        }
        lastModelHandleByEntry.put(entryName.toLowerCase(), handle);
    }


    public Map<String, SessionActorHandle> getActorHandles() {
        return Collections.unmodifiableMap(actorHandles);
    }

    public void registerActorHandle(SessionActorHandle handle) {
        if (handle == null || handle.getActorId() == null) {
            return;
        }
        actorHandles.put(handle.getActorId().toLowerCase(), handle);
    }

    public SessionActorHandle getActorHandle(String actorId) {
        if (actorId == null) {
            return null;
        }
        return actorHandles.get(actorId.toLowerCase());
    }

    public void unregisterActorHandle(String actorId) {
        if (actorId == null) {
            return;
        }
        actorHandles.remove(actorId.toLowerCase());
    }

    public void clearActorHandles() {
        actorHandles.clear();
    }

    public BukkitTask getRuntimeTask() {
        return runtimeTask;
    }

    public void setRuntimeTask(BukkitTask runtimeTask) {
        this.runtimeTask = runtimeTask;
    }

    public void registerOwnedTask(BukkitTask task) {
        if (task != null) {
            ownedTasks.add(task);
        }
    }

    public List<BukkitTask> getOwnedTasks() {
        return Collections.unmodifiableList(ownedTasks);
    }

    public void clearOwnedTasks() {
        ownedTasks.clear();
    }

    public int getSpectatorRecoveryCooldownUntilTick() {
        return spectatorRecoveryCooldownUntilTick;
    }

    public void setSpectatorRecoveryCooldownUntilTick(int spectatorRecoveryCooldownUntilTick) {
        this.spectatorRecoveryCooldownUntilTick = Math.max(0, spectatorRecoveryCooldownUntilTick);
    }

    public org.bukkit.inventory.ItemStack getOriginalHelmet() {
        return originalHelmet;
    }

    public void setOriginalHelmet(org.bukkit.inventory.ItemStack originalHelmet) {
        this.originalHelmet = originalHelmet;
    }

    public String getForcedChunkWorld() {
        return forcedChunkWorld;
    }

    public int getForcedChunkX() {
        return forcedChunkX;
    }

    public int getForcedChunkZ() {
        return forcedChunkZ;
    }

    public void setForcedChunk(String worldName, int chunkX, int chunkZ) {
        this.forcedChunkWorld = worldName;
        this.forcedChunkX = chunkX;
        this.forcedChunkZ = chunkZ;
    }

    public int getPlaybackTeleportCount() {
        return playbackTeleportCount;
    }

    public void incrementPlaybackTeleportCount() {
        this.playbackTeleportCount++;
    }

    public void incrementPlaybackTeleportCount(String caller) {
        this.playbackTeleportCount++;
        this.lastPlaybackTeleportCaller = caller;
    }

    public String getLastPlaybackTeleportCaller() {
        return lastPlaybackTeleportCaller;
    }

    public boolean isSpectatorHandshakeComplete() {
        return spectatorHandshakeComplete;
    }

    public void setSpectatorHandshakeComplete(boolean spectatorHandshakeComplete) {
        this.spectatorHandshakeComplete = spectatorHandshakeComplete;
    }

    public int getSpectatorHandshakeAttempts() {
        return spectatorHandshakeAttempts;
    }

    public void incrementSpectatorHandshakeAttempts() {
        this.spectatorHandshakeAttempts++;
    }

    public java.util.List<CutsceneFrame> getCameraTimeline() {
        return cameraTimeline;
    }

    public void setCameraTimeline(java.util.List<CutsceneFrame> cameraTimeline) {
        this.cameraTimeline = cameraTimeline == null ? java.util.Collections.emptyList() : cameraTimeline;
    }

    public CutscenePath getCutscenePath() {
        return cutscenePath;
    }

    public void setCutscenePath(CutscenePath cutscenePath) {
        this.cutscenePath = cutscenePath;
    }

    public int getLastAppliedSegmentIndex() {
        return lastAppliedSegmentIndex;
    }

    public void setLastAppliedSegmentIndex(int lastAppliedSegmentIndex) {
        this.lastAppliedSegmentIndex = lastAppliedSegmentIndex;
    }

    public CutscenePath getCutscenePath() {
        return cutscenePath;
    }

    public void setCutscenePath(CutscenePath cutscenePath) {
        this.cutscenePath = cutscenePath;
    }

    public boolean markSegmentCommandExecuted(int segmentIndex) {
        return executedSegmentCommands.add(segmentIndex);
    }

    public void resetSegmentCommandExecution() {
        executedSegmentCommands.clear();
    }
}

