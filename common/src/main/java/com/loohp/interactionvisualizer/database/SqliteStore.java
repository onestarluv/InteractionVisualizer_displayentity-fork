package com.loohp.interactionvisualizer.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class SqliteStore {
    private SqliteStore() {}

    static Connection open(Path dataFolder) throws IOException, SQLException, ClassNotFoundException {
        Files.createDirectories(dataFolder);
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.resolve("database.db").toAbsolutePath());
        try {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS USER_PERFERENCES (UUID TEXT PRIMARY KEY, NAME TEXT NOT NULL, ITEMSTAND TEXT, ITEMDROP TEXT, HOLOGRAM TEXT)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS INDEX_MAPPING (ENTRY TEXT PRIMARY KEY, BITMASK_INDEX INTEGER)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS USE_LOCK (STATUS_LOCK BOOLEAN PRIMARY KEY)");
            }
            return connection;
        } catch (SQLException | RuntimeException failure) {
            try { connection.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }
}
