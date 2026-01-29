package com.extrascenes.scene;

public enum SmoothingMode {
    INSTANT,
    LINEAR,
    SMOOTH;

    public SmoothingMode next() {
        SmoothingMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
