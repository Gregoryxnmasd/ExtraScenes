package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ModelTickListGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ModelTickListGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        int tick = session.getCurrentTick();
        Inventory inventory = GuiUtils.createInventory(54, session.getSceneName() + " • Tick " + tick + " • Models");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.ARMOR_STAND, "Models @ Tick " + tick,
                List.of("Click a model action to edit.")));

        List<ModelKeyframe> keyframes = TickUtils.getKeyframesAtTick(session.getScene().getTrack(SceneTrackType.MODEL), tick);
        int slot = 9;
        for (ModelKeyframe keyframe : keyframes) {
            inventory.setItem(slot++, GuiUtils.makeItem(Material.REDSTONE_BLOCK,
                    keyframe.getAction() + " • " + GuiUtils.nullToPlaceholder(keyframe.getEntityRef()),
                    List.of("Model: " + GuiUtils.nullToPlaceholder(keyframe.getModelId()),
                            "Anim: " + GuiUtils.nullToPlaceholder(keyframe.getAnimationId()),
                            "Left: edit", "Right: delete")));
            if (slot >= 45) {
                break;
            }
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ANVIL, "Add Model Action",
                List.of("Prompt via chat.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(53, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to tick menu.")));

        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 53) {
            editorEngine.openTickActionMenu(player, session, true);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 45) {
            session.setCursorTimeTicks(session.getCurrentTick());
            player.closeInventory();
            editorEngine.getInputManager().beginModelInput(player, session.getScene(), session, GuiType.MODEL_TICK_LIST);
            return;
        }
        if (slot >= 9 && slot < 45) {
            List<ModelKeyframe> keyframes = TickUtils.getKeyframesAtTick(session.getScene().getTrack(SceneTrackType.MODEL),
                    session.getCurrentTick());
            int index = slot - 9;
            if (index >= keyframes.size()) {
                return;
            }
            ModelKeyframe keyframe = keyframes.get(index);
            session.setSelectedKeyframeId(keyframe.getId());
            session.setSelectedTrack(SceneTrackType.MODEL);
            if (ctx.isRightClick()) {
                editorEngine.openConfirm(player, session, ConfirmAction.DELETE_KEYFRAME, SceneTrackType.MODEL,
                        keyframe.getId());
                return;
            }
            editorEngine.openModelEditor(player, session, true);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
