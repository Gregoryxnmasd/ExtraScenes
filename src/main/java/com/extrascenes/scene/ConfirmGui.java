package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ConfirmGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ConfirmGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(27, session.getSceneName() + " • Confirm");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Confirm Action",
                List.of(buildConfirmMessage(session))));
        inventory.setItem(13, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Confirm", List.of("Perform this action.")));
        inventory.setItem(15, GuiUtils.makeItem(Material.BARRIER, "Cancel", List.of("Return without changes.")));

        inventory.setItem(18, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to previous menu.")));
        inventory.setItem(22, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        int slot = ctx.getSlot();
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        if (slot == 18 || slot == 15) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 13) {
            editorEngine.handleConfirmAction(player, session);
        }
    }

    private String buildConfirmMessage(EditorSession session) {
        ConfirmAction action = session.getConfirmAction();
        if (action == null) {
            return "No action selected.";
        }
        return switch (action) {
            case DELETE_KEYFRAME -> "Delete selected keyframe?";
            case REMOVE_COMMAND -> "Remove selected command?";
            case DELETE_SCENE -> "Delete entire scene?";
            case CLEAR_TRACK -> "Clear all keyframes in track?";
            case CLEAR_TICK -> "Clear all actions at this tick?";
            case CLEAR_GROUP -> "Clear all actions in this group?";
            case CLEAR_ALL -> "Clear every tick in the scene?";
            case CLEAR_EFFECTS -> "Clear all effects at this tick?";
            case TRIM_DURATION -> "Trim scene duration by 9 ticks?";
            case DELETE_ACTOR -> "Delete selected actor?";
        };
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
