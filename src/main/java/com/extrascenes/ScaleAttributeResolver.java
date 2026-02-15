package com.extrascenes;

import org.bukkit.attribute.Attribute;

public final class ScaleAttributeResolver {
    private static Attribute cachedScale;
    private static boolean scaleResolved;

    private ScaleAttributeResolver() {
    }

    public static Attribute resolveScaleAttribute() {
        if (scaleResolved) {
            return cachedScale;
        }

        Attribute attribute = resolveAttribute("SCALE");
        if (attribute == null) {
            attribute = resolveAttribute("GENERIC_SCALE");
        }

        cachedScale = attribute;
        scaleResolved = true;
        return cachedScale;
    }

    private static Attribute resolveAttribute(String name) {
        try {
            return Attribute.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
