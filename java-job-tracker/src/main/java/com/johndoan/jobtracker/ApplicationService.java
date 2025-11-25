package com.johndoan.jobtracker;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public JobApplication addApplication(String company,
                                         String position,
                                         String location,
                                         ApplicationStatus status,
                                         LocalDate dateApplied) {
        JobApplication app = new JobApplication(company, position, location, status, dateApplied);
        return repository.save(app);
    }

    public List<JobApplication> listAll() {
        return repository.findAll();
    }

    public List<JobApplication> listByStatus(ApplicationStatus status) {
        return repository.findByStatus(status);
    }

    public boolean updateStatus(int id, ApplicationStatus newStatus) {
        Optional<JobApplication> maybe = repository.findById(id);
        if (maybe.isPresent()) {
            JobApplication app = maybe.get();
            app.setStatus(newStatus);
            return true;
        }
        return false;
    }
}