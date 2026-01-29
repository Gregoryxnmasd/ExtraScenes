package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TrackSelectGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public TrackSelectGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(27, session.getSceneName() + " • Tracks");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.NAME_TAG, "Select Track",
                List.of("Choose what you're editing.")));

        inventory.setItem(10, GuiUtils.makeItem(Material.ARMOR_STAND, "Camera Track",
                List.of("Edit camera keyframes.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.PLAYER_HEAD, "Model Track",
                List.of("Edit model keyframes.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Command Track",
                List.of("Edit command keyframes.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.BLAZE_POWDER, "Particle Track",
                List.of("Edit particle keyframes.")));
        inventory.setItem(19, GuiUtils.makeItem(Material.NOTE_BLOCK, "Sound Track",
                List.of("Edit sound keyframes.")));
        inventory.setItem(21, GuiUtils.makeItem(Material.BARRIER, "Block Track",
                List.of("Edit block illusion keyframes.")));

        inventory.setItem(18, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
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
        if (slot == 18) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 10) {
            openTrack(player, session, SceneTrackType.CAMERA);
        } else if (slot == 12) {
            openTrack(player, session, SceneTrackType.MODEL);
        } else if (slot == 14) {
            openTrack(player, session, SceneTrackType.COMMAND);
        } else if (slot == 16) {
            openTrack(player, session, SceneTrackType.PARTICLE);
        } else if (slot == 19) {
            openTrack(player, session, SceneTrackType.SOUND);
        } else if (slot == 21) {
            openTrack(player, session, SceneTrackType.BLOCK_ILLUSION);
        }
    }

    private void openTrack(Player player, EditorSession session, SceneTrackType type) {
        session.setSelectedTrack(type);
        session.setKeyframePage(0);
        editorEngine.openKeyframeList(player, session, true);
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
