package com.johndoan.jobtracker.persistence;

import com.johndoan.jobtracker.ApplicationRepository;
import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.persistence.Database;
import com.johndoan.jobtracker.JobApplication;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository backed by SQLite.
 */
public class JdbcApplicationRepository implements ApplicationRepository {

    private final Database database;

    public JdbcApplicationRepository(Database database) {
        this.database = database;
    }

    @Override
    public JobApplication save(JobApplication application) {
        final String sql = "INSERT INTO applications(company, position, location, status, date_applied) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nullToEmpty(application.getCompany()));
            ps.setString(2, nullToEmpty(application.getPosition()));
            ps.setString(3, nullToEmpty(application.getLocation()));
            ps.setString(4, application.getStatus().name());
            ps.setString(5, application.getDateApplied().toString());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    application.setId(keys.getInt(1));
                }
            }
            return application;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert application", e);
        }
    }

    @Override
    public List<JobApplication> findAll() {
        final String sql = "SELECT id, company, position, location, status, date_applied FROM applications ORDER BY date_applied DESC, id DESC";
        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<JobApplication> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query applications", e);
        }
    }

    @Override
    public List<JobApplication> findByStatus(ApplicationStatus status) {
        final String sql = "SELECT id, company, position, location, status, date_applied FROM applications WHERE status = ? ORDER BY date_applied DESC, id DESC";
        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                List<JobApplication> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query applications by status", e);
        }
    }

    @Override
    public Optional<JobApplication> findById(int id) {
        final String sql = "SELECT id, company, position, location, status, date_applied FROM applications WHERE id = ?";
        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query application by id", e);
        }
    }

    @Override
    public boolean update(JobApplication application) {
        if (application.getId() <= 0) return false;

        final String sql = "UPDATE applications SET company = ?, position = ?, location = ?, status = ?, date_applied = ? WHERE id = ?";
        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nullToEmpty(application.getCompany()));
            ps.setString(2, nullToEmpty(application.getPosition()));
            ps.setString(3, nullToEmpty(application.getLocation()));
            ps.setString(4, application.getStatus().name());
            ps.setString(5, application.getDateApplied().toString());
            ps.setInt(6, application.getId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update application id=" + application.getId(), e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        final String sql = "DELETE FROM applications WHERE id = ?";
        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete application id=" + id, e);
        }
    }

    @Override
    public void deleteAll() {
        try (Connection conn = database.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM applications");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all applications", e);
        }
    }

    private JobApplication mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String company = rs.getString("company");
        String position = rs.getString("position");
        String location = rs.getString("location");
        ApplicationStatus status = ApplicationStatus.valueOf(rs.getString("status"));
        LocalDate dateApplied = LocalDate.parse(rs.getString("date_applied"));

        return new JobApplication(id, company, position, location, status, dateApplied);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
