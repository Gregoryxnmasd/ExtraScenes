package com.extrascenes.scene;

public enum SmoothingMode {
    NONE,
    LINEAR,
    SMOOTHSTEP,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EASE_IN_OUT_CUBIC,
    EASE_IN_OUT_QUINT,
    CATMULL_ROM

    ;

    public SmoothingMode next() {
        SmoothingMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
