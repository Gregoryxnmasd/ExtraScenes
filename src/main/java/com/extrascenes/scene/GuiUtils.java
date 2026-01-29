package com.extrascenes.scene;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

public final class GuiUtils {
    public static final String TITLE_PREFIX = ChatColor.DARK_GRAY.toString();
    public static final Material FRAME_GLASS = Material.GRAY_STAINED_GLASS_PANE;
    public static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("0.00");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private GuiUtils() {
    }

    public static Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(null, size, TITLE_PREFIX + title);
    }

    public static ItemStack makeItem(Material material, String name, List<String> lore) {
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

    public static ItemStack makeGlass() {
        ItemStack glass = new ItemStack(FRAME_GLASS);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + " ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    public static void fillInventory(Inventory inventory) {
        ItemStack glass = makeGlass();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }
    }

    public static void refreshInventory(EditorSession session, Inventory updated) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getSize() != updated.getSize()) {
            player.openInventory(updated);
            return;
        }
        top.setContents(updated.getContents());
    }

    public static ItemStack buildKeyframeItem(Keyframe keyframe, UUID selectedId) {
        List<String> lore = new ArrayList<>();
        lore.add("Time: " + keyframe.getTimeTicks() + "t (" + SECONDS_FORMAT.format(keyframe.getTimeTicks() / 20.0) + "s)");
        if (keyframe instanceof CameraKeyframe camera) {
            Transform transform = camera.getTransform();
            lore.add("Pos: " + formatTransform(transform));
            lore.add("Smooth: " + camera.getSmoothingMode());
        } else if (keyframe instanceof ActionBarKeyframe actionBar) {
            lore.add("Text: " + nullToPlaceholder(actionBar.getText()));
            lore.add("Duration: " + actionBar.getDurationTicks() + "t");
        } else if (keyframe instanceof CommandKeyframe command) {
            lore.add("Commands: " + command.getCommands().size());
            lore.add("Executor: " + command.getExecutorMode());
        } else if (keyframe instanceof ModelKeyframe model) {
            lore.add("Action: " + model.getAction());
            lore.add("Entry: " + nullToPlaceholder(model.getModelEntry()));
            lore.add("ModelId: " + nullToPlaceholder(model.getModelId()));
        } else if (keyframe instanceof ParticleKeyframe particle) {
            lore.add("Particle: " + nullToPlaceholder(particle.getParticleId()));
        } else if (keyframe instanceof SoundKeyframe sound) {
            lore.add("Sound: " + nullToPlaceholder(sound.getSoundId()));
        } else if (keyframe instanceof BlockIllusionKeyframe block) {
            lore.add("Block: " + block.getMaterial());
        }
        lore.add("Left: select");
        lore.add("Right: edit");
        lore.add("Shift: move time");

        boolean selected = selectedId != null && selectedId.equals(keyframe.getId());
        Material material = selected ? Material.DIAMOND : Material.PAPER;
        return makeItem(material, keyframe.getType() + " Keyframe", lore);
    }

    public static String formatTransform(Transform transform) {
        if (transform == null) {
            return "unset";
        }
        return String.format("%.1f, %.1f, %.1f", transform.getX(), transform.getY(), transform.getZ());
    }

    public static String nullToPlaceholder(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String formatLastSaved(long epochMillis) {
        if (epochMillis <= 0) {
            return "Never";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }
}
