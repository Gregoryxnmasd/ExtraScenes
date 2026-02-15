package com.extrascenes;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

public final class KeyConversionUtil {
    private KeyConversionUtil() {
    }

    public static Key toAdventureKey(NamespacedKey key) {
        if (key == null) {
            return null;
        }
        return Key.key(key.getNamespace(), key.getKey());
    }

    public static NamespacedKey toBukkitKey(Key key) {
        if (key == null) {
            return null;
        }
        return new NamespacedKey(key.namespace(), key.value());
    }
}
