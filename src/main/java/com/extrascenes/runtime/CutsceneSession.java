package com.extrascenes.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;

public class CutsceneSession {
    public enum State { RUNNING, STOPPED }

    private final UUID viewerId;
    private final String sceneName;
    private UUID rigUuid;
    private int taskId = -1;
    private Location startLocation;
    private GameMode startGameMode;
    private State state = State.RUNNING;
    private int tick;
    private final Map<String, UUID> actorNpcUuids = new HashMap<>();

    public CutsceneSession(UUID viewerId, String sceneName) {
        this.viewerId = viewerId;
        this.sceneName = sceneName;
    }

    public UUID getViewerId() { return viewerId; }
    public String getSceneName() { return sceneName; }
    public UUID getRigUuid() { return rigUuid; }
    public void setRigUuid(UUID rigUuid) { this.rigUuid = rigUuid; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public Location getStartLocation() { return startLocation; }
    public void setStartLocation(Location startLocation) { this.startLocation = startLocation; }
    public GameMode getStartGameMode() { return startGameMode; }
    public void setStartGameMode(GameMode startGameMode) { this.startGameMode = startGameMode; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }
    public Map<String, UUID> getActorNpcUuids() { return actorNpcUuids; }
}
