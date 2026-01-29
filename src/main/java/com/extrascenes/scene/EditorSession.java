package com.extrascenes.scene;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class EditorSession {
    private final UUID playerUuid;
    private final String sceneName;
    private final Scene scene;
    private SceneTrackType selectedTrack;
    private UUID selectedKeyframeId;
    private int cursorTimeTicks;
    private boolean previewPlaying;
    private int previewTimeTicks;
    private int keyframePage;
    private int currentGroup;
    private int currentTick;
    private Integer armedTick;
    private int armedGroup;
    private int wandSlot;
    private ItemStack wandBackup;
    private GuiType currentGui;
    private final Deque<GuiType> history = new ArrayDeque<>();
    private long lastSavedAt;
    private ConfirmAction confirmAction;
    private SceneTrackType confirmTrack;
    private UUID confirmKeyframeId;
    private Integer confirmCommandIndex;

    public EditorSession(UUID playerUuid, Scene scene) {
        this.playerUuid = playerUuid;
        this.scene = scene;
        this.sceneName = scene.getName();
        this.selectedTrack = SceneTrackType.CAMERA;
        this.cursorTimeTicks = 0;
        this.keyframePage = 0;
        this.currentGroup = 1;
        this.currentTick = 1;
        this.armedTick = null;
        this.armedGroup = 1;
        this.wandSlot = -1;
        this.wandBackup = null;
        this.currentGui = GuiType.SCENE_DASHBOARD;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getSceneName() {
        return sceneName;
    }

    public Scene getScene() {
        return scene;
    }

    public SceneTrackType getSelectedTrack() {
        return selectedTrack;
    }

    public void setSelectedTrack(SceneTrackType selectedTrack) {
        this.selectedTrack = selectedTrack;
    }

    public UUID getSelectedKeyframeId() {
        return selectedKeyframeId;
    }

    public void setSelectedKeyframeId(UUID selectedKeyframeId) {
        this.selectedKeyframeId = selectedKeyframeId;
    }

    public int getCursorTimeTicks() {
        return cursorTimeTicks;
    }

    public void setCursorTimeTicks(int cursorTimeTicks) {
        this.cursorTimeTicks = Math.max(0, cursorTimeTicks);
    }

    public boolean isPreviewPlaying() {
        return previewPlaying;
    }

    public void setPreviewPlaying(boolean previewPlaying) {
        this.previewPlaying = previewPlaying;
    }

    public int getPreviewTimeTicks() {
        return previewTimeTicks;
    }

    public void setPreviewTimeTicks(int previewTimeTicks) {
        this.previewTimeTicks = previewTimeTicks;
    }

    public int getKeyframePage() {
        return keyframePage;
    }

    public void setKeyframePage(int keyframePage) {
        this.keyframePage = Math.max(0, keyframePage);
    }

    public int getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(int currentGroup) {
        this.currentGroup = Math.max(1, currentGroup);
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(int currentTick) {
        this.currentTick = Math.max(1, currentTick);
    }

    public Integer getArmedTick() {
        return armedTick;
    }

    public void setArmedTick(Integer armedTick) {
        this.armedTick = armedTick;
    }

    public int getArmedGroup() {
        return armedGroup;
    }

    public void setArmedGroup(int armedGroup) {
        this.armedGroup = Math.max(1, armedGroup);
    }

    public int getWandSlot() {
        return wandSlot;
    }

    public void setWandSlot(int wandSlot) {
        this.wandSlot = wandSlot;
    }

    public ItemStack getWandBackup() {
        return wandBackup;
    }

    public void setWandBackup(ItemStack wandBackup) {
        this.wandBackup = wandBackup;
    }

    public GuiType getCurrentGui() {
        return currentGui;
    }

    public void setCurrentGui(GuiType currentGui) {
        this.currentGui = currentGui;
    }

    public void pushHistory(GuiType guiType) {
        if (guiType != null) {
            GuiType current = history.peek();
            if (current != guiType) {
                history.push(guiType);
            }
        }
    }

    public GuiType popHistory() {
        return history.isEmpty() ? null : history.pop();
    }

    public void clearHistory() {
        history.clear();
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public long getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(long lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public ConfirmAction getConfirmAction() {
        return confirmAction;
    }

    public void setConfirmAction(ConfirmAction confirmAction) {
        this.confirmAction = confirmAction;
    }

    public SceneTrackType getConfirmTrack() {
        return confirmTrack;
    }

    public void setConfirmTrack(SceneTrackType confirmTrack) {
        this.confirmTrack = confirmTrack;
    }

    public UUID getConfirmKeyframeId() {
        return confirmKeyframeId;
    }

    public void setConfirmKeyframeId(UUID confirmKeyframeId) {
        this.confirmKeyframeId = confirmKeyframeId;
    }

    public Integer getConfirmCommandIndex() {
        return confirmCommandIndex;
    }

    public void setConfirmCommandIndex(Integer confirmCommandIndex) {
        this.confirmCommandIndex = confirmCommandIndex;
    }
}
