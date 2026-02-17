package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CameraKeyframeEditorGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public CameraKeyframeEditorGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        CameraKeyframe keyframe = editorEngine.getSelectedCameraKeyframe(session);
        int tick = keyframe == null ? session.getCurrentTick() : keyframe.getTimeTicks();
        Inventory inventory = GuiUtils.createInventory(54,
                session.getSceneName() + " • Tick " + tick + " • Camera");
        GuiUtils.fillInventory(inventory);
        String timeLabel = keyframe == null ? "Unknown" : keyframe.getTimeTicks() + "t";
        inventory.setItem(4, GuiUtils.makeItem(Material.SPYGLASS, "Editing Keyframe @ " + timeLabel,
                List.of("Camera keyframe editor.")));

        inventory.setItem(9, GuiUtils.makeItem(Material.CLOCK, "Change Time",
                List.of("Set keyframe time via chat.")));
        inventory.setItem(10, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Delete Keyframe",
                List.of("Requires confirmation.")));
        inventory.setItem(11, GuiUtils.makeItem(Material.PAPER, "Duplicate Keyframe",
                List.of("Create a copy of this keyframe.")));

        if (keyframe != null) {
            inventory.setItem(20, GuiUtils.makeItem(Material.ENDER_PEARL, "Set to Current View",
                    List.of("Capture player transform.")));
            inventory.setItem(22, GuiUtils.makeItem(Material.REPEATER, "Smoothing: " + keyframe.getSmoothingMode(),
                    List.of("Click to cycle.")));
            inventory.setItem(23, GuiUtils.makeItem(Material.ENDER_EYE,
                    "Movimiento libre: " + (keyframe.isAllowPlayerLook() ? "ON" : "OFF"),
                    List.of("ON: el jugador controla el giro de cámara.", "OFF: cámara bloqueada.")));

            inventory.setItem(29, GuiUtils.makeItem(Material.ARROW, "Adjust X ±0.5",
                    List.of("Left: -0.5", "Right: +0.5")));
            inventory.setItem(30, GuiUtils.makeItem(Material.ARROW, "Adjust Y ±0.5",
                    List.of("Left: -0.5", "Right: +0.5")));
            inventory.setItem(31, GuiUtils.makeItem(Material.ARROW, "Adjust Z ±0.5",
                    List.of("Left: -0.5", "Right: +0.5")));
            inventory.setItem(32, GuiUtils.makeItem(Material.ARROW, "Adjust Yaw ±5",
                    List.of("Left: -5", "Right: +5")));
            inventory.setItem(33, GuiUtils.makeItem(Material.ARROW, "Adjust Pitch ±5",
                    List.of("Left: -5", "Right: +5")));
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to keyframe list.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        CameraKeyframe keyframe = editorEngine.getSelectedCameraKeyframe(session);
        if (keyframe == null) {
            editorEngine.openKeyframeList(player, session, false);
            return;
        }

        int slot = ctx.getSlot();
        if (slot == 45) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 9) {
            player.closeInventory();
            editorEngine.getInputManager().beginKeyframeTimeInput(player, session.getScene(), session,
                    session.getSelectedTrack(), keyframe.getId(), GuiType.CAMERA_EDITOR);
            return;
        }
        if (slot == 10) {
            editorEngine.openConfirm(player, session, ConfirmAction.DELETE_KEYFRAME, session.getSelectedTrack(),
                    keyframe.getId());
            return;
        }
        if (slot == 11) {
            editorEngine.duplicateKeyframe(session, keyframe);
            refresh(session);
            return;
        }
        if (slot == 20) {
            keyframe.setTransform(Transform.fromLocation(player.getEyeLocation()));
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 22) {
            keyframe.setSmoothingMode(keyframe.getSmoothingMode().next());
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 23) {
            keyframe.setAllowPlayerLook(!keyframe.isAllowPlayerLook());
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 29) {
            adjustTransform(keyframe.getTransform(), ctx.isRightClick() ? 0.5 : -0.5, 0, 0, 0, 0);
        } else if (slot == 30) {
            adjustTransform(keyframe.getTransform(), 0, ctx.isRightClick() ? 0.5 : -0.5, 0, 0, 0);
        } else if (slot == 31) {
            adjustTransform(keyframe.getTransform(), 0, 0, ctx.isRightClick() ? 0.5 : -0.5, 0, 0);
        } else if (slot == 32) {
            adjustTransform(keyframe.getTransform(), 0, 0, 0, ctx.isRightClick() ? 5 : -5, 0);
        } else if (slot == 33) {
            adjustTransform(keyframe.getTransform(), 0, 0, 0, 0, ctx.isRightClick() ? 5 : -5);
        } else {
            return;
        }
        editorEngine.markDirty(session.getScene());
        refresh(session);
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

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
