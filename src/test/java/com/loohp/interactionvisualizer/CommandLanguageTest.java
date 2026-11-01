package com.loohp.interactionvisualizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandLanguageTest {
    @Test void koreanToggleNormalizesWithoutChangingPlayerNameOrInput() {
        String[] input = {"토글", "홀로그램", "벌통", "끄기", "켜기"};
        assertArrayEquals(new String[] {"toggle", "hologram", "beehive", "false", "켜기"}, CommandLanguage.normalize(input));
        assertEquals("토글", input[0]);
    }

    @Test void englishCommandsAndThirdPartyEntriesRemainSupported() {
        String[] input = {"toggle", "itemstand", "custom:example", "true", "Steve"};
        assertArrayEquals(input, CommandLanguage.normalize(input));
    }

    @Test void allPreferencesAndAdministrativeCommandsTranslate() {
        assertArrayEquals(new String[] {"toggle", "all", "all", "true"},
                CommandLanguage.normalize(new String[] {"토글", "전체", "전체", "켜기"}));
        assertArrayEquals(new String[] {"reload"}, CommandLanguage.normalize(new String[] {"리로드"}));
        assertArrayEquals(new String[] {"refresh"}, CommandLanguage.normalize(new String[] {"새로고침"}));
    }
}
