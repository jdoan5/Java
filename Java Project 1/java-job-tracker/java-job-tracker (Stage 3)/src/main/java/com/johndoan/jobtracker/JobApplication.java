package com.johndoan.jobtracker;

import java.time.LocalDate;

public class JobApplication {
    private int id; // 0 means "not assigned yet"

    private String company;
    private String position;
    private String location;
    private ApplicationStatus status;
    private LocalDate dateApplied;

    public JobApplication(String company, String position, String location,
                          ApplicationStatus status, LocalDate dateApplied) {
        this.company = company;
        this.position = position;
        this.location = location;
        this.status = status;
        this.dateApplied = dateApplied;
    }

    public int getId() { return id; }

    // Repository assigns this. Keep it public or package-private (no modifier).
    public void setId(int id) { this.id = id; }

    public String getCompany() { return company; }
    public String getPosition() { return position; }
    public String getLocation() { return location; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDate getDateApplied() { return dateApplied; }

    public void setStatus(ApplicationStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "#" + id + " | " + company + " - " + position + " (" + location + ")"
                + " | status=" + status
                + " | applied=" + (dateApplied != null ? dateApplied : "");
    }
}