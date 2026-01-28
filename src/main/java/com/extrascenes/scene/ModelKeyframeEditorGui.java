package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ModelKeyframeEditorGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ModelKeyframeEditorGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(54, "Model Keyframe");
        GuiUtils.fillInventory(inventory);

        ModelKeyframe keyframe = editorEngine.getSelectedModelKeyframe(session);
        String timeLabel = keyframe == null ? "Unknown" : keyframe.getTimeTicks() + "t";
        inventory.setItem(4, GuiUtils.makeItem(Material.NAME_TAG, "Editing Keyframe @ " + timeLabel,
                List.of("Model keyframe editor.")));

        inventory.setItem(9, GuiUtils.makeItem(Material.CLOCK, "Change Time",
                List.of("Set keyframe time via chat.")));
        inventory.setItem(10, GuiUtils.makeItem(Material.LAVA_BUCKET, "Delete Keyframe",
                List.of("Requires confirmation.")));
        inventory.setItem(11, GuiUtils.makeItem(Material.PAPER, "Duplicate Keyframe",
                List.of("Create a copy of this keyframe.")));

        if (keyframe != null) {
            inventory.setItem(20, GuiUtils.makeItem(Material.ARMOR_STAND, "Action: " + keyframe.getAction(),
                    List.of("Cycle action.")));
            inventory.setItem(22, GuiUtils.makeItem(Material.NAME_TAG,
                    "ModelId / EntityRef",
                    List.of("Left: edit modelId", "Right: edit entityRef")));
            inventory.setItem(24, GuiUtils.makeItem(Material.WRITABLE_BOOK,
                    "Animation / Loop / Speed",
                    List.of("Left: animationId", "Right: toggle loop", "Shift: speed input")));
            inventory.setItem(31, GuiUtils.makeItem(Material.ENDER_PEARL, "Capture Spawn Transform",
                    List.of("Only for SPAWN action.")));
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to keyframe list.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(53, GuiUtils.makeItem(Material.WRITABLE_BOOK, "Apply",
                List.of("Save changes to keyframe.")));

        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        ModelKeyframe keyframe = editorEngine.getSelectedModelKeyframe(session);
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
        if (slot == 53) {
            player.sendMessage(GuiUtils.TITLE_PREFIX + "Keyframe saved.");
            refresh(session);
            return;
        }
        if (slot == 9) {
            player.closeInventory();
            editorEngine.getInputManager().beginKeyframeTimeInput(player, session.getScene(), session,
                    session.getSelectedTrack(), keyframe.getId(), GuiType.MODEL_EDITOR);
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
            keyframe.setAction(nextAction(keyframe.getAction()));
            refresh(session);
            return;
        }
        if (slot == 22) {
            if (ctx.isRightClick()) {
                player.closeInventory();
                editorEngine.getInputManager().beginModelFieldInput(player, session.getScene(), session, keyframe,
                        EditorInputManager.ModelField.ENTITY_REF, GuiType.MODEL_EDITOR);
            } else {
                player.closeInventory();
                editorEngine.getInputManager().beginModelFieldInput(player, session.getScene(), session, keyframe,
                        EditorInputManager.ModelField.MODEL_ID, GuiType.MODEL_EDITOR);
            }
            return;
        }
        if (slot == 24) {
            if (ctx.isShiftClick()) {
                player.closeInventory();
                editorEngine.getInputManager().beginModelSpeedInput(player, session.getScene(), session, keyframe,
                        GuiType.MODEL_EDITOR);
            } else if (ctx.isRightClick()) {
                keyframe.setLoop(!keyframe.isLoop());
                refresh(session);
            } else {
                player.closeInventory();
                editorEngine.getInputManager().beginModelFieldInput(player, session.getScene(), session, keyframe,
                        EditorInputManager.ModelField.ANIMATION_ID, GuiType.MODEL_EDITOR);
            }
            return;
        }
        if (slot == 31) {
            if (keyframe.getAction() == ModelKeyframe.Action.SPAWN) {
                keyframe.setSpawnTransform(Transform.fromLocation(player.getLocation()));
                refresh(session);
            }
        }
    }

    private ModelKeyframe.Action nextAction(ModelKeyframe.Action action) {
        ModelKeyframe.Action[] values = ModelKeyframe.Action.values();
        int index = action.ordinal();
        return values[(index + 1) % values.length];
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
