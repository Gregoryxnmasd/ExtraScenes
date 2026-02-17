package com.extrascenes.command;

import com.extrascenes.scene.RecordingDurationUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ActorRecordArguments {
    private static final int DEFAULT_START_TICK = 1;
    private static final int DEFAULT_DURATION_TICKS = 15 * 20;

    private int startTick = DEFAULT_START_TICK;
    private int durationTicks = DEFAULT_DURATION_TICKS;
    private RecordingDurationUnit durationUnit = RecordingDurationUnit.SECONDS;
    private boolean previewOthers = true;
    private boolean startTickSet;
    private final List<String> warnings = new ArrayList<>();

    int startTick() {
        return startTick;
    }

    int durationTicks() {
        return durationTicks;
    }

    RecordingDurationUnit durationUnit() {
        return durationUnit;
    }

    boolean previewOthers() {
        return previewOthers;
    }

    List<String> warnings() {
        return warnings;
    }

    static ActorRecordArguments parse(String[] args, int startIndex) {
        ActorRecordArguments parsed = new ActorRecordArguments();
        for (int i = startIndex; i < args.length; i++) {
            String token = args[i].trim();
            if (token.isBlank()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (!parsed.startTickSet && lower.matches("\\d+")) {
                parsed.startTick = Math.max(1, Integer.parseInt(lower));
                parsed.startTickSet = true;
                continue;
            }
            if (lower.startsWith("start:")) {
                Integer resolved = parsePositiveInt(lower.substring("start:".length()));
                if (resolved == null) {
                    parsed.warnings.add("Invalid start tick token '" + token + "'.");
                } else {
                    parsed.startTick = resolved;
                    parsed.startTickSet = true;
                }
                continue;
            }
            if (lower.startsWith("preview:")) {
                String value = lower.substring("preview:".length());
                if (value.equals("on") || value.equals("true") || value.equals("yes")) {
                    parsed.previewOthers = true;
                } else if (value.equals("off") || value.equals("false") || value.equals("no")) {
                    parsed.previewOthers = false;
                } else {
                    parsed.warnings.add("Invalid preview token '" + token + "'; expected preview:on|off.");
                }
                continue;
            }
            String durationRaw = lower.startsWith("duration:")
                    ? lower.substring("duration:".length())
                    : lower;
            if (looksLikeDuration(durationRaw) || durationRaw.matches("\\d+")) {
                int[] duration = parseDurationTicks(durationRaw);
                if (duration == null) {
                    parsed.warnings.add("Invalid duration token '" + token + "'.");
                } else {
                    parsed.durationTicks = duration[0];
                    parsed.durationUnit = duration[1] == 1 ? RecordingDurationUnit.TICKS : RecordingDurationUnit.SECONDS;
                }
                continue;
            }
            parsed.warnings.add("Unknown recording option ignored: " + token);
        }
        return parsed;
    }

    private static Integer parsePositiveInt(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean looksLikeDuration(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        char suffix = value.charAt(value.length() - 1);
        return suffix == 's' || suffix == 't';
    }

    private static int[] parseDurationTicks(String raw) {
        try {
            if (raw.endsWith("t")) {
                return new int[]{Math.max(1, Integer.parseInt(raw.substring(0, raw.length() - 1))), 1};
            }
            if (raw.endsWith("s")) {
                return new int[]{Math.max(1, Integer.parseInt(raw.substring(0, raw.length() - 1)) * 20), 0};
            }
            return new int[]{Math.max(1, Integer.parseInt(raw) * 20), 0};
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
