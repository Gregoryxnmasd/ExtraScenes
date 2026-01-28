package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import org.bukkit.entity.Player;

public class SceneEditorEngine {
    private final ExtraScenesPlugin plugin;

    public SceneEditorEngine(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
    }

    public void openEditor(Player player, Scene scene) {
        SceneEditorGUI gui = new SceneEditorGUI(plugin, scene);
        gui.open(player);
    }
}
