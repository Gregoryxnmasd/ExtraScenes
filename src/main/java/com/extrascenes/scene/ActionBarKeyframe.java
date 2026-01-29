package com.extrascenes.scene;

import java.util.UUID;

public class ActionBarKeyframe extends Keyframe {
    private String text;
    private int durationTicks;

    public ActionBarKeyframe(UUID id, int timeTicks, String text, int durationTicks) {
        super(id, timeTicks);
        this.text = text;
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.ACTIONBAR;
    }

    @Override
    public String getType() {
        return "Actionbar";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }
}
