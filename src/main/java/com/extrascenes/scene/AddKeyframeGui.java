package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class AddKeyframeGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public AddKeyframeGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(27,
                "Scene: " + session.getSceneName() + " • Add Keyframe • Group: " + session.getCurrentGroup()
                        + " • Tick: " + session.getCurrentTick());
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.WRITABLE_BOOK,
                "Add Keyframe (" + formatTrack(session.getSelectedTrack()) + ")",
                List.of("Choose a keyframe type.")));

        switch (session.getSelectedTrack()) {
            case CAMERA -> {
                inventory.setItem(11, GuiUtils.makeItem(Material.ENDER_EYE, "Add Camera Keyframe",
                        List.of("Capture current view.")));
                inventory.setItem(13, GuiUtils.makeItem(Material.SPYGLASS, "Add Camera Keyframe (Instant)",
                        List.of("Instant cut to view.")));
                inventory.setItem(15, GuiUtils.makeItem(Material.TARGET, "Add LookAt Segment",
                        List.of("Use current view as target.")));
            }
            case COMMAND -> inventory.setItem(13, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Add Command Keyframe",
                    List.of("Enter commands via chat.")));
            case MODEL -> {
                inventory.setItem(11, GuiUtils.makeItem(Material.ARMOR_STAND, "Spawn Model Keyframe",
                        List.of("Create a spawn action.")));
                inventory.setItem(13, GuiUtils.makeItem(Material.NAME_TAG, "Play Animation Keyframe",
                        List.of("Create an animation action.")));
                inventory.setItem(15, GuiUtils.makeItem(Material.BARRIER, "Despawn Model Keyframe",
                        List.of("Create a despawn action.")));
            }
            case PARTICLE -> inventory.setItem(13, GuiUtils.makeItem(Material.BLAZE_POWDER, "Add Particle Keyframe",
                    List.of("Uses current location.")));
            case SOUND -> inventory.setItem(13, GuiUtils.makeItem(Material.NOTE_BLOCK, "Add Sound Keyframe",
                    List.of("Uses current location.")));
            case BLOCK_ILLUSION -> inventory.setItem(13, GuiUtils.makeItem(Material.BARRIER, "Add Block Illusion",
                    List.of("Uses current location.")));
            default -> {
            }
        }

        inventory.setItem(18, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to keyframes.")));
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
        if (slot == 18) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 22) {
            editorEngine.closeEditor(player, session);
            return;
        }

        SceneTrackType trackType = session.getSelectedTrack();
        if (trackType == SceneTrackType.CAMERA) {
            if (slot == 11) {
                editorEngine.addCameraKeyframe(player, session, false);
            } else if (slot == 13) {
                editorEngine.addCameraKeyframe(player, session, true);
            } else if (slot == 15) {
                editorEngine.addLookAtKeyframe(player, session);
            }
            return;
        }
        if (trackType == SceneTrackType.COMMAND && slot == 13) {
            player.closeInventory();
            editorEngine.getInputManager().beginCommandInput(player, session.getScene(), session, GuiType.KEYFRAME_LIST);
            return;
        }
        if (trackType == SceneTrackType.MODEL) {
            if (slot == 11) {
                player.closeInventory();
                editorEngine.getInputManager().beginModelInput(player, session.getScene(), session,
                        ModelKeyframe.Action.SPAWN, GuiType.KEYFRAME_LIST);
            } else if (slot == 13) {
                player.closeInventory();
                editorEngine.getInputManager().beginModelInput(player, session.getScene(), session,
                        ModelKeyframe.Action.ANIM, GuiType.KEYFRAME_LIST);
            } else if (slot == 15) {
                player.closeInventory();
                editorEngine.getInputManager().beginModelInput(player, session.getScene(), session,
                        ModelKeyframe.Action.DESPAWN, GuiType.KEYFRAME_LIST);
            }
            return;
        }
        if (trackType == SceneTrackType.PARTICLE && slot == 13) {
            editorEngine.addParticleKeyframe(player, session);
            return;
        }
        if (trackType == SceneTrackType.SOUND && slot == 13) {
            editorEngine.addSoundKeyframe(player, session);
            return;
        }
        if (trackType == SceneTrackType.BLOCK_ILLUSION && slot == 13) {
            editorEngine.addBlockKeyframe(player, session);
        }
    }

    private String formatTrack(SceneTrackType type) {
        String name = type.name().toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}
