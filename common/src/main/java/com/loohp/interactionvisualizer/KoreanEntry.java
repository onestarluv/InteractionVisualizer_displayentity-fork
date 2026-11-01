package com.loohp.interactionvisualizer;

/** Names checked against Mojang's Minecraft 26.3 ko_kr language asset. */
public enum KoreanEntry {
    CRAFTING_TABLE("crafting_table", "제작대"), CRAFTER("crafter", "제작기"), LOOM("loom", "베틀"),
    ENCHANTMENT_TABLE("enchantment_table", "마법 부여대", "인챈트대", "인첸트대"),
    CARTOGRAPHY_TABLE("cartography_table", "지도 제작대"), ANVIL("anvil", "모루"), GRINDSTONE("grindstone", "숫돌"),
    STONECUTTER("stonecutter", "석재 절단기"), BREWING_STAND("brewing_stand", "양조기"),
    CHEST("chest", "상자"), DOUBLE_CHEST("double_chest", "큰 상자"), BARREL("barrel", "통", "배럴"),
    FURNACE("furnace", "화로"), BLAST_FURNACE("blast_furnace", "용광로"), SMOKER("smoker", "훈연기"),
    ENDER_CHEST("ender_chest", "엔더 상자"), SHULKER_BOX("shulker_box", "셜커 상자"),
    DISPENSER("dispenser", "발사기"), DROPPER("dropper", "공급기"), HOPPER("hopper", "호퍼"), BEACON("beacon", "신호기"),
    NOTE_BLOCK("note_block", "소리 블록"), JUKEBOX("jukebox", "주크박스"), SMITHING_TABLE("smithing_table", "대장장이 작업대"),
    BEE_NEST("bee_nest", "벌집"), BEEHIVE("beehive", "벌통"), LECTERN("lectern", "독서대"),
    CAMPFIRE("campfire", "모닥불"), SOUL_CAMPFIRE("soul_campfire", "영혼 모닥불"),
    SPAWNER("spawner", "몬스터 생성기", "생성기", "스포너"), CONDUIT("conduit", "전달체", "도관"),
    BANNER("banner", "현수막"), ITEM("item", "아이템"), VILLAGER("villager", "주민"), BOOKSHELF("bookshelf:bookshelf", "책장");

    private final String key;
    private final String label;
    private final String[] aliases;
    KoreanEntry(String key, String label, String... aliases) { this.key = key; this.label = label; this.aliases = aliases; }
    public String key() { return key; }
    public String label() { return label; }
    public boolean matches(String input) {
        String compact = input.replaceAll("\\s+", "");
        if (label.replace(" ", "").equals(compact)) return true;
        for (String alias : aliases) if (alias.equals(compact)) return true;
        return false;
    }
    public static String resolve(String input) {
        if (input.equals("전체")) return "all";
        for (KoreanEntry entry : values()) if (entry.matches(input)) return entry.key;
        return null;
    }
    public static String correctDefaultLabel(String key, String configured) {
        for (KoreanEntry entry : values()) {
            String fullKey = entry.key.contains(":") ? entry.key : "interactionvisualizer:" + entry.key;
            if (fullKey.equals(key) && (entry.matches(configured) || configured.equals(fullKey))) return entry.label;
        }
        return configured;
    }
}
