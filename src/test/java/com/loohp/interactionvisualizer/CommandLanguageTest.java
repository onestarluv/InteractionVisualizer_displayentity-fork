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
}
