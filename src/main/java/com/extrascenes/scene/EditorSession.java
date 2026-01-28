package com.extrascenes.scene;

import java.util.UUID;

public class EditorSession {
    private final UUID playerUuid;
    private final String sceneName;
    private final Scene scene;
    private SceneTrackType selectedTrack;
    private UUID selectedKeyframeId;
    private int cursorTimeTicks;
    private boolean previewPlaying;
    private int previewTimeTicks;
    private EditorMenu lastOpenedMenu;
    private int keyframePage;

    public EditorSession(UUID playerUuid, Scene scene) {
        this.playerUuid = playerUuid;
        this.scene = scene;
        this.sceneName = scene.getName();
        this.selectedTrack = SceneTrackType.CAMERA;
        this.cursorTimeTicks = 0;
        this.lastOpenedMenu = EditorMenu.MAIN;
        this.keyframePage = 0;
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

    public EditorMenu getLastOpenedMenu() {
        return lastOpenedMenu;
    }

    public void setLastOpenedMenu(EditorMenu lastOpenedMenu) {
        this.lastOpenedMenu = lastOpenedMenu;
    }

    public int getKeyframePage() {
        return keyframePage;
    }

    public void setKeyframePage(int keyframePage) {
        this.keyframePage = Math.max(0, keyframePage);
    }
}
