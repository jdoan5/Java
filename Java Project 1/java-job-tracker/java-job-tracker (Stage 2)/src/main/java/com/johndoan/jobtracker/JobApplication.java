package com.johndoan.jobtracker;

import java.time.LocalDate;

public class JobApplication {

    private int id;  // assigned by repository
    private final String company;
    private final String position;
    private final String location;
    private ApplicationStatus status;
    private final LocalDate dateApplied;

    public JobApplication(String company,
                          String position,
                          String location,
                          ApplicationStatus status,
                          LocalDate dateApplied) {
        this.company = company;
        this.position = position;
        this.location = location;
        this.status = status;
        this.dateApplied = dateApplied;
    }

    public int getId() {
        return id;
    }

    // used by ApplicationRepository when assigning IDs
    public void setId(int id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public String getPosition() {
        return position;
    }

    public String getLocation() {
        return location;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getDateApplied() {
        return dateApplied;
    }

    // Backwards-compatible alias (you had getDataApplied earlier)
    public LocalDate getDataApplied() {
        return dateApplied;
    }

    @Override
    public String toString() {
        return "#" + id + " | " + company + " - " + position +
                " | status=" + status +
                " | applied=" + (dateApplied != null ? dateApplied : "unknown");
    }
}