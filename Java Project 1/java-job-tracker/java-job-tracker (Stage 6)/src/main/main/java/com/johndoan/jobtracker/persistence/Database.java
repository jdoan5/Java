package com.johndoan.jobtracker.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private final Path dbPath;

    public Database(Path dbPath) {
        this.dbPath = dbPath;
    }

    public Path getDbPath() {
        return dbPath;
    }

    public Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath.", e);
        }
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        return DriverManager.getConnection(url);
    }

    public void init() throws SQLException {
        try {
            Files.createDirectories(dbPath.toAbsolutePath().getParent());
        } catch (Exception e) {
            throw new SQLException("Failed to create DB directory: " + dbPath, e);
        }

        try (Connection c = connect()) {
            if (!tableExists(c, "applications")) {
                log.info("Initializing new database at {}", dbPath);
                runSchemaSql(c, "schema.sql");
            }
            try (Statement st = c.createStatement()) {
                st.execute("CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status)");
            }
        }
    }

    private static boolean tableExists(Connection c, String tableName) throws SQLException {
        try (ResultSet rs = c.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private static void runSchemaSql(Connection c, String resourceName) throws SQLException {
        String sql = readResource(resourceName);
        if (sql.trim().isEmpty()) {
            throw new SQLException("schema.sql is empty. Please add CREATE TABLE statements.");
        }
        try (Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static String readResource(String name) throws SQLException {
        InputStream in = Database.class.getClassLoader().getResourceAsStream(name);
        if (in == null) throw new SQLException("Resource not found on classpath: " + name);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            throw new SQLException("Failed to read resource: " + name, e);
        }
    }
}
