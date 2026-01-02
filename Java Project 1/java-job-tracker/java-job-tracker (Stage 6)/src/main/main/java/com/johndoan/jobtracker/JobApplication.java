package com.johndoan.jobtracker;

import java.time.LocalDate;
import java.util.Objects;

public class JobApplication {
    private int id; // DB primary key
    private String company;
    private String position;
    private String location;
    private ApplicationStatus status;
    private LocalDate dateApplied;

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

    // For creating new rows (ID assigned by DB)
    public JobApplication(String company,
                          String position,
                          String location,
                          ApplicationStatus status,
                          LocalDate dateApplied) {
        this(0, company, position, location, status, dateApplied);
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
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
