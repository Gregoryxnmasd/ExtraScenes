package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CameraOptionsGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public CameraOptionsGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        int tick = session.getCurrentTick();
        Inventory inventory = GuiUtils.createInventory(27, session.getSceneName() + " • Tick " + tick + " • Camera");
        GuiUtils.fillInventory(inventory);

        CameraKeyframe keyframe = editorEngine.getOrCreateCameraKeyframe(session, tick);
        if (keyframe.getTransform() == null) {
            keyframe.setTransform(Transform.fromLocation(Bukkit.getPlayer(session.getPlayerUuid()).getLocation()));
            editorEngine.markDirty(session.getScene());
        }
        inventory.setItem(4, GuiUtils.makeItem(Material.COMPASS, "Camera Options",
                List.of("Tick " + tick, "Mode: " + (keyframe.isInstant() ? "INSTANT_CUT" : "FOLLOW_PATH"),
                        "LookAt: " + (keyframe.getLookAt().getMode() == LookAtTarget.Mode.NONE ? "OFF" : keyframe.getLookAt().getMode()))));

        inventory.setItem(10, GuiUtils.makeItem(Material.ENDER_EYE, "Toggle Instant Cut",
                List.of("Instant: " + keyframe.isInstant())));
        inventory.setItem(12, GuiUtils.makeItem(Material.TARGET, "Set LookAt Here",
                List.of("Aim at your current location.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Clear LookAt",
                List.of("Disable look-at.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.REPEATER, "Smoothing: " + keyframe.getSmoothingMode(),
                List.of("Cycle easing mode.")));

        inventory.setItem(18, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to tick menu.")));
        inventory.setItem(22, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 18) {
            editorEngine.openTickActionMenu(player, session, true);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }
        CameraKeyframe keyframe = editorEngine.getOrCreateCameraKeyframe(session, session.getCurrentTick());
        if (slot == 10) {
            keyframe.setInstant(!keyframe.isInstant());
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 12) {
            keyframe.setLookAt(new LookAtTarget(LookAtTarget.Mode.POSITION, Transform.fromLocation(player.getLocation()), null));
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 14) {
            keyframe.setLookAt(LookAtTarget.none());
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 16) {
            keyframe.setSmoothingMode(keyframe.getSmoothingMode().next());
            editorEngine.markDirty(session.getScene());
            refresh(session);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
