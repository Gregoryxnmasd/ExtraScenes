package com.extrascenes.scene;

public enum RecordingDurationUnit {
    SECONDS,
    TICKS;

    public String suffix() {
        return this == SECONDS ? "s" : "t";
    }

    public int toTicks(int value) {
        int bounded = Math.max(1, value);
        return this == SECONDS ? bounded * 20 : bounded;
    }
}

