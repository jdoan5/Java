package com.johndoan.jobtracker;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain model for a single job application.
 */
public class JobApplication {

    private int id; // 0 means "not persisted yet"
    private String company;
    private String position;
    private String location;
    private ApplicationStatus status;
    private LocalDate dateApplied;

    /** Constructor for a new application (id assigned by repository). */
    public JobApplication(String company,
                          String position,
                          String location,
                          ApplicationStatus status,
                          LocalDate dateApplied) {
        this(0, company, position, location, status, dateApplied);
    }

    /** Constructor for a loaded/persisted application. */
    public JobApplication(int id,
                          String company,
                          String position,
                          String location,
                          ApplicationStatus status,
                          LocalDate dateApplied) {
        this.id = id;
        this.company = company;
        this.position = position;
        this.location = location;
        this.status = status;
        this.dateApplied = dateApplied;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public LocalDate getDateApplied() { return dateApplied; }
    public void setDateApplied(LocalDate dateApplied) { this.dateApplied = dateApplied; }

    @Override
    public String toString() {
        return "JobApplication{" +
                "id=" + id +
                ", company='" + company + '\'' +
                ", position='" + position + '\'' +
                ", location='" + location + '\'' +
                ", status=" + status +
                ", dateApplied=" + dateApplied +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobApplication that)) return false;

        // Prefer stable identity when available
        if (this.id != 0 && that.id != 0) {
            return this.id == that.id;
        }

        return Objects.equals(company, that.company) &&
                Objects.equals(position, that.position) &&
                Objects.equals(location, that.location) &&
                status == that.status &&
                Objects.equals(dateApplied, that.dateApplied);
    }

    @Override
    public int hashCode() {
        if (id != 0) return Integer.hashCode(id);
        return Objects.hash(company, position, location, status, dateApplied);
    }
}
