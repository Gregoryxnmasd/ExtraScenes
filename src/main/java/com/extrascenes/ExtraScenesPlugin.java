package com.extrascenes;

import com.extrascenes.command.SceneCommandExecutor;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneRuntimeEngine;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.visibility.SceneVisibilityController;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtraScenesPlugin extends JavaPlugin {
    private SceneManager sceneManager;
    private SceneSessionManager sessionManager;
    private SceneRuntimeEngine runtimeEngine;
    private SceneVisibilityController visibilityController;
    private SceneProtocolAdapter protocolAdapter;
    private SceneModelTrackAdapter modelTrackAdapter;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.protocolAdapter = new SceneProtocolAdapter(this);
        this.visibilityController = new SceneVisibilityController(this);
        this.sceneManager = new SceneManager(this);
        this.sessionManager = new SceneSessionManager(this, visibilityController, protocolAdapter);
        this.runtimeEngine = new SceneRuntimeEngine(this, sessionManager, visibilityController, protocolAdapter);
        this.modelTrackAdapter = new SceneModelTrackAdapter(this, visibilityController);

        Bukkit.getPluginManager().registerEvents(new SceneListener(sessionManager, visibilityController), this);

        PluginCommand command = getCommand("scene");
        if (command != null) {
            command.setExecutor(new SceneCommandExecutor(this, sceneManager, sessionManager, runtimeEngine));
            command.setTabCompleter(new SceneCommandExecutor(this, sceneManager, sessionManager, runtimeEngine));
        }

        runtimeEngine.start();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ScenePlaceholderExpansion(this, sessionManager).register();
        }
    }

    @Override
    public void onDisable() {
        if (runtimeEngine != null) {
            runtimeEngine.stop();
        }
        if (sessionManager != null) {
            sessionManager.stopAll("plugin_disable");
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
}
