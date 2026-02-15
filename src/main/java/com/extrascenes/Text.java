package com.extrascenes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Text {
    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY_IN = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final LegacyComponentSerializer LEGACY_OUT = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {
    }

    public static Component colorize(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String normalized = withLegacyHex(input).replace('§', '&');
        return LEGACY_IN.deserialize(normalized);
    }

    public static String legacy(String input) {
        return LEGACY_OUT.serialize(colorize(input));
    }

    public static void send(CommandSender sender, String input) {
        sender.sendMessage(colorize(input));
    }

    private static String withLegacyHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(out, replacement.toString());
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
