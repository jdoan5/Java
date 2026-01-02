package com.johndoan.jobtracker.persistence;

import com.johndoan.jobtracker.ApplicationRepository;
import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.JobApplication;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight integration test against a temp SQLite database file.
 */
public class JdbcApplicationRepositoryTest {

    @Test
    void insertAndQuery_work() throws Exception {
        Path dir = Files.createTempDirectory("jobtracker-test-");
        Path db = dir.resolve("job_tracker_test.db");

        JdbcApplicationRepository repo = new JdbcApplicationRepository(db);

        repo.save(new JobApplication("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1)));
        repo.save(new JobApplication("Beta", "Analyst", "NY", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 2)));

        var sort = ApplicationRepository.SortSpec.by(ApplicationRepository.SortField.ID, true);
        assertEquals(2, repo.findAll(sort).size());

        assertEquals(1, repo.search("Acme", sort).size());
        assertEquals(1, repo.findByStatus(ApplicationStatus.INTERVIEW, sort).size());

        // delete
        int idToDelete = repo.findAll(sort).get(0).getClass().getMethod("getId").invoke(repo.findAll(sort).get(0)) instanceof Number n ? n.intValue() : 1;
        assertTrue(repo.deleteById(idToDelete));
    }
}
