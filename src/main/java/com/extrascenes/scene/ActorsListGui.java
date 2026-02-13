package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ActorsListGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ActorsListGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(54, session.getSceneName() + " • Actors");
        GuiUtils.fillInventory(inventory);
        List<SceneActorTemplate> actors = new ArrayList<>(session.getScene().getActorTemplates().values());
        for (int i = 0; i < Math.min(45, actors.size()); i++) {
            SceneActorTemplate actor = actors.get(i);
            boolean hasRecording = !actor.getTransformTicks().isEmpty();
            inventory.setItem(i, GuiUtils.makeItem(
                    hasRecording ? Material.LIME_DYE : Material.GRAY_DYE,
                    actor.getActorId(),
                    List.of(
                            "Recorded ticks: " + actor.getTransformTicks().size(),
                            "Preview: " + (actor.isPreviewEnabled() ? "ON" : "OFF"),
                            "Click for details"
                    )));
        }
        inventory.setItem(49, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        if (ctx.getSlot() == 49) {
            editorEngine.navigateBack(player, session);
            return;
        }
        List<SceneActorTemplate> actors = new ArrayList<>(session.getScene().getActorTemplates().values());
        if (ctx.getSlot() >= 0 && ctx.getSlot() < Math.min(45, actors.size())) {
            session.setSelectedActorId(actors.get(ctx.getSlot()).getActorId());
            editorEngine.openActorDetail(player, session, true);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
