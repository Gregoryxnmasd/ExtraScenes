package com.extrascenes.scene;

public enum SmoothingQuality {
    NORMAL(1, false, false, 0.0f),
    SMOOTH(2, true, false, 0.0f),
    ULTRA(8, true, true, 0.35f);

    private final int subSamples;
    private final boolean splinePosition;
    private final boolean lookAhead;
    private final float lookAheadTicks;

    SmoothingQuality(int subSamples, boolean splinePosition, boolean lookAhead, float lookAheadTicks) {
        this.subSamples = subSamples;
        this.splinePosition = splinePosition;
        this.lookAhead = lookAhead;
        this.lookAheadTicks = lookAheadTicks;
    }

    public int getSubSamples() {
        return subSamples;
    }

    public boolean isSplinePosition() {
        return splinePosition;
    }

    public boolean isLookAhead() {
        return lookAhead;
    }

    public float getLookAheadTicks() {
        return lookAheadTicks;
    }

    public SmoothingQuality next() {
        SmoothingQuality[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
