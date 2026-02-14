package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ActorTimelineGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ActorTimelineGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        Inventory inventory = GuiUtils.createInventory(54, scene.getName() + " • Actors Timeline");
        GuiUtils.fillInventory(inventory);
        List<SceneActorTemplate> actors = new ArrayList<>(scene.getActorTemplates().values());
        for (int i = 0; i < Math.min(9, actors.size()); i++) {
            SceneActorTemplate actor = actors.get(i);
            boolean selected = actor.getActorId().equalsIgnoreCase(session.getSelectedActorId());
            inventory.setItem(i, GuiUtils.makeItem(selected ? Material.GLOWSTONE_DUST : Material.PLAYER_HEAD,
                    actor.getActorId(),
                    List.of(selected ? "Selected actor" : "Click to select actor",
                            "Transforms: " + actor.getTransformTicks().size(),
                            "Actions: " + actor.getTickActions().size())));
        }

        int startTick = ((session.getCurrentTick() - 1) / 9) * 9 + 1;
        SceneActorTemplate selectedActor = scene.getActorTemplate(session.getSelectedActorId());
        for (int i = 0; i < 9; i++) {
            int tick = startTick + i;
            ActorTickAction action = selectedActor == null ? null : selectedActor.getTickAction(tick);
            Material mat = action == null ? Material.GRAY_WOOL : Material.LIME_WOOL;
            inventory.setItem(18 + i, GuiUtils.makeItem(mat, "Tick " + tick,
                    List.of("Selected: " + (selectedActor == null ? "none" : selectedActor.getActorId()),
                            "Click: edit tick actions")));
        }
        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        List<SceneActorTemplate> actors = new ArrayList<>(session.getScene().getActorTemplates().values());
        if (ctx.getSlot() >= 0 && ctx.getSlot() < Math.min(9, actors.size())) {
            session.setSelectedActorId(actors.get(ctx.getSlot()).getActorId());
            refresh(session);
            return;
        }
        if (ctx.getSlot() >= 18 && ctx.getSlot() <= 26) {
            session.setCurrentTick(((session.getCurrentTick() - 1) / 9) * 9 + 1 + (ctx.getSlot() - 18));
            editorEngine.openActorTickActions(player, session, true);
            return;
        }
        if (ctx.getSlot() == 45) {
            editorEngine.navigateBack(player, session);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
