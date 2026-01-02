package com.johndoan.jobtracker;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for job applications.
 *
 * Stage 5 adds search + sorting support so the UI can filter quickly
 * without loading everything into memory.
 */
public interface ApplicationRepository {

    /**
     * Safe, whitelisted sort fields. These map to real database columns.
     */
    enum SortField {
        ID,
        COMPANY,
        POSITION,
        LOCATION,
        STATUS,
        DATE_APPLIED
    }

    /**
     * Sorting configuration passed from service/UI to repository.
     */
    record SortSpec(SortField field, boolean ascending) {
        public static SortSpec by(SortField field, boolean ascending) {
            return new SortSpec(field, ascending);
        }

        public static SortSpec defaultSort() {
            // Most recent first (higher id first) tends to feel best in the UI.
            return new SortSpec(SortField.ID, false);
        }
    }

    JobApplication save(JobApplication app);

    List<JobApplication> findAll(SortSpec sort);

    List<JobApplication> findByStatus(ApplicationStatus status, SortSpec sort);

    /**
     * Search by company OR position. The repository decides how to implement it (SQL LIKE, FTS, etc).
     */
    List<JobApplication> search(String query, SortSpec sort);

    /**
     * Search + status filter combined (recommended path for UI).
     */
    List<JobApplication> search(String query, ApplicationStatus status, SortSpec sort);

    Optional<JobApplication> findById(int id);

    boolean updateStatus(int id, ApplicationStatus newStatus);

    boolean deleteById(int id);
}
