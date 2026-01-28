package com.extrascenes.scene;

import java.util.UUID;
import org.bukkit.Material;

public class BlockIllusionKeyframe extends Keyframe {
    private Material material;
    private Transform transform;

    public BlockIllusionKeyframe(UUID id, int timeTicks, Material material, Transform transform) {
        super(id, timeTicks);
        this.material = material == null ? Material.BARRIER : material;
        this.transform = transform;
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.BLOCK_ILLUSION;
    }

    @Override
    public String getType() {
        return "block_illusion";
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }
}
