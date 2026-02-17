package com.extrascenes;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

import com.extrascenes.command.SceneCommand;
import com.extrascenes.core.Scene;
import com.extrascenes.gui.GuiListener;
import com.extrascenes.runtime.ActorController;
import com.extrascenes.runtime.CameraRigController;
import com.extrascenes.runtime.CutsceneRuntime;
import com.extrascenes.runtime.ZoomController;
import com.extrascenes.storage.SceneStorage;

public class ExtraScenesPlugin extends JavaPlugin {
    private SceneStorage storage;
    private final Map<String, Scene> scenes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        storage = new SceneStorage(getDataFolder().toPath());
        scenes.clear();
        scenes.putAll(storage.load());

        CameraRigController cameraRigController = new CameraRigController(this);
        ZoomController zoomController = new ZoomController(this);
        ActorController actorController = new ActorController(this);
        CutsceneRuntime runtime = new CutsceneRuntime(this, cameraRigController, zoomController, actorController);
        new GuiListener(this, runtime);

        SceneCommand sceneCommand = new SceneCommand(scenes, storage, runtime);
        getCommand("scene").setExecutor(sceneCommand);
        getLogger().info("ExtraScenes core mode enabled.");
    }

    @Override
    public void onDisable() {
        storage.save(scenes);
    }
}
