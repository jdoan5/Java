package com.johndoan.jobtracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationRepository {

    private final List<JobApplication> applications = new ArrayList<>();

    public JobApplication save(JobApplication app) {
        applications.add(app);
        return app;
    }

    public List<JobApplication> findAll() {
        return new ArrayList<>(applications);
    }

    public List<JobApplication> findByStatus(ApplicationStatus status) {
        return applications.stream()
                .filter(app -> app.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Optional<JobApplication> findById(int id) {
        return applications.stream()
                .filter(app -> app.getId() == id)
                .findFirst();
    }
}