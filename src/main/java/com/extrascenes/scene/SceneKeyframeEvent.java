package com.extrascenes.scene;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SceneKeyframeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Scene scene;
    private final SceneKeyframe keyframe;

    public SceneKeyframeEvent(Player player, Scene scene, SceneKeyframe keyframe) {
        this.player = player;
        this.scene = scene;
        this.keyframe = keyframe;
    }

    public Player getPlayer() {
        return player;
    }

    public Scene getScene() {
        return scene;
    }

    public SceneKeyframe getKeyframe() {
        return keyframe;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
