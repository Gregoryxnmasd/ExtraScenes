package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SceneEditorEngine {
    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final EditorSessionManager editorSessionManager;
    private final SceneEditorGUI gui;
    private final EditorInputManager inputManager;

    public SceneEditorEngine(ExtraScenesPlugin plugin, SceneManager sceneManager, EditorSessionManager editorSessionManager) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.editorSessionManager = editorSessionManager;
        this.gui = new SceneEditorGUI();
        this.inputManager = new EditorInputManager(plugin, sceneManager, this);
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
        openMainMenu(player, scene, session);
    }

    public void openMainMenu(Player player, Scene scene, EditorSession session) {
        session.setLastOpenedMenu(EditorMenu.MAIN);
        Inventory inventory = gui.buildMainMenu(scene, session);
        player.openInventory(inventory);
    }

    public void openAddMenu(Player player, Scene scene, EditorSession session) {
        session.setLastOpenedMenu(EditorMenu.ADD_KEYFRAME);
        player.openInventory(gui.buildAddKeyframeMenu(scene, session));
    }

    public void openEditMenu(Player player, Scene scene, EditorSession session, Keyframe keyframe) {
        session.setLastOpenedMenu(EditorMenu.KEYFRAME_EDIT);
        player.openInventory(gui.buildKeyframeEditMenu(scene, session, keyframe));
    }

    public void handleClick(Player player, Scene scene, EditorSession session, int slot, boolean rightClick,
                            boolean shiftClick, ItemStack clicked) {
        String title = player.getOpenInventory().getTitle();
        if (title.startsWith(SceneEditorGUI.ADD_TITLE_PREFIX)) {
            handleAddMenuClick(player, scene, session, slot);
            return;
        }
        if (title.startsWith(SceneEditorGUI.EDIT_TITLE_PREFIX)) {
            handleEditMenuClick(player, scene, session, slot);
            return;
        }

        if (slot == 0) {
            togglePreview(player, scene, session);
            return;
        }
        if (slot == 1) {
            stopPreview(player, session);
            return;
        }
        if (slot == 2) {
            saveScene(player, scene);
            return;
        }
        if (slot == 6) {
            session.setKeyframePage(Math.max(0, session.getKeyframePage() - 1));
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 7) {
            session.setKeyframePage(session.getKeyframePage() + 1);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 8) {
            player.closeInventory();
            return;
        }
        if (slot == 9) {
            session.setSelectedTrack(SceneTrackType.CAMERA);
            session.setKeyframePage(0);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 18) {
            session.setSelectedTrack(SceneTrackType.COMMAND);
            session.setKeyframePage(0);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 27) {
            session.setSelectedTrack(SceneTrackType.MODEL);
            session.setKeyframePage(0);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 36) {
            session.setSelectedTrack(SceneTrackType.PARTICLE);
            session.setKeyframePage(0);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 45) {
            session.setSelectedTrack(SceneTrackType.SOUND);
            session.setKeyframePage(0);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 46) {
            session.setSelectedTrack(SceneTrackType.BLOCK_ILLUSION);
            session.setKeyframePage(0);
            openMainMenu(player, scene, session);
            return;
        }

        if (slot == 47) {
            session.setCursorTimeTicks(session.getCursorTimeTicks() - 20);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 48) {
            session.setCursorTimeTicks(session.getCursorTimeTicks() - 1);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 49) {
            openAddMenu(player, scene, session);
            return;
        }
        if (slot == 50) {
            session.setCursorTimeTicks(session.getCursorTimeTicks() + 1);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 51) {
            session.setCursorTimeTicks(session.getCursorTimeTicks() + 20);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 52) {
            player.closeInventory();
            inputManager.clearPrompt(player.getUniqueId());
            inputManager.beginCursorInput(player, scene, session);
            return;
        }
        if (slot == 53) {
            UUID selectedId = session.getSelectedKeyframeId();
            Track<? extends Keyframe> track = scene.getTrack(session.getSelectedTrack());
            if (selectedId != null && track != null) {
                Keyframe keyframe = track.getKeyframe(selectedId);
                if (keyframe != null) {
                    session.setCursorTimeTicks(keyframe.getTimeTicks());
                }
            }
            openMainMenu(player, scene, session);
            return;
        }

        handleKeyframeListClick(player, scene, session, slot, rightClick, shiftClick, clicked);
    }

    private void handleAddMenuClick(Player player, Scene scene, EditorSession session, int slot) {
        if (slot == 22) {
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 10) {
            SmoothingMode smoothing;
            try {
                smoothing = SmoothingMode.valueOf(plugin.getConfig().getString("camera.defaultSmoothing", "LINEAR"));
            } catch (IllegalArgumentException ex) {
                smoothing = SmoothingMode.LINEAR;
            }
            CameraKeyframe keyframe = new CameraKeyframe(null, session.getCursorTimeTicks(),
                    Transform.fromLocation(player.getLocation()),
                    smoothing,
                    false, LookAtTarget.none());
            scene.getTrack(SceneTrackType.CAMERA).addKeyframe(keyframe);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 12) {
            player.closeInventory();
            inputManager.beginCommandInput(player, scene, session);
            return;
        }
        if (slot == 14) {
            player.closeInventory();
            inputManager.beginModelInput(player, scene, session);
            return;
        }
        if (slot == 16) {
            SoundKeyframe keyframe = new SoundKeyframe(null, session.getCursorTimeTicks(), "entity.player.levelup",
                    1.0f, 1.0f, Transform.fromLocation(player.getLocation()));
            scene.getTrack(SceneTrackType.SOUND).addKeyframe(keyframe);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 18) {
            ParticleKeyframe keyframe = new ParticleKeyframe(null, session.getCursorTimeTicks(), "happy_villager",
                    Transform.fromLocation(player.getLocation()));
            scene.getTrack(SceneTrackType.PARTICLE).addKeyframe(keyframe);
            openMainMenu(player, scene, session);
            return;
        }
        if (slot == 20) {
            BlockIllusionKeyframe keyframe = new BlockIllusionKeyframe(null, session.getCursorTimeTicks(),
                    Material.GLASS, Transform.fromLocation(player.getLocation()));
            scene.getTrack(SceneTrackType.BLOCK_ILLUSION).addKeyframe(keyframe);
            openMainMenu(player, scene, session);
        }
    }

    private void handleEditMenuClick(Player player, Scene scene, EditorSession session, int slot) {
        UUID selectedId = session.getSelectedKeyframeId();
        if (selectedId == null) {
            openMainMenu(player, scene, session);
            return;
        }
        Track<? extends Keyframe> track = scene.getTrack(session.getSelectedTrack());
        if (track == null) {
            openMainMenu(player, scene, session);
            return;
        }
        Keyframe keyframe = track.getKeyframe(selectedId);
        if (keyframe == null) {
            openMainMenu(player, scene, session);
            return;
        }

        if (slot == 22) {
            openMainMenu(player, scene, session);
            return;
        }

        if (keyframe instanceof CameraKeyframe camera) {
            if (slot == 10) {
                camera.setTransform(Transform.fromLocation(player.getLocation()));
            } else if (slot == 11) {
                camera.setSmoothingMode(camera.getSmoothingMode().next());
            } else if (slot == 12) {
                camera.setInstant(!camera.isInstant());
            } else if (slot == 13) {
                LookAtTarget.Mode mode = camera.getLookAt().getMode();
                if (mode == LookAtTarget.Mode.NONE) {
                    camera.setLookAt(new LookAtTarget(LookAtTarget.Mode.POSITION, Transform.fromLocation(player.getLocation()), null));
                } else {
                    camera.setLookAt(LookAtTarget.none());
                }
            } else if (slot == 15) {
                adjustTransform(camera.getTransform(), -0.5, 0, 0, 0, 0);
            } else if (slot == 16) {
                adjustTransform(camera.getTransform(), 0.5, 0, 0, 0, 0);
            } else if (slot == 17) {
                adjustTransform(camera.getTransform(), 0, -0.5, 0, 0, 0);
            } else if (slot == 18) {
                adjustTransform(camera.getTransform(), 0, 0.5, 0, 0, 0);
            } else if (slot == 19) {
                adjustTransform(camera.getTransform(), 0, 0, -0.5, 0, 0);
            } else if (slot == 20) {
                adjustTransform(camera.getTransform(), 0, 0, 0.5, 0, 0);
            } else if (slot == 21) {
                adjustTransform(camera.getTransform(), 0, 0, 0, 5, 5);
            }
        } else if (keyframe instanceof CommandKeyframe command) {
            if (slot == 10) {
                player.closeInventory();
                inputManager.beginCommandAppendInput(player, scene, session, command);
                return;
            }
            if (slot == 11) {
                command.setCommands(List.of());
            }
            if (slot == 12) {
                command.setExecutorMode(command.getExecutorMode() == CommandKeyframe.ExecutorMode.PLAYER
                        ? CommandKeyframe.ExecutorMode.CONSOLE
                        : CommandKeyframe.ExecutorMode.PLAYER);
            }
            if (slot == 13) {
                command.setAllowGlobal(!command.isAllowGlobal());
            }
        } else if (keyframe instanceof ModelKeyframe model) {
            if (slot == 10) {
                model.setAction(nextModelAction(model.getAction()));
            } else if (slot == 11) {
                player.closeInventory();
                inputManager.clearPrompt(player.getUniqueId());
                inputManager.beginModelFieldInput(player, scene, session, model, EditorInputManager.ModelField.ENTITY_REF);
                return;
            } else if (slot == 12) {
                player.closeInventory();
                inputManager.clearPrompt(player.getUniqueId());
                inputManager.beginModelFieldInput(player, scene, session, model, EditorInputManager.ModelField.MODEL_ID);
                return;
            } else if (slot == 13) {
                player.closeInventory();
                inputManager.clearPrompt(player.getUniqueId());
                inputManager.beginModelFieldInput(player, scene, session, model, EditorInputManager.ModelField.ANIMATION_ID);
                return;
            }
        }

        openEditMenu(player, scene, session, keyframe);
    }

    private void handleKeyframeListClick(Player player, Scene scene, EditorSession session, int slot,
                                         boolean rightClick, boolean shiftClick, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        List<Integer> slots = gui.getKeyframeSlots();
        int slotIndex = slots.indexOf(slot);
        if (slotIndex < 0) {
            return;
        }
        Track<? extends Keyframe> track = scene.getTrack(session.getSelectedTrack());
        if (track == null) {
            return;
        }
        int startIndex = session.getKeyframePage() * slots.size();
        int keyframeIndex = startIndex + slotIndex;
        if (keyframeIndex >= track.getKeyframes().size()) {
            return;
        }
        Keyframe keyframe = track.getKeyframes().get(keyframeIndex);

        if (shiftClick) {
            int delta = rightClick ? 5 : -5;
            track.moveKeyframe(keyframe.getId(), keyframe.getTimeTicks() + delta);
            openMainMenu(player, scene, session);
            return;
        }
        session.setSelectedKeyframeId(keyframe.getId());
        if (rightClick) {
            openEditMenu(player, scene, session, keyframe);
        } else {
            openMainMenu(player, scene, session);
        }
    }

    private void saveScene(Player player, Scene scene) {
        try {
            sceneManager.saveScene(scene);
            player.sendMessage(ChatColor.GREEN + "Scene saved.");
        } catch (IOException ex) {
            player.sendMessage(ChatColor.RED + "Failed to save scene.");
        }
    }

    private void togglePreview(Player player, Scene scene, EditorSession session) {
        if (session.isPreviewPlaying()) {
            plugin.getSessionManager().stopScene(player, "preview_stop");
            session.setPreviewPlaying(false);
            openMainMenu(player, scene, session);
        } else {
            plugin.getSessionManager().startScene(player, scene, true);
            session.setPreviewPlaying(true);
        }
    }

    private void stopPreview(Player player, EditorSession session) {
        plugin.getSessionManager().stopScene(player, "preview_stop");
        session.setPreviewPlaying(false);
    }

    private void adjustTransform(Transform transform, double dx, double dy, double dz, float dyaw, float dpitch) {
        if (transform == null) {
            return;
        }
        transform.setX(transform.getX() + dx);
        transform.setY(transform.getY() + dy);
        transform.setZ(transform.getZ() + dz);
        transform.setYaw(transform.getYaw() + dyaw);
        transform.setPitch(transform.getPitch() + dpitch);
    }

    private ModelKeyframe.Action nextModelAction(ModelKeyframe.Action action) {
        ModelKeyframe.Action[] values = ModelKeyframe.Action.values();
        int index = action.ordinal();
        return values[(index + 1) % values.length];
    }
}
