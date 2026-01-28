package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CommandKeyframeEditorGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public CommandKeyframeEditorGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(54, "Command Keyframe");
        GuiUtils.fillInventory(inventory);

        CommandKeyframe keyframe = editorEngine.getSelectedCommandKeyframe(session);
        String timeLabel = keyframe == null ? "Unknown" : keyframe.getTimeTicks() + "t";
        inventory.setItem(4, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Editing Keyframe @ " + timeLabel,
                List.of("Command keyframe editor.")));

        inventory.setItem(9, GuiUtils.makeItem(Material.CLOCK, "Change Time",
                List.of("Set keyframe time via chat.")));
        inventory.setItem(10, GuiUtils.makeItem(Material.LAVA_BUCKET, "Delete Keyframe",
                List.of("Requires confirmation.")));
        inventory.setItem(11, GuiUtils.makeItem(Material.PAPER, "Duplicate Keyframe",
                List.of("Create a copy of this keyframe.")));

        if (keyframe != null) {
            inventory.setItem(20, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Add Command",
                    List.of("Chat input.")));
            inventory.setItem(22, GuiUtils.makeItem(Material.BARRIER, "List/Remove Commands",
                    List.of("Commands: " + keyframe.getCommands().size(), "Click to clear.")));
            inventory.setItem(24, GuiUtils.makeItem(Material.LEVER, "Executor: " + keyframe.getExecutorMode(),
                    List.of("Toggle executor.")));
            inventory.setItem(31, GuiUtils.makeItem(Material.PAPER, "Allow Global: " + keyframe.isAllowGlobal(),
                    List.of("Toggle global commands.")));
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
        CommandKeyframe keyframe = editorEngine.getSelectedCommandKeyframe(session);
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
                    session.getSelectedTrack(), keyframe.getId(), GuiType.COMMAND_EDITOR);
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
            player.closeInventory();
            editorEngine.getInputManager().beginCommandAppendInput(player, session.getScene(), session, keyframe,
                    GuiType.COMMAND_EDITOR);
            return;
        }
        if (slot == 22) {
            keyframe.setCommands(List.of());
            refresh(session);
            return;
        }
        if (slot == 24) {
            keyframe.setExecutorMode(keyframe.getExecutorMode() == CommandKeyframe.ExecutorMode.PLAYER
                    ? CommandKeyframe.ExecutorMode.CONSOLE
                    : CommandKeyframe.ExecutorMode.PLAYER);
            refresh(session);
            return;
        }
        if (slot == 31) {
            keyframe.setAllowGlobal(!keyframe.isAllowGlobal());
            refresh(session);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
