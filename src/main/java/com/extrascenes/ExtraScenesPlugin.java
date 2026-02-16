package com.extrascenes;

import com.extrascenes.command.SceneCommandExecutor;
import com.extrascenes.scene.EditorChatListener;
import com.extrascenes.scene.ActorRecordingService;
import com.extrascenes.scene.EditorSessionManager;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneEditorListener;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneRuntimeEngine;
import com.extrascenes.scene.SkinLibrary;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.visibility.SceneVisibilityController;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtraScenesPlugin extends JavaPlugin {
    private SceneManager sceneManager;
    private SceneSessionManager sessionManager;
    private SceneRuntimeEngine runtimeEngine;
    private SceneVisibilityController visibilityController;
    private SceneProtocolAdapter protocolAdapter;
    private SceneModelTrackAdapter modelTrackAdapter;
    private CitizensAdapter citizensAdapter;
    private EditorSessionManager editorSessionManager;
    private SceneEditorEngine editorEngine;
    private ActorRecordingService actorRecordingService;
    private SkinLibrary skinLibrary;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.protocolAdapter = new SceneProtocolAdapter(this);
        this.citizensAdapter = new CitizensAdapter(this);
        this.visibilityController = new SceneVisibilityController(this);
        this.sceneManager = new SceneManager(this);
        this.actorRecordingService = new ActorRecordingService(this);
        this.skinLibrary = new SkinLibrary(getDataFolder());
        this.editorSessionManager = new EditorSessionManager();
        this.editorEngine = new SceneEditorEngine(this, sceneManager, editorSessionManager);
        this.sessionManager = new SceneSessionManager(this, visibilityController, protocolAdapter);
        this.runtimeEngine = new SceneRuntimeEngine(this, sessionManager, visibilityController, protocolAdapter);
        this.modelTrackAdapter = new SceneModelTrackAdapter(this, visibilityController);
        Plugin modelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        boolean modelEngineDetected = modelEngine != null && modelEngine.isEnabled();
        getLogger().info("ModelEngine detected " + (modelEngineDetected ? "YES" : "NO"));

        Bukkit.getPluginManager().registerEvents(new SceneListener(sessionManager, visibilityController,
                editorSessionManager, editorEngine.getInputManager(), editorEngine, actorRecordingService), this);
        Bukkit.getPluginManager().registerEvents(new SceneEditorListener(editorEngine, editorSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new EditorChatListener(this, editorEngine.getInputManager(), editorEngine), this);

        PluginCommand command = getCommand("scene");
        if (command != null) {
            SceneCommandExecutor commandExecutor = new SceneCommandExecutor(this, sceneManager, sessionManager, editorEngine);
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        runtimeEngine.start();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ScenePlaceholderExpansion(this, sessionManager).register();
        }
    }

    @Override
    public void onDisable() {
        if (actorRecordingService != null) {
            actorRecordingService.stopAll(true);
        }
        if (runtimeEngine != null) {
            runtimeEngine.stop();
        }
        if (sessionManager != null) {
            sessionManager.stopAll("plugin_disable");
        }
        if (sceneManager != null) {
            sceneManager.saveAllDirty();
        }
        if (editorSessionManager != null) {
            editorSessionManager.clear();
        }
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public SceneSessionManager getSessionManager() {
        return sessionManager;
    }

    public SceneProtocolAdapter getProtocolAdapter() {
        return protocolAdapter;
    }

    public SceneModelTrackAdapter getModelTrackAdapter() {
        return modelTrackAdapter;
    }


    public CitizensAdapter getCitizensAdapter() {
        return citizensAdapter;
    }


    public ActorRecordingService getActorRecordingService() {
        return actorRecordingService;
    }

    public SceneEditorEngine getEditorEngine() {
        return editorEngine;
    }

    public SceneRuntimeEngine getRuntimeEngine() {
        return runtimeEngine;
    }

    public SceneVisibilityController getVisibilityController() {
        return visibilityController;
    }

    public SkinLibrary getSkinLibrary() {
        return skinLibrary;
    }
}
