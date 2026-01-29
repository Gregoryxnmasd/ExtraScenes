package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TickToolsGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public TickToolsGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        int tick = session.getCurrentTick();
        Inventory inventory = GuiUtils.createInventory(27, session.getSceneName() + " • Tick Tools");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.CLOCK, "Tick " + tick,
                List.of("Global tick utilities.")));

        inventory.setItem(10, GuiUtils.makeItem(Material.ENDER_PEARL, "Move Tick",
                List.of("Chat input for destination tick.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.PAPER, "Copy Tick",
                List.of("Copy actions to another tick.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.WRITABLE_BOOK, "Duplicate Tick Actions",
                List.of("Clone actions to tick " + (tick + 1) + ".")));
        inventory.setItem(16, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Clear Tick",
                List.of("Remove all actions at this tick.")));
        inventory.setItem(18, GuiUtils.makeItem(Material.HOPPER, "Shift Range",
                List.of("Shift ticks via chat input.")));

        inventory.setItem(22, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to tick menu.")));
        inventory.setItem(26, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 22) {
            editorEngine.openTickActionMenu(player, session, true);
            return;
        }
        if (slot == 26) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 10) {
            player.closeInventory();
            editorEngine.getInputManager().beginMoveTickInput(player, session.getScene(), session, session.getCurrentTick(),
                    GuiType.TICK_TOOLS);
            return;
        }
        if (slot == 12) {
            player.closeInventory();
            editorEngine.getInputManager().beginCopyTickInput(player, session.getScene(), session, session.getCurrentTick(),
                    GuiType.TICK_TOOLS);
            return;
        }
        if (slot == 14) {
            editorEngine.duplicateTickActions(session.getScene(), session.getCurrentTick(), session.getCurrentTick() + 1,
                    session);
            refresh(session);
            return;
        }
        if (slot == 16) {
            editorEngine.openConfirm(player, session, ConfirmAction.CLEAR_TICK, null, null);
            return;
        }
        if (slot == 18) {
            player.closeInventory();
            editorEngine.getInputManager().beginShiftRangeInput(player, session.getScene(), session, GuiType.TICK_TOOLS);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
