package com.johndoan.jobtracker.persistence;

import com.johndoan.jobtracker.ApplicationRepository;
import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.JobApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC/SQLite implementation.
 *
 * Notes:
 * - Uses a whitelisted sort mapping to avoid SQL injection.
 * - Reads schema from classpath resource: /schema.sql
 */
public class JdbcApplicationRepository implements ApplicationRepository {

    private static final String TABLE = "applications";

    private final Path dbPath;
    private final String jdbcUrl;

    public JdbcApplicationRepository(Path dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        ensureDriverLoaded();
        ensureDatabaseFileDir();
        initSchema();
    }

    private void ensureDriverLoaded() {
        // In most cases sqlite-jdbc auto-registers, but this avoids
        // "No suitable driver found" when classpath/shading is misconfigured.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // If missing, the next getConnection() call will fail with a clear message.
        }
    }

    private void ensureDatabaseFileDir() {
        try {
            Path parent = dbPath.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create database directory for: " + dbPath, e);
        }
    }

    private void initSchema() {
        String sql = readSchemaResource();
        if (sql == null || sql.trim().isEmpty()) {
            // Fallback: minimal schema
            sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "company TEXT NOT NULL, " +
                    "position TEXT NOT NULL, " +
                    "location TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "date_applied TEXT NOT NULL" +
                    ");";
        }

        try (Connection c = connect(); Statement st = c.createStatement()) {
            // schema.sql may include multiple statements separated by ';'
            for (String stmt : sql.split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) st.execute(s);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize schema. " +
                    "Check src/main/resources/schema.sql.", e);
        }
    }

    private String readSchemaResource() {
        try (var in = JdbcApplicationRepository.class.getResourceAsStream("/schema.sql")) {
            if (in == null) return null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public JobApplication save(JobApplication app) {
        String sql = "INSERT INTO " + TABLE + " (company, position, location, status, date_applied) VALUES (?, ?, ?, ?, ?)";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nullToEmpty(app.getCompany()));
            ps.setString(2, nullToEmpty(app.getPosition()));
            ps.setString(3, nullToEmpty(app.getLocation()));
            ps.setString(4, app.getStatus() != null ? app.getStatus().name() : ApplicationStatus.APPLIED.name());
            ps.setString(5, app.getDateApplied() != null ? app.getDateApplied().toString() : LocalDate.now().toString());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    assignIdIfPossible(app, id);
                }
            }

            return app;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert application into database.", e);
        }
    }

    @Override
    public List<JobApplication> findAll(SortSpec sort) {
        String sql = "SELECT id, company, position, location, status, date_applied FROM " + TABLE +
                orderBy(sort);
        return queryMany(sql, List.of());
    }

    @Override
    public List<JobApplication> findByStatus(ApplicationStatus status, SortSpec sort) {
        String sql = "SELECT id, company, position, location, status, date_applied FROM " + TABLE +
                " WHERE status = ?" + orderBy(sort);
        return queryMany(sql, List.of(status.name()));
    }

    @Override
    public List<JobApplication> search(String query, SortSpec sort) {
        String like = "%" + query.trim() + "%";
        String sql = "SELECT id, company, position, location, status, date_applied FROM " + TABLE +
                " WHERE company LIKE ? OR position LIKE ?" + orderBy(sort);
        return queryMany(sql, List.of(like, like));
    }

    @Override
    public List<JobApplication> search(String query, ApplicationStatus status, SortSpec sort) {
        String like = "%" + query.trim() + "%";
        String sql = "SELECT id, company, position, location, status, date_applied FROM " + TABLE +
                " WHERE (company LIKE ? OR position LIKE ?) AND status = ?" + orderBy(sort);
        return queryMany(sql, List.of(like, like, status.name()));
    }

    @Override
    public Optional<JobApplication> findById(int id) {
        String sql = "SELECT id, company, position, location, status, date_applied FROM " + TABLE + " WHERE id = ?";
        List<JobApplication> rows = queryMany(sql, List.of(String.valueOf(id)));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public boolean updateStatus(int id, ApplicationStatus newStatus) {
        String sql = "UPDATE " + TABLE + " SET status = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update status for id=" + id, e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM " + TABLE + " WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete application id=" + id, e);
        }
    }

    private List<JobApplication> queryMany(String sql, List<String> params) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<JobApplication> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database query failed.", e);
        }
    }

    private JobApplication mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String company = rs.getString("company");
        String position = rs.getString("position");
        String location = rs.getString("location");
        ApplicationStatus status = ApplicationStatus.valueOf(rs.getString("status"));
        LocalDate applied = LocalDate.parse(rs.getString("date_applied"));

        JobApplication app = new JobApplication(company, position, location, status, applied);
        assignIdIfPossible(app, id);
        return app;
    }

    private String orderBy(SortSpec sort) {
        SortSpec s = (sort != null) ? sort : SortSpec.defaultSort();
        String col = switch (s.field()) {
            case ID -> "id";
            case COMPANY -> "company";
            case POSITION -> "position";
            case LOCATION -> "location";
            case STATUS -> "status";
            case DATE_APPLIED -> "date_applied";
        };

        return " ORDER BY " + col + (s.ascending() ? " ASC" : " DESC");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void assignIdIfPossible(JobApplication app, int id) {
        try {
            Method m = JobApplication.class.getMethod("setId", int.class);
            m.invoke(app, id);
            return;
        } catch (Exception ignored) {
            // fall through
        }
        try {
            // As a fallback, if there's a field called `id`, set it reflectively.
            var f = JobApplication.class.getDeclaredField("id");
            f.setAccessible(true);
            f.setInt(app, id);
        } catch (Exception ignored) {
            // If neither exists, ID will remain default (0) and UI may show 0.
        }
    }
}
