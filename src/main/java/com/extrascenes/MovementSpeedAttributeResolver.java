package com.extrascenes;

import org.bukkit.attribute.Attribute;

public final class MovementSpeedAttributeResolver {
    private static Attribute cachedMovementSpeed;

    private MovementSpeedAttributeResolver() {
    }

    public static Attribute resolveMovementSpeedAttribute() {
        if (cachedMovementSpeed != null) {
            return cachedMovementSpeed;
        }
        Attribute attribute = resolveAttribute("MOVEMENT_SPEED");
        if (attribute == null) {
            attribute = resolveAttribute("GENERIC_MOVEMENT_SPEED");
        }
        cachedMovementSpeed = attribute;
        return attribute;
    }

    private static Attribute resolveAttribute(String name) {
        try {
            return Attribute.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
