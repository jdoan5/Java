package com.johndoan.jobtracker;

import java.time.LocalDate;

public class JobApplication {

    private static int nextId = 1;

    private final int id;
    private String company;
    private String position;
    private String location;
    private ApplicationStatus status;
    private LocalDate appliedDate;   // renamed for clarity
    private String notes;            // optional notes for CSV / future use

    // Existing constructor (no notes) – keeps your current code working
    public JobApplication(String company,
                          String position,
                          String location,
                          ApplicationStatus status,
                          LocalDate appliedDate) {
        this(company, position, location, status, appliedDate, "");
    }

    // Overloaded constructor with notes (optional, for future features)
    public JobApplication(String company,
                          String position,
                          String location,
                          ApplicationStatus status,
                          LocalDate appliedDate,
                          String notes) {
        this.id = nextId++;
        this.company = company;
        this.position = position;
        this.location = location;
        this.status = status;
        this.appliedDate = appliedDate;
        this.notes = notes == null ? "" : notes;
    }

    public int getId() {
        return id;
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

    // New getter used by CSV export
    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    // Backwards-compat alias (if any old code still calls getDataApplied)
    public LocalDate getDataApplied() {
        return appliedDate;
    }

    // New getter used by CSV export
    public String getNotes() {
        return notes;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    @Override
    public String toString() {
        return String.format(
                "#%d | %s - %s (%s) | status=%s | applied=%s",
                id,
                company,
                position,
                location,
                status,
                appliedDate
        );
    }
}