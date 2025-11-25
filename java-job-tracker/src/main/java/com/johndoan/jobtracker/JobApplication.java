package com.johndoan.jobtracker;

import java.time.LocalDate;

public class JobApplication {
    private static int nextId = 1;

    private final int id;
    private String company;
    private String position;
    private String location;
    private ApplicationStatus status;
    private LocalDate dataApplied;

    public JobApplication(String company, String position, String location, ApplicationStatus status, LocalDate dataApplied) {
        this.id = nextId++;
        this.company = company;
        this.position = position;
        this.location = location;
        this.status = status;
        this.dataApplied = dataApplied;
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

    public LocalDate getDataApplied() {
        return dataApplied;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
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
                dataApplied
        );
    }
    }





