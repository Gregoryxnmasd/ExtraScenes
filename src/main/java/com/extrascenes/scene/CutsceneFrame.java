package com.extrascenes.scene;

import org.bukkit.Location;

public class CutsceneFrame {
    private final Location location;
    private final int segmentIndex;
    private final boolean playerCamera;
    private final boolean allowPlayerLook;

    public CutsceneFrame(Location location, int segmentIndex, boolean playerCamera, boolean allowPlayerLook) {
        this.location = location;
        this.segmentIndex = segmentIndex;
        this.playerCamera = playerCamera;
        this.allowPlayerLook = allowPlayerLook;
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

    public boolean isAllowPlayerLook() {
        return allowPlayerLook;
    }
}
