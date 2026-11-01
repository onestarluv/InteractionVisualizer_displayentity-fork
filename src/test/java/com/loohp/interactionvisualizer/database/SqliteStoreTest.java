package com.loohp.interactionvisualizer.database;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SqliteStoreTest {
    @TempDir Path temporary;

    @Test void renamedPluginCreatesDatabaseAndReopensExistingPreferences() throws Exception {
        Path folder = temporary.resolve("plugins/kokonut_ineteraction");
        try (var connection = SqliteStore.open(folder); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO USER_PERFERENCES (UUID, NAME) VALUES ('test-uuid', 'onestarluv')");
        }
        assertTrue(Files.isRegularFile(folder.resolve("database.db")));
        assertFalse(Files.exists(temporary.resolve("plugins/InteractionVisualizer")));
        try (var connection = SqliteStore.open(folder); var statement = connection.createStatement();
             var results = statement.executeQuery("SELECT NAME FROM USER_PERFERENCES WHERE UUID = 'test-uuid'")) {
            assertTrue(results.next());
            assertEquals("onestarluv", results.getString(1));
        }
    }

    @Test void unusableDirectoryFailsWithoutReturningANullConnection() throws Exception {
        Path file = temporary.resolve("not-a-directory");
        Files.writeString(file, "keep");
        assertThrows(java.io.IOException.class, () -> SqliteStore.open(file));
        assertEquals("keep", Files.readString(file));
    }
}
