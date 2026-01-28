package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SceneSettingsGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public SceneSettingsGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        Inventory inventory = GuiUtils.createInventory(27, "Scene Settings");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.REPEATER, "Scene Settings",
                List.of("Configure scene defaults.")));

        inventory.setItem(10, GuiUtils.makeItem(Material.REPEATER, "Default Smoothing: " + scene.getDefaultSmoothing(),
                List.of("Cycle smoothing mode.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.COMPASS, "Camera Mode: " + scene.getCameraMode(),
                List.of("Toggle spectator/packet.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.ICE, "Freeze Player: " + scene.isFreezePlayer(),
                List.of("Toggle player freeze.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.COMMAND_BLOCK,
                "Allow Global Commands: " + scene.isAllowGlobalCommands(),
                List.of("Toggle global command execution.")));

        inventory.setItem(18, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
        inventory.setItem(22, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(26, GuiUtils.makeItem(Material.WRITABLE_BOOK, "Save Scene", List.of("Write JSON to disk.")));

        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        int slot = ctx.getSlot();
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        Scene scene = session.getScene();
        if (slot == 18) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 26) {
            editorEngine.saveScene(player, session);
            refresh(session);
            return;
        }
        if (slot == 10) {
            scene.setDefaultSmoothing(scene.getDefaultSmoothing().next());
            refresh(session);
            return;
        }
        if (slot == 12) {
            scene.setCameraMode(scene.getCameraMode().equalsIgnoreCase("SPECTATOR") ? "PACKET" : "SPECTATOR");
            refresh(session);
            return;
        }
        if (slot == 14) {
            scene.setFreezePlayer(!scene.isFreezePlayer());
            refresh(session);
            return;
        }
        if (slot == 16) {
            scene.setAllowGlobalCommands(!scene.isAllowGlobalCommands());
            refresh(session);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
