package com.johndoan.jobtracker;

import java.io.IOException;
import java.nio.file.Path;
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
            maybe.get().setStatus(newStatus);
            return true;
        }
        return false;
    }

    public boolean deleteApplication(int id) {
        return repository.deleteById(id);
    }

    /** Load apps from CSV into the repository (and set default path for future saves). */
    public int loadApplicationsFromCsv(Path csvPath) throws IOException {
        repository.setCsvPath(csvPath);
        return repository.loadFromCsv(csvPath);
    }

    /** Save apps to CSV at the given path (and set default path for future saves). */
    public int exportApplicationsToCsv(Path csvPath) throws IOException {
        repository.setCsvPath(csvPath);
        return repository.exportToCsv(csvPath);
    }

    /** Save to repository default CSV path (throws if not configured). */
    public int exportApplicationsToDefaultCsv() throws IOException {
        return repository.exportToDefaultCsv();
    }
}