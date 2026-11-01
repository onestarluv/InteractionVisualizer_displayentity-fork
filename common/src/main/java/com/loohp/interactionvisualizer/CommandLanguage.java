package com.loohp.interactionvisualizer;

import java.util.LinkedHashMap;
import java.util.Map;

final class CommandLanguage {
    static final Map<String, String> ACTIONS = Map.of("토글", "toggle", "새로고침", "refresh", "리로드", "reload", "업데이트", "update");
    static final Map<String, String> VALUES = Map.of("켜기", "true", "끄기", "false");
    static final Map<String, String> ENTRIES = new LinkedHashMap<>();
    static {
        String[][] entries = {
            {"제작대", "crafting_table"}, {"제작기", "crafter"}, {"베틀", "loom"}, {"인챈트대", "enchantment_table"},
            {"지도제작대", "cartography_table"}, {"모루", "anvil"}, {"숫돌", "grindstone"}, {"석재절단기", "stonecutter"},
            {"양조기", "brewing_stand"}, {"상자", "chest"}, {"큰상자", "double_chest"}, {"화로", "furnace"},
            {"용광로", "blast_furnace"}, {"훈연기", "smoker"}, {"엔더상자", "ender_chest"}, {"셜커상자", "shulker_box"},
            {"발사기", "dispenser"}, {"공급기", "dropper"}, {"호퍼", "hopper"}, {"신호기", "beacon"},
            {"소리블록", "note_block"}, {"주크박스", "jukebox"}, {"대장장이작업대", "smithing_table"},
            {"벌집", "bee_nest"}, {"벌통", "beehive"}, {"독서대", "lectern"}, {"모닥불", "campfire"},
            {"영혼모닥불", "soul_campfire"}, {"생성기", "spawner"}, {"도관", "conduit"},
            {"현수막", "banner"}, {"아이템", "item"}, {"주민", "villager"}, {"책장", "bookshelf:bookshelf"}
        };
        for (String[] entry : entries) ENTRIES.put(entry[0], entry[1]);
        ENTRIES.put("전체", "all");
    }

    static String[] normalize(String[] input) {
        String[] args = input.clone();
        if (args.length == 0) return args;
        args[0] = ACTIONS.getOrDefault(args[0], args[0]);
        if (!args[0].equalsIgnoreCase("toggle")) return args;
        if (args.length > 1) args[1] = ENTRIES.getOrDefault(args[1], args[1]);
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
