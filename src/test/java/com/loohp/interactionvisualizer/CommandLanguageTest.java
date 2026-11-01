package com.loohp.interactionvisualizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandLanguageTest {
    @Test void koreanToggleNormalizesWithoutChangingPlayerNameOrInput() {
        String[] input = {"토글", "벌통", "끄기", "켜기"};
        assertArrayEquals(new String[] {"toggle", "beehive", "false", "켜기"}, CommandLanguage.normalize(input));
        assertEquals("토글", input[0]);
    }

    @Test void englishCommandsAndThirdPartyEntriesRemainSupported() {
        String[] input = {"toggle", "custom:example", "true", "Steve"};
        assertArrayEquals(input, CommandLanguage.normalize(input));
    }

    @Test void allPreferencesAndAdministrativeCommandsTranslate() {
        assertArrayEquals(new String[] {"toggle", "all", "true"},
                CommandLanguage.normalize(new String[] {"토글", "전체", "켜기"}));
        assertArrayEquals(new String[] {"reload"}, CommandLanguage.normalize(new String[] {"리로드"}));
        assertArrayEquals(new String[] {"refresh"}, CommandLanguage.normalize(new String[] {"새로고침"}));
    }

    @Test void shortenedSyntaxSupportsImplicitToggleAndOptionalTarget() {
        var implicit = CommandLanguage.toggleArguments(CommandLanguage.normalize(new String[] {"토글", "숫돌"}));
        assertEquals("grindstone", implicit.entry());
        assertNull(implicit.enabled());
        assertNull(implicit.player());
        var target = CommandLanguage.toggleArguments(CommandLanguage.normalize(new String[] {"토글", "전체", "끄기", "Steve"}));
        assertEquals(Boolean.FALSE, target.enabled());
        assertEquals("Steve", target.player());
        assertEquals("Steve", CommandLanguage.toggleArguments(new String[] {"toggle", "all", "Steve"}).player());
    }

    @Test void malformedSyntaxIsRejectedRatherThanSilentlyDisablingEffects() {
        assertThrows(IllegalArgumentException.class, () -> CommandLanguage.toggleArguments(new String[] {"toggle"}));
        assertThrows(IllegalArgumentException.class, () -> CommandLanguage.toggleArguments(new String[] {"toggle", "all", "invalid", "Steve"}));
        assertThrows(IllegalArgumentException.class, () -> CommandLanguage.toggleArguments(new String[] {"toggle", "all", "all", "false", "Steve"}));
    }

    @Test void spacedOfficialNamesShiftOnlyTheEntryArguments() {
        assertArrayEquals(new String[] {"toggle", "enchantment_table", "false", "Steve"},
                CommandLanguage.normalize(new String[] {"토글", "마법", "부여대", "끄기", "Steve"}));
        assertArrayEquals(new String[] {"toggle", "smithing_table", "true"},
                CommandLanguage.normalize(new String[] {"토글", "대장장이", "작업대", "켜기"}));
        assertArrayEquals(new String[] {"toggle", "ender_chest", ""},
                CommandLanguage.normalize(new String[] {"토글", "엔더", "상자", ""}));
    }

    @Test void oldAliasesStillResolveButCanonicalLabelsMatchTheGame() {
        assertEquals("enchantment_table", KoreanEntry.resolve("인챈트대"));
        assertEquals("conduit", KoreanEntry.resolve("도관"));
        assertEquals("cartography_table", KoreanEntry.resolve("지도제작대"));
        assertEquals("전달체", KoreanEntry.correctDefaultLabel("interactionvisualizer:conduit", "도관"));
        assertEquals("마법 부여대", KoreanEntry.correctDefaultLabel("interactionvisualizer:enchantment_table", "인챈트대"));
        assertEquals("나만의 이름", KoreanEntry.correctDefaultLabel("interactionvisualizer:conduit", "나만의 이름"));
        assertEquals("barrel", KoreanEntry.resolve("통"));
    }
}
