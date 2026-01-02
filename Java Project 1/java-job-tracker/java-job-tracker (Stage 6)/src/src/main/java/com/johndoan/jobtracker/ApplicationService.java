package com.johndoan.jobtracker;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Thin service layer:
 * - input validation (light)
 * - orchestrates repository calls
 */
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

        if (company == null || company.trim().isEmpty()) {
            throw new IllegalArgumentException("Company is required.");
        }
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position is required.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        if (dateApplied == null) {
            dateApplied = LocalDate.now();
        }

        JobApplication app = new JobApplication(company.trim(), position.trim(), location.trim(), status, dateApplied);
        return repository.save(app);
    }

    public List<JobApplication> listAll(ApplicationRepository.SortSpec sort) {
        return repository.findAll(sort != null ? sort : ApplicationRepository.SortSpec.defaultSort());
    }

    public List<JobApplication> listByStatus(ApplicationStatus status, ApplicationRepository.SortSpec sort) {
        return repository.findByStatus(status, sort != null ? sort : ApplicationRepository.SortSpec.defaultSort());
    }

    /**
     * UI-friendly search:
     * - if query blank: returns listAll / listByStatus
     * - else: executes search (company OR position) and optional status filter
     */
    public List<JobApplication> search(String query,
                                       ApplicationStatus statusOrNull,
                                       ApplicationRepository.SortSpec sort) {

        var s = sort != null ? sort : ApplicationRepository.SortSpec.defaultSort();

        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (!hasQuery) {
            if (statusOrNull == null) return repository.findAll(s);
            return repository.findByStatus(statusOrNull, s);
        }

        String q = query.trim();
        if (statusOrNull == null) return repository.search(q, s);
        return repository.search(q, statusOrNull, s);
    }

    public boolean updateStatus(int id, ApplicationStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("Status is required.");
        return repository.updateStatus(id, newStatus);
    }

    public boolean deleteById(int id) {
        return repository.deleteById(id);
    }

    public Optional<JobApplication> findById(int id) {
        return repository.findById(id);
    }
}
