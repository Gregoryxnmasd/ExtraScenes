package com.extrascenes.scene;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SceneEditorGUI {
    public static final String MAIN_TITLE_PREFIX = ChatColor.DARK_AQUA + "ExtraScenes • Editing: ";
    public static final String ADD_TITLE_PREFIX = ChatColor.DARK_AQUA + "ExtraScenes • Add Keyframe: ";
    public static final String EDIT_TITLE_PREFIX = ChatColor.DARK_AQUA + "ExtraScenes • Edit Keyframe: ";

    private static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("0.00");

    public Inventory buildMainMenu(Scene scene, EditorSession session) {
        Inventory inventory = Bukkit.createInventory(null, 54, MAIN_TITLE_PREFIX + scene.getName());

        fillBackground(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(0, makeItem(session.isPreviewPlaying() ? Material.LIME_DYE : Material.YELLOW_DYE,
                session.isPreviewPlaying() ? "Preview Pause" : "Preview Play",
                List.of("Preview current scene for you only.")));
        inventory.setItem(1, makeItem(Material.RED_DYE, "Preview Stop", List.of("Stop preview and return.")));
        inventory.setItem(2, makeItem(Material.WRITABLE_BOOK, "Save Scene",
                List.of("Persist scene to disk.")));
        inventory.setItem(3, makeItem(Material.CLOCK, "Scene Duration",
                List.of("Ticks: " + scene.getDurationTicks(),
                        "Seconds: " + SECONDS_FORMAT.format(scene.getDurationTicks() / 20.0))));
        inventory.setItem(4, makeItem(Material.COMPASS, "Cursor Time",
                List.of("Ticks: " + session.getCursorTimeTicks(),
                        "Seconds: " + SECONDS_FORMAT.format(session.getCursorTimeTicks() / 20.0))));
        inventory.setItem(6, makeItem(Material.ARROW, "Prev Page", List.of("Page " + (session.getKeyframePage() + 1))));
        inventory.setItem(7, makeItem(Material.ARROW, "Next Page", List.of("Page " + (session.getKeyframePage() + 1))));
        inventory.setItem(8, makeItem(Material.BARRIER, "Exit Editor", List.of("Close the editor.")));

        addTrackButton(inventory, 9, SceneTrackType.CAMERA, session);
        addTrackButton(inventory, 18, SceneTrackType.COMMAND, session);
        addTrackButton(inventory, 27, SceneTrackType.MODEL, session);
        addTrackButton(inventory, 36, SceneTrackType.PARTICLE, session);
        addTrackButton(inventory, 45, SceneTrackType.SOUND, session);
        addTrackButton(inventory, 46, SceneTrackType.BLOCK_ILLUSION, session);

        renderKeyframes(inventory, scene, session);

        inventory.setItem(47, makeItem(Material.REDSTONE, "Cursor -20", List.of("Move cursor back 20 ticks.")));
        inventory.setItem(48, makeItem(Material.REDSTONE, "Cursor -1", List.of("Move cursor back 1 tick.")));
        inventory.setItem(49, makeItem(Material.EMERALD, "Add Keyframe",
                List.of("Add a keyframe at cursor time.")));
        inventory.setItem(50, makeItem(Material.GLOWSTONE_DUST, "Cursor +1", List.of("Move cursor forward 1 tick.")));
        inventory.setItem(51, makeItem(Material.GLOWSTONE_DUST, "Cursor +20", List.of("Move cursor forward 20 ticks.")));
        inventory.setItem(52, makeItem(Material.ANVIL, "Jump Cursor",
                List.of("Set cursor to specific time via chat.")));
        inventory.setItem(53, makeItem(Material.LEVER, "Cursor to Selected",
                List.of("Set cursor to selected keyframe time.")));

        return inventory;
    }

    public Inventory buildAddKeyframeMenu(Scene scene, EditorSession session) {
        Inventory inventory = Bukkit.createInventory(null, 27, ADD_TITLE_PREFIX + scene.getName());
        fillBackground(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(10, makeItem(Material.ENDER_EYE, "Add Camera Keyframe",
                List.of("Capture current view", "Time: " + session.getCursorTimeTicks())));
        inventory.setItem(12, makeItem(Material.COMMAND_BLOCK, "Add Command Keyframe",
                List.of("Enter commands via chat")));
        inventory.setItem(14, makeItem(Material.ARMOR_STAND, "Add Model Keyframe",
                List.of("Enter model data via chat")));
        inventory.setItem(16, makeItem(Material.NOTE_BLOCK, "Add Sound Keyframe",
                List.of("Uses current location")));
        inventory.setItem(18, makeItem(Material.BLAZE_POWDER, "Add Particle Keyframe",
                List.of("Uses current location")));
        inventory.setItem(20, makeItem(Material.BARRIER, "Add Block Illusion",
                List.of("Uses current location")));
        inventory.setItem(22, makeItem(Material.ARROW, "Back", List.of("Return to editor")));
        return inventory;
    }

    public Inventory buildKeyframeEditMenu(Scene scene, EditorSession session, Keyframe keyframe) {
        Inventory inventory = Bukkit.createInventory(null, 27, EDIT_TITLE_PREFIX + scene.getName());
        fillBackground(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(22, makeItem(Material.ARROW, "Back", List.of("Return to editor")));

        if (keyframe instanceof CameraKeyframe camera) {
            inventory.setItem(10, makeItem(Material.ENDER_PEARL, "Set To Current View",
                    List.of("Overwrite transform with current view.")));
            inventory.setItem(11, makeItem(Material.REPEATER, "Smoothing: " + camera.getSmoothingMode(),
                    List.of("Click to cycle.")));
            inventory.setItem(12, makeItem(Material.LEVER, "Instant: " + camera.isInstant(),
                    List.of("Toggle instant teleport.")));
            inventory.setItem(13, makeItem(Material.TARGET, "LookAt: " + camera.getLookAt().getMode(),
                    List.of("Toggle none/position.")));
            inventory.setItem(15, makeItem(Material.ARROW, "X -0.5", List.of("Adjust X")));
            inventory.setItem(16, makeItem(Material.ARROW, "X +0.5", List.of("Adjust X")));
            inventory.setItem(17, makeItem(Material.ARROW, "Y -0.5", List.of("Adjust Y")));
            inventory.setItem(18, makeItem(Material.ARROW, "Y +0.5", List.of("Adjust Y")));
            inventory.setItem(19, makeItem(Material.ARROW, "Z -0.5", List.of("Adjust Z")));
            inventory.setItem(20, makeItem(Material.ARROW, "Z +0.5", List.of("Adjust Z")));
            inventory.setItem(21, makeItem(Material.ARROW, "Yaw/Pitch +5", List.of("Adjust rotation")));
        } else if (keyframe instanceof CommandKeyframe command) {
            inventory.setItem(10, makeItem(Material.COMMAND_BLOCK, "Add Command", List.of("Chat input")));
            inventory.setItem(11, makeItem(Material.BARRIER, "Clear Commands",
                    List.of("Commands: " + command.getCommands().size())));
            inventory.setItem(12, makeItem(Material.LEVER, "Executor: " + command.getExecutorMode(),
                    List.of("Toggle executor")));
            inventory.setItem(13, makeItem(Material.PAPER, "Allow Global: " + command.isAllowGlobal(),
                    List.of("Toggle console access")));
        } else if (keyframe instanceof ModelKeyframe model) {
            inventory.setItem(10, makeItem(Material.ARMOR_STAND, "Action: " + model.getAction(),
                    List.of("Cycle action")));
            inventory.setItem(11, makeItem(Material.NAME_TAG, "EntityRef: " + nullToPlaceholder(model.getEntityRef()),
                    List.of("Chat input")));
            inventory.setItem(12, makeItem(Material.PAPER, "ModelId: " + nullToPlaceholder(model.getModelId()),
                    List.of("Chat input")));
            inventory.setItem(13, makeItem(Material.WRITABLE_BOOK, "AnimationId: " + nullToPlaceholder(model.getAnimationId()),
                    List.of("Chat input")));
        }

        return inventory;
    }

    public List<Integer> getKeyframeSlots() {
        return List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        );
    }

    private void renderKeyframes(Inventory inventory, Scene scene, EditorSession session) {
        List<Integer> slots = getKeyframeSlots();
        Track<? extends Keyframe> track = scene.getTrack(session.getSelectedTrack());
        List<? extends Keyframe> keyframes = track == null ? List.of() : track.getKeyframes();
        int pageSize = slots.size();
        int startIndex = session.getKeyframePage() * pageSize;
        int endIndex = Math.min(keyframes.size(), startIndex + pageSize);

        if (keyframes.isEmpty() || startIndex >= keyframes.size()) {
            ItemStack empty = makeItem(Material.BLACK_STAINED_GLASS_PANE, "No Keyframes",
                    List.of("Use Add Keyframe to create one."));
            for (int slot : slots) {
                inventory.setItem(slot, empty);
            }
            return;
        }

        for (int i = 0; i < slots.size(); i++) {
            int index = startIndex + i;
            if (index >= endIndex) {
                inventory.setItem(slots.get(i), makeItem(Material.BLACK_STAINED_GLASS_PANE, "", List.of()));
                continue;
            }
            Keyframe keyframe = keyframes.get(index);
            inventory.setItem(slots.get(i), buildKeyframeItem(keyframe, session.getSelectedKeyframeId()));
        }
    }

    private ItemStack buildKeyframeItem(Keyframe keyframe, UUID selectedId) {
        List<String> lore = new ArrayList<>();
        lore.add("Time: " + keyframe.getTimeTicks() + " (" + SECONDS_FORMAT.format(keyframe.getTimeTicks() / 20.0) + "s)");
        if (keyframe instanceof CameraKeyframe camera) {
            Transform transform = camera.getTransform();
            lore.add("Pos: " + formatTransform(transform));
            lore.add("Smooth: " + camera.getSmoothingMode());
            lore.add("Instant: " + camera.isInstant());
        } else if (keyframe instanceof CommandKeyframe command) {
            lore.add("Commands: " + command.getCommands().size());
            lore.add("Executor: " + command.getExecutorMode());
        } else if (keyframe instanceof ModelKeyframe model) {
            lore.add("Action: " + model.getAction());
            lore.add("Ref: " + nullToPlaceholder(model.getEntityRef()));
            lore.add("Model: " + nullToPlaceholder(model.getModelId()));
        } else if (keyframe instanceof ParticleKeyframe particle) {
            lore.add("Particle: " + nullToPlaceholder(particle.getParticleId()));
        } else if (keyframe instanceof SoundKeyframe sound) {
            lore.add("Sound: " + nullToPlaceholder(sound.getSoundId()));
        } else if (keyframe instanceof BlockIllusionKeyframe block) {
            lore.add("Block: " + block.getMaterial());
        }
        lore.add("Left: select | Right: edit | Shift: move");

        boolean selected = selectedId != null && selectedId.equals(keyframe.getId());
        return makeItem(selected ? Material.DIAMOND : Material.PAPER,
                keyframe.getType() + " Keyframe", lore);
    }

    private void addTrackButton(Inventory inventory, int slot, SceneTrackType type, EditorSession session) {
        Material material = switch (type) {
            case CAMERA -> Material.ENDER_EYE;
            case COMMAND -> Material.COMMAND_BLOCK;
            case MODEL -> Material.ARMOR_STAND;
            case PARTICLE -> Material.BLAZE_POWDER;
            case SOUND -> Material.NOTE_BLOCK;
            case BLOCK_ILLUSION -> Material.BARRIER;
        };
        String name = type.name().substring(0, 1) + type.name().substring(1).toLowerCase();
        List<String> lore = List.of("Select track", "Selected: " + session.getSelectedTrack());
        if (session.getSelectedTrack() == type) {
            name = ChatColor.GREEN + name + " (selected)";
        }
        inventory.setItem(slot, makeItem(material, name, lore));
    }

    private ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + name);
            List<String> formatted = new ArrayList<>();
            for (String line : lore) {
                formatted.add(ChatColor.GRAY + line);
            }
            meta.setLore(formatted);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBackground(Inventory inventory, Material material) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(material));
        }
    }

    private String formatTransform(Transform transform) {
        if (transform == null) {
            return "unset";
        }
        return String.format("%.1f, %.1f, %.1f", transform.getX(), transform.getY(), transform.getZ());
    }

    private String nullToPlaceholder(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
