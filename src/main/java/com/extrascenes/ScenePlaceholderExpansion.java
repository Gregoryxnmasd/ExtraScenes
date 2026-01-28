package com.extrascenes;

import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.scene.SceneState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ScenePlaceholderExpansion extends PlaceholderExpansion {
    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;

    public ScenePlaceholderExpansion(ExtraScenesPlugin plugin, SceneSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @Override
    public String getIdentifier() {
        return "scenes";
    }

    @Override
    public String getAuthor() {
        return "ExtraScenes";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        SceneSession session = sessionManager.getSession(player.getUniqueId());
        if (session == null) {
            return switch (params) {
                case "active" -> "false";
                case "scene" -> "";
                case "time_ticks" -> "0";
                case "tick" -> "0";
                case "duration_ticks" -> "0";
                case "progress" -> "0";
                case "state" -> "";
                default -> "";
            };
        }
        int duration = session.getScene().getDurationTicks();
        int time = session.getTimeTicks();
        return switch (params) {
            case "active" -> "true";
            case "scene" -> session.getScene().getName();
            case "time_ticks" -> String.valueOf(time);
            case "tick" -> String.valueOf(time);
            case "duration_ticks" -> String.valueOf(duration);
            case "progress" -> String.valueOf(duration <= 0 ? 0 : Math.min(100, Math.round((time / (float) duration) * 100f)));
            case "state" -> session.getState() == null ? SceneState.PLAYING.name() : session.getState().name();
            default -> "";
        };
    }
}
