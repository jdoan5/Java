package com.johndoan.jobtracker.persistence;

import com.johndoan.jobtracker.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JdbcApplicationRepository implements ApplicationRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcApplicationRepository.class);

    private final Database db;

    public JdbcApplicationRepository(Database db) {
        this.db = db;
    }

    @Override
    public JobApplication add(JobApplication app) {
        String sql = "INSERT INTO applications(company, position, location, status, date_applied) VALUES(?,?,?,?,?)";
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, app.getCompany());
            ps.setString(2, app.getPosition());
            ps.setString(3, app.getLocation());
            ps.setString(4, app.getStatus().name());
            ps.setString(5, app.getDateApplied().toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    app.setId(keys.getInt(1));
                }
            }
            return app;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert application.", e);
        }
    }

    @Override
    public boolean updateStatus(int id, ApplicationStatus newStatus) {
        String sql = "UPDATE applications SET status=? WHERE id=?";
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status.", e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM applications WHERE id=?";
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete application.", e);
        }
    }

    @Override
    public List<JobApplication> findAll() {
        return query("SELECT id, company, position, location, status, date_applied FROM applications ORDER BY id DESC", List.of());
    }

    @Override
    public List<JobApplication> findByStatus(ApplicationStatus status) {
        return query("SELECT id, company, position, location, status, date_applied FROM applications WHERE status=? ORDER BY id DESC",
                List.of(status.name()));
    }

    @Override
    public List<JobApplication> search(SearchFilter filter) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, company, position, location, status, date_applied FROM applications WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        String company = normalizeContains(filter.companyContains());
        if (company != null) {
            sql.append(" AND lower(company) LIKE ?");
            params.add(company);
        }
        String position = normalizeContains(filter.positionContains());
        if (position != null) {
            sql.append(" AND lower(position) LIKE ?");
            params.add(position);
        }
        String location = normalizeContains(filter.locationContains());
        if (location != null) {
            sql.append(" AND lower(location) LIKE ?");
            params.add(location);
        }
        if (filter.status() != null) {
            sql.append(" AND status = ?");
            params.add(filter.status().name());
        }
        if (filter.dateFrom() != null) {
            sql.append(" AND date_applied >= ?");
            params.add(filter.dateFrom().toString());
        }
        if (filter.dateTo() != null) {
            sql.append(" AND date_applied <= ?");
            params.add(filter.dateTo().toString());
        }

        SortField sortField = filter.sortField() == null ? SortField.ID : filter.sortField();
        SortDirection dir = filter.sortDirection() == null ? SortDirection.DESC : filter.sortDirection();

        sql.append(" ORDER BY ").append(sortField.column()).append(" ").append(dir.name());

        return query(sql.toString(), params);
    }

    @Override
    public void replaceAll(List<JobApplication> apps) {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM applications");
            }

            String insert = "INSERT INTO applications(company, position, location, status, date_applied) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                for (JobApplication a : apps) {
                    ps.setString(1, a.getCompany());
                    ps.setString(2, a.getPosition());
                    ps.setString(3, a.getLocation());
                    ps.setString(4, a.getStatus().name());
                    ps.setString(5, a.getDateApplied().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to replace applications.", e);
        }
    }

    private List<JobApplication> query(String sql, List<?> params) {
        List<JobApplication> out = new ArrayList<>();
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String companyR = rs.getString("company");
                    String positionR = rs.getString("position");
                    String locationR = rs.getString("location");
                    ApplicationStatus statusR = ApplicationStatus.valueOf(rs.getString("status"));
                    LocalDate dateApplied = LocalDate.parse(rs.getString("date_applied"));
                    out.add(new JobApplication(id, companyR, positionR, locationR, statusR, dateApplied));
                }
            }
            return out;

        } catch (SQLException e) {
            log.error("SQL error running query: {}", sql, e);
            throw new RuntimeException("Database query failed.", e);
        }
    }

    private static String normalizeContains(String v) {
        if (v == null) return null;
        String s = v.trim().toLowerCase();
        if (s.isEmpty()) return null;
        return "%" + s + "%";
    }
}
