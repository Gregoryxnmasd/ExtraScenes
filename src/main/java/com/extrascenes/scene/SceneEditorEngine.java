package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SceneEditorEngine {
    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final EditorSessionManager editorSessionManager;
    private final EditorInputManager inputManager;
    private final Map<GuiType, EditorGui> guis = new EnumMap<>(GuiType.class);

    public SceneEditorEngine(ExtraScenesPlugin plugin, SceneManager sceneManager, EditorSessionManager editorSessionManager) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.editorSessionManager = editorSessionManager;
        this.inputManager = new EditorInputManager(plugin, sceneManager, this);
        registerGuis();
    }

    private void registerGuis() {
        guis.put(GuiType.SCENE_DASHBOARD, new SceneDashboardGui(this));
        guis.put(GuiType.TRACK_SELECT, new TrackSelectGui(this));
        guis.put(GuiType.KEYFRAME_LIST, new KeyframeListGui(this));
        guis.put(GuiType.ADD_KEYFRAME, new AddKeyframeGui(this));
        guis.put(GuiType.CAMERA_EDITOR, new CameraKeyframeEditorGui(this));
        guis.put(GuiType.COMMAND_EDITOR, new CommandKeyframeEditorGui(this));
        guis.put(GuiType.MODEL_EDITOR, new ModelKeyframeEditorGui(this));
        guis.put(GuiType.SCENE_SETTINGS, new SceneSettingsGui(this));
        guis.put(GuiType.CONFIRM, new ConfirmGui(this));
    }

    public EditorInputManager getInputManager() {
        return inputManager;
    }

    public EditorSessionManager getEditorSessionManager() {
        return editorSessionManager;
    }

    public void openEditor(Player player, Scene scene) {
        EditorSession session = editorSessionManager.getSession(player.getUniqueId());
        if (session == null) {
            session = editorSessionManager.createSession(player.getUniqueId(), scene);
        }
        session.clearHistory();
        openDashboard(player, session, false);
    }

    public void openDashboard(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.SCENE_DASHBOARD, pushHistory);
    }

    public void openTrackSelect(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.TRACK_SELECT, pushHistory);
    }

    public void openKeyframeList(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.KEYFRAME_LIST, pushHistory);
    }

    public void openAddKeyframe(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ADD_KEYFRAME, pushHistory);
    }

    public void openCameraEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.CAMERA_EDITOR, pushHistory);
    }

    public void openCommandEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.COMMAND_EDITOR, pushHistory);
    }

    public void openModelEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.MODEL_EDITOR, pushHistory);
    }

    public void openSceneSettings(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.SCENE_SETTINGS, pushHistory);
    }

    public void openConfirm(Player player, EditorSession session, ConfirmAction action, SceneTrackType track, UUID keyframeId) {
        session.setConfirmAction(action);
        session.setConfirmTrack(track);
        session.setConfirmKeyframeId(keyframeId);
        openGui(player, session, GuiType.CONFIRM, true);
    }

    public void navigateBack(Player player, EditorSession session) {
        GuiType previous = session.popHistory();
        if (previous == null) {
            openDashboard(player, session, false);
            return;
        }
        openGui(player, session, previous, false);
    }

    public void closeEditor(Player player, EditorSession session) {
        player.closeInventory();
        editorSessionManager.removeSession(player.getUniqueId());
        session.clearHistory();
        inputManager.clearPrompt(player.getUniqueId());
    }

    public void handleClick(Player player, Scene scene, EditorSession session, int slot, boolean rightClick,
                            boolean shiftClick, org.bukkit.inventory.ItemStack clicked) {
        EditorGui gui = guis.get(session.getCurrentGui());
        if (gui == null) {
            return;
        }
        gui.handleClick(session, new ClickContext(slot, rightClick, shiftClick, clicked));
    }

    private void openGui(Player player, EditorSession session, GuiType guiType, boolean pushHistory) {
        if (pushHistory && session.getCurrentGui() != null) {
            session.pushHistory(session.getCurrentGui());
        }
        session.setCurrentGui(guiType);
        EditorGui gui = guis.get(guiType);
        if (gui == null) {
            return;
        }
        Inventory inventory = gui.build(session);
        player.openInventory(inventory);
    }

    public void openGuiByType(Player player, EditorSession session, GuiType guiType) {
        openGui(player, session, guiType, false);
    }

    public void saveScene(Player player, EditorSession session) {
        try {
            sceneManager.saveScene(session.getScene());
            session.setLastSavedAt(System.currentTimeMillis());
            player.sendMessage(ChatColor.GREEN + "Scene saved.");
        } catch (IOException ex) {
            player.sendMessage(ChatColor.RED + "Failed to save scene.");
        }
    }

    public void togglePreview(Player player, EditorSession session) {
        if (session.isPreviewPlaying()) {
            plugin.getSessionManager().stopScene(player, "preview_stop");
            session.setPreviewPlaying(false);
        } else {
            plugin.getSessionManager().startScene(player, session.getScene(), true);
            session.setPreviewPlaying(true);
        }
    }

    public void playScene(Player player, EditorSession session) {
        plugin.getSessionManager().startScene(player, session.getScene(), false);
    }

    public CameraKeyframe getSelectedCameraKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.CAMERA, CameraKeyframe.class);
    }

    public CommandKeyframe getSelectedCommandKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.COMMAND, CommandKeyframe.class);
    }

    public ModelKeyframe getSelectedModelKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.MODEL, ModelKeyframe.class);
    }

    private <T extends Keyframe> T getSelectedKeyframe(EditorSession session, SceneTrackType type, Class<T> clazz) {
        if (session.getSelectedKeyframeId() == null) {
            return null;
        }
        Track<? extends Keyframe> track = session.getScene().getTrack(type);
        if (track == null) {
            return null;
        }
        Keyframe keyframe = track.getKeyframe(session.getSelectedKeyframeId());
        if (clazz.isInstance(keyframe)) {
            return clazz.cast(keyframe);
        }
        return null;
    }

    public void duplicateKeyframe(EditorSession session, Keyframe keyframe) {
        if (keyframe == null) {
            return;
        }
        if (keyframe instanceof CameraKeyframe camera) {
            Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
            if (track == null) {
                return;
            }
            CameraKeyframe copy = new CameraKeyframe(null, camera.getTimeTicks(),
                    cloneTransform(camera.getTransform()),
                    camera.getSmoothingMode(), camera.isInstant(),
                    cloneLookAt(camera.getLookAt()));
            track.addKeyframe(copy);
        } else if (keyframe instanceof CommandKeyframe command) {
            Track<CommandKeyframe> track = session.getScene().getTrack(SceneTrackType.COMMAND);
            if (track == null) {
                return;
            }
            CommandKeyframe copy = new CommandKeyframe(null, command.getTimeTicks(),
                    List.copyOf(command.getCommands()), command.getExecutorMode(), command.isAllowGlobal());
            track.addKeyframe(copy);
        } else if (keyframe instanceof ModelKeyframe model) {
            Track<ModelKeyframe> track = session.getScene().getTrack(SceneTrackType.MODEL);
            if (track == null) {
                return;
            }
            ModelKeyframe copy = new ModelKeyframe(null, model.getTimeTicks(), model.getAction());
            copy.setModelId(model.getModelId());
            copy.setEntityRef(model.getEntityRef());
            copy.setAnimationId(model.getAnimationId());
            copy.setLoop(model.isLoop());
            copy.setSpeed(model.getSpeed());
            copy.setSpawnTransform(cloneTransform(model.getSpawnTransform()));
            track.addKeyframe(copy);
        }
    }

    public void addCameraKeyframe(Player player, EditorSession session, boolean instant) {
        SmoothingMode smoothing = session.getScene().getDefaultSmoothing();
        CameraKeyframe keyframe = new CameraKeyframe(null, session.getCursorTimeTicks(),
                Transform.fromLocation(player.getLocation()),
                smoothing, instant, LookAtTarget.none());
        Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
        track.addKeyframe(keyframe);
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addLookAtKeyframe(Player player, EditorSession session) {
        SmoothingMode smoothing = session.getScene().getDefaultSmoothing();
        CameraKeyframe keyframe = new CameraKeyframe(null, session.getCursorTimeTicks(),
                Transform.fromLocation(player.getLocation()),
                smoothing, false,
                new LookAtTarget(LookAtTarget.Mode.POSITION, Transform.fromLocation(player.getLocation()), null));
        Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
        track.addKeyframe(keyframe);
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addSoundKeyframe(Player player, EditorSession session) {
        SoundKeyframe keyframe = new SoundKeyframe(null, session.getCursorTimeTicks(), "entity.player.levelup",
                1.0f, 1.0f, Transform.fromLocation(player.getLocation()));
        Track<SoundKeyframe> track = session.getScene().getTrack(SceneTrackType.SOUND);
        track.addKeyframe(keyframe);
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addParticleKeyframe(Player player, EditorSession session) {
        ParticleKeyframe keyframe = new ParticleKeyframe(null, session.getCursorTimeTicks(), "happy_villager",
                Transform.fromLocation(player.getLocation()));
        Track<ParticleKeyframe> track = session.getScene().getTrack(SceneTrackType.PARTICLE);
        track.addKeyframe(keyframe);
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addBlockKeyframe(Player player, EditorSession session) {
        BlockIllusionKeyframe keyframe = new BlockIllusionKeyframe(null, session.getCursorTimeTicks(),
                Material.GLASS, Transform.fromLocation(player.getLocation()));
        Track<BlockIllusionKeyframe> track = session.getScene().getTrack(SceneTrackType.BLOCK_ILLUSION);
        track.addKeyframe(keyframe);
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void handleConfirmAction(Player player, EditorSession session) {
        ConfirmAction action = session.getConfirmAction();
        if (action == null) {
            navigateBack(player, session);
            return;
        }
        if (action == ConfirmAction.DELETE_KEYFRAME) {
            Track<? extends Keyframe> track = session.getScene().getTrack(session.getConfirmTrack());
            if (track != null && session.getConfirmKeyframeId() != null) {
                track.removeKeyframe(session.getConfirmKeyframeId());
                if (session.getConfirmKeyframeId().equals(session.getSelectedKeyframeId())) {
                    session.setSelectedKeyframeId(null);
                }
            }
            clearConfirm(session);
            session.setKeyframePage(0);
            openKeyframeList(player, session, false);
            return;
        }
        if (action == ConfirmAction.CLEAR_TRACK) {
            Track<? extends Keyframe> track = session.getScene().getTrack(session.getConfirmTrack());
            if (track != null) {
                track.clear();
            }
            clearConfirm(session);
            session.setSelectedKeyframeId(null);
            session.setKeyframePage(0);
            openKeyframeList(player, session, false);
            return;
        }
        if (action == ConfirmAction.DELETE_SCENE) {
            sceneManager.deleteScene(session.getScene().getName());
            clearConfirm(session);
            closeEditor(player, session);
        }
    }

    private void clearConfirm(EditorSession session) {
        session.setConfirmAction(null);
        session.setConfirmTrack(null);
        session.setConfirmKeyframeId(null);
    }

    private Transform cloneTransform(Transform transform) {
        if (transform == null) {
            return null;
        }
        return new Transform(transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch());
    }

    private LookAtTarget cloneLookAt(LookAtTarget lookAt) {
        if (lookAt == null) {
            return LookAtTarget.none();
        }
        Transform position = cloneTransform(lookAt.getPosition());
        return new LookAtTarget(lookAt.getMode(), position, lookAt.getEntityId());
    }
}
