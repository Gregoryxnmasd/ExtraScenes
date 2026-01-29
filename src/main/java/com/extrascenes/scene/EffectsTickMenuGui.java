package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class EffectsTickMenuGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public EffectsTickMenuGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        int tick = session.getCurrentTick();
        Inventory inventory = GuiUtils.createInventory(27, session.getSceneName() + " • Tick " + tick + " • Effects");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.NOTE_BLOCK, "Effects @ Tick " + tick,
                List.of("Particles, sounds, block illusions.")));

        inventory.setItem(10, GuiUtils.makeItem(Material.BLAZE_POWDER, "Add Particle",
                List.of("Place particle at your location.")));
        inventory.setItem(12, GuiUtils.makeItem(Material.NOTE_BLOCK, "Add Sound",
                List.of("Play sound at your location.")));
        inventory.setItem(14, GuiUtils.makeItem(Material.GLASS, "Add Block Illusion",
                List.of("Show block at your location.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Clear Effects",
                List.of("Remove all effect keyframes.")));

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
            editorEngine.addParticleKeyframeAtTick(player, session, session.getCurrentTick());
            refresh(session);
            return;
        }
        if (slot == 12) {
            editorEngine.addSoundKeyframeAtTick(player, session, session.getCurrentTick());
            refresh(session);
            return;
        }
        if (slot == 14) {
            editorEngine.addBlockKeyframeAtTick(player, session, session.getCurrentTick());
            refresh(session);
            return;
        }
        if (slot == 16) {
            editorEngine.openConfirm(player, session, ConfirmAction.CLEAR_EFFECTS, null, null);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
