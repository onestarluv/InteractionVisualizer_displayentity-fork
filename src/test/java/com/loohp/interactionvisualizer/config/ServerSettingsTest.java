package com.loohp.interactionvisualizer.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ServerSettingsTest {
    @TempDir Path directory;

    @Test void everyEnumValueIsAvailableInSharedDefaults() {
        for (ServerSetting setting : ServerSetting.values()) {
            assertEquals(setting.value(), ServerSettings.INSTANCE.get(setting.path()), setting.path());
        }
        assertEquals(3306, ServerSettings.INSTANCE.getInt("Database.MYSQL.Port"));
        assertFalse(ServerSettings.INSTANCE.getBoolean("Settings.DefaultDisableAll"));
    }

    @Test void nestedInputsAreDefensivelyCopied() {
        List<Object> list = new ArrayList<>(List.of("initial"));
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        ServerSettings settings = new ServerSettings(map);
        list.add("changed");
        map.clear();
        assertEquals(List.of("initial"), settings.getList("list"));
        assertThrows(UnsupportedOperationException.class, () -> settings.values().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.getList("list").clear());
    }

    @Test void existingFileOverridesDefaultsWithoutBeingRewritten() throws Exception {
        Path file = directory.resolve("config.yml");
        String yaml = "Settings:\n  DefaultDisableAll: true\nMessages:\n  NoPermission: '직접 작성한 문구'\nCustom:\n  Value: 42\n";
        Files.writeString(file, yaml);
        ServerSettings settings = ServerSettings.importLegacy(file.toFile());
        assertTrue(settings.getBoolean("Settings.DefaultDisableAll"));
        assertEquals("직접 작성한 문구", settings.getString("Messages.NoPermission"));
        assertEquals(42, settings.getInt("Custom.Value"));
        assertEquals(yaml, Files.readString(file));
    }

    @Test void missingLegacyFileUsesSharedDefaultsAndSectionsRemainAvailable() throws Exception {
        assertSame(ServerSettings.INSTANCE, ServerSettings.importLegacy(directory.resolve("missing.yml").toFile()));
        assertTrue(ServerSettings.INSTANCE.isConfigurationSection("Messages.ModuleName"));
        assertEquals("홀로그램", ServerSettings.INSTANCE.getConfigurationSection("Messages.ModuleName").getString("hologram"));
    }
}
