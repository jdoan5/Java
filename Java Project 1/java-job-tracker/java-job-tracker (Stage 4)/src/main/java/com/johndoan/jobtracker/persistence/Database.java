package com.johndoan.jobtracker.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite bootstrap + connection helper.
 *
 * The database file is created automatically if it does not exist.
 */
public final class Database {

    private final String jdbcUrl;

    public Database(Path dbPath) {
        // SQLite JDBC uses the file path directly in the JDBC URL
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Create tables/indexes if they do not exist.
     */
    public void init() throws SQLException {
        String createTable = ""
                + "CREATE TABLE IF NOT EXISTS applications ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " company TEXT NOT NULL,"
                + " position TEXT NOT NULL,"
                + " location TEXT NOT NULL,"
                + " status TEXT NOT NULL,"
                + " date_applied TEXT NOT NULL"
                + ");";

        String createIndex = ""
                + "CREATE INDEX IF NOT EXISTS idx_applications_status "
                + "ON applications(status);";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTable);
            stmt.executeUpdate(createIndex);
        }
    }
}
