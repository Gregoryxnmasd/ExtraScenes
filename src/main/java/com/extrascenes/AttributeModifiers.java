package com.extrascenes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;

public final class AttributeModifiers {
    private AttributeModifiers() {
    }

    public static AttributeModifier newModifier(NamespacedKey pluginKey, double amount,
                                                AttributeModifier.Operation operation,
                                                EquipmentSlotGroup slotGroup) {
        Objects.requireNonNull(pluginKey, "pluginKey");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(slotGroup, "slotGroup");

        AttributeModifier modifier = instantiate(pluginKey, amount, operation, slotGroup);
        if (modifier != null) {
            return modifier;
        }

        EquipmentSlot fallbackSlot = slotGroup == EquipmentSlotGroup.HEAD
                ? EquipmentSlot.HEAD
                : EquipmentSlot.HAND;
        modifier = instantiate(pluginKey, amount, operation, fallbackSlot);
        if (modifier != null) {
            return modifier;
        }

        try {
            Constructor<AttributeModifier> ctor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class);
            return ctor.newInstance(UUID.nameUUIDFromBytes(pluginKey.toString().getBytes()), pluginKey.toString(), amount, operation);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No compatible AttributeModifier constructor found", ex);
        }
    }

    public static AttributeModifier findModifier(AttributeInstance attribute, NamespacedKey key) {
        if (attribute == null || key == null) {
            return null;
        }
        Collection<AttributeModifier> modifiers = attribute.getModifiers();
        if (modifiers == null) {
            return null;
        }
        for (AttributeModifier modifier : modifiers) {
            if (hasKey(modifier, key)) {
                return modifier;
            }
        }
        return null;
    }

    public static boolean removeModifier(AttributeInstance attribute, NamespacedKey key) {
        AttributeModifier modifier = findModifier(attribute, key);
        if (modifier == null) {
            return false;
        }
        attribute.removeModifier(modifier);
        return true;
    }

    public static boolean hasKey(AttributeModifier modifier, NamespacedKey key) {
        return normalizeKey(extractKeyString(modifier)).equals(normalizeKey(key.toString()));
    }

    private static String extractKeyString(AttributeModifier modifier) {
        if (modifier == null) {
            return "";
        }
        try {
            Method getKey = AttributeModifier.class.getMethod("getKey");
            Object key = getKey.invoke(modifier);
            if (key != null) {
                return key.toString();
            }
        } catch (ReflectiveOperationException ignored) {
            // Older API variant.
        }
        try {
            Method getName = AttributeModifier.class.getMethod("getName");
            Object name = getName.invoke(modifier);
            return name == null ? "" : String.valueOf(name);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static AttributeModifier instantiate(NamespacedKey key, double amount,
                                                 AttributeModifier.Operation operation,
                                                 EquipmentSlotGroup group) {
        try {
            Constructor<AttributeModifier> ctor = AttributeModifier.class.getConstructor(
                    NamespacedKey.class, double.class, AttributeModifier.Operation.class, EquipmentSlotGroup.class);
            return ctor.newInstance(key, amount, operation, group);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<AttributeModifier> ctor = AttributeModifier.class.getConstructor(
                    Key.class, double.class, AttributeModifier.Operation.class, EquipmentSlotGroup.class);
            return ctor.newInstance(Key.key(key.getNamespace(), key.getKey()), amount, operation, group);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static AttributeModifier instantiate(NamespacedKey key, double amount,
                                                 AttributeModifier.Operation operation,
                                                 EquipmentSlot slot) {
        try {
            Constructor<AttributeModifier> ctor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class);
            return ctor.newInstance(UUID.nameUUIDFromBytes(key.toString().getBytes()), key.toString(), amount, operation, slot);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
