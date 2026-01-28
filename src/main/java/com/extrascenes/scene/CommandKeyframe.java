package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandKeyframe extends Keyframe {
    public enum ExecutorMode {
        PLAYER,
        CONSOLE
    }

    private final List<String> commands;
    private ExecutorMode executorMode;
    private boolean allowGlobal;

    public CommandKeyframe(UUID id, int timeTicks, List<String> commands, ExecutorMode executorMode, boolean allowGlobal) {
        super(id, timeTicks);
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
        this.executorMode = executorMode == null ? ExecutorMode.PLAYER : executorMode;
        this.allowGlobal = allowGlobal;
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.COMMAND;
    }

    @Override
    public String getType() {
        return "command";
    }

    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    public void setCommands(List<String> commands) {
        this.commands.clear();
        if (commands != null) {
            this.commands.addAll(commands);
        }
    }

    public void addCommand(String command) {
        this.commands.add(command);
    }

    public ExecutorMode getExecutorMode() {
        return executorMode;
    }

    public void setExecutorMode(ExecutorMode executorMode) {
        this.executorMode = executorMode;
    }

    public boolean isAllowGlobal() {
        return allowGlobal;
    }

    public void setAllowGlobal(boolean allowGlobal) {
        this.allowGlobal = allowGlobal;
    }
}
