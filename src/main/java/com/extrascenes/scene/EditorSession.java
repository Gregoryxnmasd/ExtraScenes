package com.extrascenes.scene;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class EditorSession {
    private final UUID playerUuid;
    private final String sceneName;
    private Scene scene;
    private SceneTrackType selectedTrack;
    private UUID selectedKeyframeId;
    private int cursorTimeTicks;
    private boolean previewPlaying;
    private int previewTimeTicks;
    private int keyframePage;
    private int groupPage;
    private int currentGroup;
    private int currentTick;
    private Integer armedTick;
    private int armedGroup;
    private int wandSlot;
    private ItemStack wandBackup;
    private String selectedModelEntryName;
    private String selectedActorId;
    private boolean previewOtherActors;
    private int actorRecordingStartTick;
    private int actorsPage;
    private String armedModelEntryName;
    private UUID armedModelKeyframeId;
    private GuiType armedReturnGui;
    private GuiType currentGui;
    private final Deque<NavigationState> history = new ArrayDeque<>();
    private long lastSavedAt;
    private ConfirmAction confirmAction;
    private SceneTrackType confirmTrack;
    private UUID confirmKeyframeId;
    private Integer confirmCommandIndex;
    private long guiTransitionGuardUntilMs;

    public EditorSession(UUID playerUuid, Scene scene) {
        this.playerUuid = playerUuid;
        this.scene = scene;
        this.sceneName = scene.getName();
        this.selectedTrack = SceneTrackType.CAMERA;
        this.cursorTimeTicks = 0;
        this.keyframePage = 0;
        this.groupPage = 0;
        this.currentGroup = 1;
        this.currentTick = 1;
        this.armedTick = null;
        this.armedGroup = 1;
        this.wandSlot = -1;
        this.wandBackup = null;
        this.currentGui = GuiType.SCENE_DASHBOARD;
        this.previewOtherActors = true;
        this.actorRecordingStartTick = 1;
        this.actorsPage = 0;
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

    public void setScene(Scene scene) {
        if (scene != null && scene.getName().equalsIgnoreCase(sceneName)) {
            this.scene = scene;
        }
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

    public int getGroupPage() {
        return groupPage;
    }

    public void setGroupPage(int groupPage) {
        this.groupPage = Math.max(0, groupPage);
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

    public String getSelectedActorId() {
        return selectedActorId;
    }

    public void setSelectedActorId(String selectedActorId) {
        this.selectedActorId = selectedActorId;
    }

    public boolean isPreviewOtherActors() {
        return previewOtherActors;
    }

    public void setPreviewOtherActors(boolean previewOtherActors) {
        this.previewOtherActors = previewOtherActors;
    }

    public int getActorRecordingStartTick() {
        return actorRecordingStartTick;
    }

    public void setActorRecordingStartTick(int actorRecordingStartTick) {
        this.actorRecordingStartTick = Math.max(1, actorRecordingStartTick);
    }

    public int getActorsPage() {
        return actorsPage;
    }

    public void setActorsPage(int actorsPage) {
        this.actorsPage = Math.max(0, actorsPage);
    }

    public String getSelectedModelEntryName() {
        return selectedModelEntryName;
    }

    public void setSelectedModelEntryName(String selectedModelEntryName) {
        this.selectedModelEntryName = selectedModelEntryName;
    }

    public String getArmedModelEntryName() {
        return armedModelEntryName;
    }

    public void setArmedModelEntryName(String armedModelEntryName) {
        this.armedModelEntryName = armedModelEntryName;
    }

    public UUID getArmedModelKeyframeId() {
        return armedModelKeyframeId;
    }

    public void setArmedModelKeyframeId(UUID armedModelKeyframeId) {
        this.armedModelKeyframeId = armedModelKeyframeId;
    }

    public GuiType getArmedReturnGui() {
        return armedReturnGui;
    }

    public void setArmedReturnGui(GuiType armedReturnGui) {
        this.armedReturnGui = armedReturnGui;
    }

    public GuiType getCurrentGui() {
        return currentGui;
    }

    public void setCurrentGui(GuiType currentGui) {
        this.currentGui = currentGui;
    }

    public void pushHistory(GuiType guiType) {
        if (guiType != null) {
            NavigationState snapshot = new NavigationState(guiType, keyframePage, groupPage, currentGroup, currentTick,
                    actorsPage, selectedActorId);
            if (!snapshot.equals(history.peek())) {
                history.push(snapshot);
            }
        }
    }

    public NavigationState popHistory() {
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

    public void armGuiTransitionGuard(long durationMs) {
        this.guiTransitionGuardUntilMs = System.currentTimeMillis() + Math.max(0L, durationMs);
    }

    public boolean isGuiTransitionGuardActive() {
        return System.currentTimeMillis() <= guiTransitionGuardUntilMs;
    }

    public record NavigationState(GuiType guiType, int keyframePage, int groupPage, int currentGroup,
                                  int currentTick, int actorsPage, String selectedActorId) {
    }
}
