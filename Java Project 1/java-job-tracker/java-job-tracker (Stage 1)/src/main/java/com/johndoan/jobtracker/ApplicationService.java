package com.johndoan.jobtracker;

import java.io.IOException;
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
        JobApplication app = new JobApplication(
                company,
                position,
                location,
                status,
                dateApplied
        );
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
            // store is in-memory; if you had a real DB, you'd re-save here.
            return true;
        }
        return false;
    }

    /**
     * Export all applications to CSV using the repository's default path
     * (CSV/job_applications.csv).
     *
     * @return number of applications written
     */
    public int exportApplicationsToCsv() throws IOException {
        List<JobApplication> apps = repository.findAll();
        repository.exportToCsv(apps);
        return apps.size();
    }
}