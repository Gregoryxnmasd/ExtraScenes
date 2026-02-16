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
        CommandKeyframe keyframe = editorEngine.getSelectedCommandKeyframe(session);
        String timeLabel = keyframe == null ? "Unknown" : keyframe.getTimeTicks() + "t";
        int tick = keyframe == null ? session.getCurrentTick() : keyframe.getTimeTicks();
        Inventory inventory = GuiUtils.createInventory(54,
                session.getSceneName() + " • Tick " + tick + " • Commands");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Editing Keyframe @ " + timeLabel,
                List.of("Command keyframe editor.")));

        inventory.setItem(9, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Delete Keyframe",
                List.of("Requires confirmation.")));
        inventory.setItem(10, GuiUtils.makeItem(Material.PAPER, "Duplicate Keyframe",
                List.of("Create a copy of this keyframe.")));

        if (keyframe != null) {
            inventory.setItem(20, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Add Commands",
                    List.of("Chat input (multi-line).", "Type 'done' to finish.")));
            inventory.setItem(24, GuiUtils.makeItem(Material.LEVER, "Executor: " + keyframe.getExecutorMode(),
                    List.of("Toggle executor.")));
            inventory.setItem(25, GuiUtils.makeItem(keyframe.isAllowGlobal() ? Material.LIME_DYE : Material.GRAY_DYE,
                    "Global Command Override: " + keyframe.isAllowGlobal(),
                    List.of("Allows global: prefixed commands even when scene-level option is off.")));

            int slot = 36;
            int index = 0;
            for (String command : keyframe.getCommands()) {
                inventory.setItem(slot++, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Command " + (index + 1),
                        List.of(command, "Right-click to remove.")));
                index++;
                if (slot >= 45) {
                    break;
                }
            }
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to tick menu.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
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
            editorEngine.openTickActionMenu(player, session, false);
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
            editorEngine.openConfirm(player, session, ConfirmAction.DELETE_KEYFRAME, session.getSelectedTrack(),
                    keyframe.getId());
            return;
        }
        if (slot == 10) {
            editorEngine.duplicateKeyframe(session, keyframe);
            refresh(session);
            return;
        }
        if (slot == 20) {
            player.closeInventory();
            session.setCursorTimeTicks(keyframe.getTimeTicks());
            editorEngine.getInputManager().beginCommandAppendInputMulti(player, session.getScene(), session, keyframe,
                    GuiType.COMMAND_EDITOR);
            return;
        }
        if (slot == 24) {
            keyframe.setExecutorMode(keyframe.getExecutorMode() == CommandKeyframe.ExecutorMode.PLAYER
                    ? CommandKeyframe.ExecutorMode.CONSOLE
                    : CommandKeyframe.ExecutorMode.PLAYER);
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot == 25) {
            keyframe.setAllowGlobal(!keyframe.isAllowGlobal());
            editorEngine.markDirty(session.getScene());
            refresh(session);
            return;
        }
        if (slot >= 36 && slot < 45 && ctx.isRightClick()) {
            int index = slot - 36;
            if (index < keyframe.getCommands().size()) {
                session.setConfirmCommandIndex(index);
                editorEngine.openConfirm(player, session, ConfirmAction.REMOVE_COMMAND, null, null);
            }
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
