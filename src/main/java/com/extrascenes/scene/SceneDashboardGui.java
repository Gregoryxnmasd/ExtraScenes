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
        Inventory inventory = GuiUtils.createInventory(27, "Editing: " + scene.getName());
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.WRITABLE_BOOK,
                "Scene: " + scene.getName(),
                List.of(
                        "Duration: " + scene.getDurationTicks() + "t",
                        "Mode: " + (session.isPreviewPlaying() ? "Preview" : "Edit"),
                        "Last Saved: " + GuiUtils.formatLastSaved(session.getLastSavedAt())
                )));

        inventory.setItem(10, GuiUtils.makeItem(Material.CLOCK, "Tick Grid",
                List.of("Edit ticks in grouped grids.")));
        inventory.setItem(12, GuiUtils.makeItem(session.isPreviewPlaying() ? Material.LIME_DYE : Material.YELLOW_DYE,
                session.isPreviewPlaying() ? "Preview Pause" : "Preview Playback",
                List.of("Preview scene playback.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.REDSTONE_TORCH, "Play",
                List.of("Run the scene for real.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.REPEATER, "Settings",
                List.of("Scene-level settings.")));

        if (session.hasHistory()) {
            inventory.setItem(18, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to previous menu.")));
        }
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
        if (slot == 10) {
            editorEngine.openGroupSelect(player, session, true);
            return;
        }
        if (slot == 12) {
            editorEngine.togglePreview(player, session);
            refresh(session);
            return;
        }
        if (slot == 14) {
            editorEngine.playScene(player, session);
            refresh(session);
            return;
        }
        if (slot == 16) {
            editorEngine.openSceneSettings(player, session, true);
            return;
        }
        if (slot == 18 && session.hasHistory()) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
