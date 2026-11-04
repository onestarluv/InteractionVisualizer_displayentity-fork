package com.loohp.interactionvisualizer;

import java.util.LinkedHashMap;
import java.util.Map;

final class CommandLanguage {
    static final Map<String, String> ACTIONS = Map.of("토글", "toggle", "새로고침", "refresh", "리로드", "reload", "설정창", "gui");
    static final Map<String, String> VALUES = Map.of("켜기", "true", "끄기", "false");
    static final Map<String, String> ENTRIES = new LinkedHashMap<>();
    static {
        for (KoreanEntry entry : KoreanEntry.values()) ENTRIES.put(entry.label(), entry.key());
        ENTRIES.put("전체", "all");
    }

    static String[] normalize(String[] input) {
        String[] args = input.clone();
        if (args.length == 0) return args;
        args[0] = ACTIONS.getOrDefault(args[0], args[0]);
        if (!args[0].equalsIgnoreCase("toggle")) return args;
        // Match the longest Korean entry name so spaced official names remain one argument.
        for (int end = args.length; end >= 2; end--) {
            if (args[end - 1].isEmpty()) continue;
            String entry = KoreanEntry.resolve(String.join(" ", java.util.Arrays.copyOfRange(args, 1, end)));
            if (entry == null) continue;
            String[] merged = new String[args.length - end + 2];
            merged[0] = args[0];
            merged[1] = entry;
            System.arraycopy(args, end, merged, 2, args.length - end);
            args = merged;
            break;
        }
        if (args.length > 2) args[2] = VALUES.getOrDefault(args[2], args[2]);
        return args;
    }

    record ToggleArguments(String entry, Boolean enabled, String player) {}

    static ToggleArguments toggleArguments(String[] args) {
        if (args.length < 2 || args.length > 4) throw new IllegalArgumentException();
        Boolean enabled = null;
        String player = null;
        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) enabled = Boolean.valueOf(args[2]);
            else if (args.length == 3) player = args[2];
            else throw new IllegalArgumentException();
        }
        if (args.length == 4) player = args[3];
        return new ToggleArguments(args[1], enabled, player);
    }
}
