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
        Inventory inventory = GuiUtils.createInventory(27, scene.getName() + " • Settings");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.REPEATER, "Scene Settings",
                List.of("Configure scene defaults.")));

        inventory.setItem(10, GuiUtils.makeItem(Material.REPEATER, "Default Smoothing: " + scene.getDefaultSmoothing(),
                List.of("Cycle easing mode.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.SLIME_BALL, "Smoothing Quality: " + scene.getSmoothingQuality(),
                List.of("Cycle quality preset.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.ICE, "Freeze Player: " + scene.isFreezePlayer(),
                List.of("Toggle player freeze.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.COMMAND_BLOCK,
                "Allow Unrestricted Command Keyframes: " + scene.isAllowGlobalCommands(),
                List.of("When disabled, only non-global command keyframes run.", "Prefix with global: to require this option.")));

        inventory.setItem(20, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
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
        Scene scene = session.getScene();
        if (slot == 20) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 10) {
            scene.setDefaultSmoothing(scene.getDefaultSmoothing().next());
            editorEngine.markDirty(scene);
            refresh(session);
            return;
        }
        if (slot == 12) {
            scene.setSmoothingQuality(scene.getSmoothingQuality().next());
            editorEngine.markDirty(scene);
            refresh(session);
            return;
        }
        if (slot == 14) {
            scene.setFreezePlayer(!scene.isFreezePlayer());
            editorEngine.markDirty(scene);
            refresh(session);
            return;
        }
        if (slot == 16) {
            scene.setAllowGlobalCommands(!scene.isAllowGlobalCommands());
            editorEngine.markDirty(scene);
            refresh(session);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
