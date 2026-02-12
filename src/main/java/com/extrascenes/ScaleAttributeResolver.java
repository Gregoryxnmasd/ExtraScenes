package com.extrascenes;

import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public final class ScaleAttributeResolver {
    private ScaleAttributeResolver() {
    }

    public static Attribute resolveScaleAttribute() {
        Attribute attribute = find("SCALE");
        if (attribute != null) {
            return attribute;
        }
        return find("GENERIC_SCALE");
    }

    private static Attribute find(String key) {
        try {
            return Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft(key.toLowerCase()));
        } catch (Exception ex) {
            return null;
        }
    }
}
