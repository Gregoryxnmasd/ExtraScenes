package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SceneDashboardGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public SceneDashboardGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        Inventory inventory = GuiUtils.createInventory(36, "Scene Editor • " + scene.getName());
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.WRITABLE_BOOK,
                "Scene: " + scene.getName(),
                List.of(
                        "Duration: " + scene.getDurationTicks() + "t",
                        "Last Saved: " + GuiUtils.formatLastSaved(session.getLastSavedAt())
                )));

        inventory.setItem(10, GuiUtils.makeItem(Material.ENDER_PEARL, "Camera", List.of("Camera track and options.")));
        inventory.setItem(11, GuiUtils.makeItem(Material.CLOCK, "Groups (ticks)", List.of("Browse 9-tick groups.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.PLAYER_HEAD, "Actors", List.of("Create, select and record actors.")));
        inventory.setItem(13, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Commands", List.of("Open current tick commands.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.PAPER, "Actionbars", List.of("Open current tick actionbar.")));
        inventory.setItem(15, GuiUtils.makeItem(Material.REPEATER, "Settings", List.of("Scene-level settings.")));

        if (session.hasHistory()) {
            inventory.setItem(27, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to previous menu.")));
        }
        inventory.setItem(35, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        int slot = ctx.getSlot();
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        if (slot == 10) {
            editorEngine.openGroupGrid(player, session, true);
            return;
        }
        if (slot == 11) {
            editorEngine.openGroupSelect(player, session, true);
            return;
        }
        if (slot == 12) {
            editorEngine.openActorsList(player, session, true);
            return;
        }
        if (slot == 13) {
            editorEngine.openCommandEditorForTick(player, session, session.getCurrentTick());
            return;
        }
        if (slot == 14) {
            editorEngine.openActionBarEditorForTick(player, session, session.getCurrentTick());
            return;
        }
        if (slot == 15) {
            editorEngine.openSceneSettings(player, session, true);
            return;
        }
        if (slot == 27 && session.hasHistory()) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 35) {
            editorEngine.closeEditor(player, session);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
