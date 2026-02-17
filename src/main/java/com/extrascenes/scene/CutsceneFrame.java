package com.extrascenes.scene;

import org.bukkit.Location;

public class CutsceneFrame {
    private final Location location;
    private final int segmentIndex;
    private final boolean playerCamera;

    public CutsceneFrame(Location location, int segmentIndex, boolean playerCamera) {
        this.location = location;
        this.segmentIndex = segmentIndex;
        this.playerCamera = playerCamera;
    }

    public Location getLocation() {
        return location;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public boolean isPlayerCamera() {
        return playerCamera;
    }
}
