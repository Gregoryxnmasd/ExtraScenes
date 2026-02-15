package com.extrascenes.scene;

import com.extrascenes.Text;

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
        ModelKeyframe keyframe = editorEngine.getSelectedModelKeyframe(session);
        int tick = keyframe == null ? session.getCurrentTick() : keyframe.getTimeTicks();
        Inventory inventory = GuiUtils.createInventory(54,
                session.getSceneName() + " • Tick " + tick + " • Models");
        GuiUtils.fillInventory(inventory);

        String timeLabel = keyframe == null ? "Unknown" : keyframe.getTimeTicks() + "t";
        inventory.setItem(4, GuiUtils.makeItem(Material.NAME_TAG, "Model Action @ " + timeLabel,
                List.of("Model keyframe editor.")));

        if (keyframe != null) {
            if (keyframe.getModelEntry() == null || keyframe.getModelEntry().isBlank()) {
                String defaultEntry = null;
                if (keyframe.getModelId() != null) {
                    SceneModelEntry entry = session.getScene().getModelEntry(keyframe.getModelId());
                    if (entry != null) {
                        defaultEntry = entry.getName();
                    }
                }
                if (defaultEntry == null) {
                    defaultEntry = editorEngine.nextModelEntry(session.getScene(), null);
                }
                if (defaultEntry != null) {
                    keyframe.setModelEntry(defaultEntry);
                    editorEngine.markDirty(session.getScene());
                }
            }
            inventory.setItem(20, GuiUtils.makeItem(Material.ARMOR_STAND, "Action: " + keyframe.getAction(),
                    List.of("Cycle action.")));
            inventory.setItem(22, GuiUtils.makeItem(Material.NAME_TAG,
                    "Model Entry",
                    List.of("Left: cycle entry", "Right: open library",
                            "Entry: " + GuiUtils.nullToPlaceholder(keyframe.getModelEntry()))));
            if (keyframe.getAction() == ModelKeyframe.Action.ANIM) {
                inventory.setItem(24, GuiUtils.makeItem(Material.WRITABLE_BOOK,
                        "Animation / Loop / Speed",
                        List.of("Left: animationId", "Right: toggle loop", "Shift: speed input")));
            } else if (keyframe.getAction() == ModelKeyframe.Action.STOP) {
                inventory.setItem(24, GuiUtils.makeItem(Material.WRITABLE_BOOK,
                        "Stop Animation",
                        List.of("Left: animationId (optional)")));
            }
            if (keyframe.getAction() == ModelKeyframe.Action.SPAWN) {
                inventory.setItem(31, GuiUtils.makeItem(Material.ENDER_PEARL, "Set Model Spawn Location",
                        List.of("Placement mode.")));
            }
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to previous menu.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));

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
        if (slot == 20) {
            keyframe.setAction(nextAction(keyframe.getAction()));
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 22) {
            if (ctx.isRightClick()) {
                editorEngine.openModelLibrary(player, session, true);
                return;
            }
            String nextEntry = editorEngine.nextModelEntry(session.getScene(), keyframe.getModelEntry());
            if (nextEntry == null) {
                Text.send(player, "&c" + "No model entries available.");
                return;
            }
            keyframe.setModelEntry(nextEntry);
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 24) {
            if (keyframe.getAction() == ModelKeyframe.Action.STOP) {
                if (!ctx.isRightClick() && !ctx.isShiftClick()) {
                    player.closeInventory();
                    editorEngine.getInputManager().beginModelFieldInput(player, session.getScene(), session, keyframe,
                            EditorInputManager.ModelField.ANIMATION_ID, GuiType.MODEL_EDITOR);
                }
                return;
            }
            if (keyframe.getAction() != ModelKeyframe.Action.ANIM) {
                return;
            }
            if (ctx.isShiftClick()) {
                player.closeInventory();
                editorEngine.getInputManager().beginModelSpeedInput(player, session.getScene(), session, keyframe,
                        GuiType.MODEL_EDITOR);
            } else if (ctx.isRightClick()) {
                keyframe.setLoop(!keyframe.isLoop());
                editorEngine.markDirty(session.getScene());
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
                editorEngine.armModelKeyframePlacement(player, session, keyframe.getId(), GuiType.MODEL_EDITOR);
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
