package com.johndoan.jobtracker;

import java.util.List;
import java.util.Optional;

/**
 * Persistence abstraction for JobApplication records.
 * Stage 4 uses a JDBC implementation backed by SQLite.
 */
public interface ApplicationRepository {

    JobApplication save(JobApplication application);

    List<JobApplication> findAll();

    List<JobApplication> findByStatus(ApplicationStatus status);

    Optional<JobApplication> findById(int id);

    /**
     * Update an existing record (matched by id).
     *
     * @return true if a row was updated
     */
    boolean update(JobApplication application);

    /**
     * Delete a record by id.
     *
     * @return true if a row was deleted
     */
    boolean deleteById(int id);

    /** Remove all records (used for CSV import). */
    void deleteAll();
}
