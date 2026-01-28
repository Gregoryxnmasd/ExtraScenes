package com.extrascenes.scene;

import java.util.UUID;

public class SoundKeyframe extends Keyframe {
    private String soundId;
    private float volume;
    private float pitch;
    private Transform transform;

    public SoundKeyframe(UUID id, int timeTicks, String soundId, float volume, float pitch, Transform transform) {
        super(id, timeTicks);
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
        this.transform = transform;
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.SOUND;
    }

    @Override
    public String getType() {
        return "sound";
    }

    public String getSoundId() {
        return soundId;
    }

    public void setSoundId(String soundId) {
        this.soundId = soundId;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }
}
