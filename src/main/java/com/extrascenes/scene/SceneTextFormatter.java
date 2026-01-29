package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SceneTextFormatter {
    private SceneTextFormatter() {
    }

    public static String replacePlaceholders(ExtraScenesPlugin plugin, Player player, SceneSession session,
                                             String input, int durationTicks) {
        Location location = player.getLocation();
        String result = input
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{world}", player.getWorld().getName())
                .replace("{x}", String.valueOf(location.getX()))
                .replace("{y}", String.valueOf(location.getY()))
                .replace("{z}", String.valueOf(location.getZ()))
                .replace("{yaw}", String.valueOf(location.getYaw()))
                .replace("{pitch}", String.valueOf(location.getPitch()))
                .replace("{scene}", session.getScene().getName())
                .replace("{time}", String.valueOf(session.getTimeTicks()))
                .replace("{tick}", String.valueOf(session.getTimeTicks()))
                .replace("{duration}", String.valueOf(durationTicks))
                .replace("{progress}", String.valueOf(calculateProgress(session.getTimeTicks(), durationTicks)));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (NoClassDefFoundError ignored) {
                // PlaceholderAPI not present at runtime.
            }
        }
        return result;
    }

    public static String colorizeActionBar(String text) {
        if (text == null) {
            return "";
        }
        String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
        return applyHexColors(colored);
    }

    private static int calculateProgress(int timeTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 0;
        }
        return Math.min(100, Math.round((timeTicks / (float) durationTicks) * 100f));
    }

    private static String applyHexColors(String input) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#([A-Fa-f0-9]{6})").matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
