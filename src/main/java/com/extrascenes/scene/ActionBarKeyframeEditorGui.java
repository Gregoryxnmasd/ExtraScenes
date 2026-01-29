package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ActionBarKeyframeEditorGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ActionBarKeyframeEditorGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        ActionBarKeyframe keyframe = editorEngine.getSelectedActionBarKeyframe(session);
        int tick = keyframe == null ? session.getCurrentTick() : keyframe.getTimeTicks();
        Inventory inventory = GuiUtils.createInventory(27,
                session.getSceneName() + " • Tick " + tick + " • Actionbar");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.PAPER, "Actionbar @ " + tick + "t",
                List.of("Edit text + duration.")));

        inventory.setItem(10, GuiUtils.makeItem(Material.WRITABLE_BOOK, "Set Text",
                List.of("Chat input.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.CLOCK, "Duration",
                List.of("Ticks: " + (keyframe == null ? 0 : keyframe.getDurationTicks()))));
        inventory.setItem(14, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Delete Actionbar",
                List.of("Requires confirmation.")));

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
        ActionBarKeyframe keyframe = editorEngine.getSelectedActionBarKeyframe(session);
        if (keyframe == null) {
            editorEngine.openTickActionMenu(player, session, false);
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
        if (slot == 10) {
            player.closeInventory();
            editorEngine.getInputManager().beginActionBarTextInput(player, session.getScene(), session,
                    GuiType.ACTIONBAR_EDITOR);
            return;
        }
        if (slot == 12) {
            player.closeInventory();
            editorEngine.getInputManager().beginActionBarDurationInput(player, session.getScene(), session,
                    GuiType.ACTIONBAR_EDITOR);
            return;
        }
        if (slot == 14) {
            editorEngine.openConfirm(player, session, ConfirmAction.DELETE_KEYFRAME, SceneTrackType.ACTIONBAR,
                    keyframe.getId());
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
