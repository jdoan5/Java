package com.johndoan.jobtracker.persistence;

import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.JobApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that run against a real SQLite file.
 *
 * {@link TempDir} gives every test method its own throwaway directory, so the suite
 * never touches the developer's real database at {@code ~/.jobtracker/job_tracker.db}
 * and leaves nothing behind in the working tree.
 */
public class JdbcApplicationRepositoryTest {

    /**
     * The one place the database file name is written down. Tests that need to re-open the
     * same file share {@link #dbFile} rather than re-deriving the name, so the two can never
     * drift apart and leave a test silently pointing at a different (empty) database.
     */
    private static final String DB_FILENAME = "job_tracker_test.db";

    @TempDir
    Path tempDir;

    /** The file {@link #setUp} created -- shared with every test that re-opens it. */
    private Path dbFile;
    private Database database;
    private JdbcApplicationRepository repo;

    @BeforeEach
    void setUp() throws SQLException {
        dbFile = tempDir.resolve(DB_FILENAME);
        database = new Database(dbFile);
        database.init();

        assertTrue(Files.exists(dbFile), "init() should create the SQLite file");
        repo = new JdbcApplicationRepository(database);
    }

    /**
     * Inserts a row with an explicitly chosen id, bypassing the repository. Used to put a real
     * row at an id the repository's own {@code save} would never hand out.
     */
    private void insertRowWithExplicitId(int id, String company) throws SQLException {
        final String sql = "INSERT INTO applications(id, company, position, location, status, date_applied)"
                + " VALUES (?, ?, 'Keeper', 'Nowhere', 'APPLIED', '2026-01-01')";
        try (Connection conn = database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, company);
            assertEquals(1, ps.executeUpdate(), "fixture insert at id " + id + " should affect one row");
        }
    }

    private JobApplication save(String company, String position, String location,
                                ApplicationStatus status, LocalDate applied) {
        return repo.save(new JobApplication(company, position, location, status, applied));
    }

    @Test
    void saveAssignsTheGeneratedIdToThePassedObject() {
        JobApplication app = new JobApplication("Acme", "Developer", "Remote",
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        assertEquals(0, app.getId(), "a new application starts unpersisted");

        JobApplication saved = repo.save(app);

        assertSame(app, saved, "save returns the same instance it was handed");
        assertTrue(saved.getId() > 0, "SQLite should hand back an AUTOINCREMENT id");
    }

    @Test
    void findAllOrdersByDateAppliedThenIdDescending() {
        save("Oldest", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        save("SameDayFirst", "Analyst", "NY", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 3));
        save("SameDaySecond", "Tester", "LA", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 3));
        save("Middle", "Manager", "SF", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 2));

        List<JobApplication> all = repo.findAll();

        assertEquals(4, all.size());
        // Newest date first; within one date the higher id (most recently entered) wins.
        assertEquals(List.of("SameDaySecond", "SameDayFirst", "Middle", "Oldest"),
                all.stream().map(JobApplication::getCompany).toList());
    }

    @Test
    void findAllMapsEveryColumnBack() {
        save("Acme", "Developer", "Remote", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 1));

        JobApplication row = repo.findAll().get(0);

        assertTrue(row.getId() > 0);
        assertEquals("Acme", row.getCompany());
        assertEquals("Developer", row.getPosition());
        assertEquals("Remote", row.getLocation());
        assertEquals(ApplicationStatus.INTERVIEW, row.getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), row.getDateApplied());
    }

    @Test
    void findByStatusReturnsOnlyRowsWithThatStatus() {
        save("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        save("Beta", "Analyst", "NY", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 2));
        save("Gamma", "Tester", "LA", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 3));

        List<JobApplication> interviews = repo.findByStatus(ApplicationStatus.INTERVIEW);

        assertEquals(2, interviews.size());
        assertTrue(interviews.stream().allMatch(a -> a.getStatus() == ApplicationStatus.INTERVIEW));
        assertEquals(List.of("Gamma", "Beta"),
                interviews.stream().map(JobApplication::getCompany).toList());

        assertEquals(1, repo.findByStatus(ApplicationStatus.APPLIED).size());
        assertTrue(repo.findByStatus(ApplicationStatus.REJECTED).isEmpty());
    }

    @Test
    void findByIdReturnsTheRowOrEmpty() {
        JobApplication saved = save("Acme", "Developer", "Remote",
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        Optional<JobApplication> found = repo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Acme", found.get().getCompany());

        assertTrue(repo.findById(saved.getId() + 999).isEmpty());
    }

    @Test
    void updatePersistsEveryEditableField() {
        JobApplication saved = save("Acme", "Developer", "Remote",
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        saved.setCompany("Acme Corp");
        saved.setPosition("Senior Developer");
        saved.setLocation("NY");
        saved.setStatus(ApplicationStatus.OFFER);
        saved.setDateApplied(LocalDate.of(2026, 2, 5));

        assertTrue(repo.update(saved));

        JobApplication reloaded = repo.findById(saved.getId()).orElseThrow();
        assertEquals("Acme Corp", reloaded.getCompany());
        assertEquals("Senior Developer", reloaded.getPosition());
        assertEquals("NY", reloaded.getLocation());
        assertEquals(ApplicationStatus.OFFER, reloaded.getStatus());
        assertEquals(LocalDate.of(2026, 2, 5), reloaded.getDateApplied());

        // Still a single row -- an update must not insert.
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void updateReturnsFalseForUnpersistedOrUnknownRows() {
        JobApplication never = new JobApplication("Ghost", "Nobody", "Nowhere",
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        // An id of 0 means "never persisted": update reports failure.
        // (This says nothing about *how* -- see updateShortCircuitsOnNonPositiveIds below.)
        assertFalse(repo.update(never));

        // A plausible-looking id that is simply not in the table.
        never.setId(4242);
        assertFalse(repo.update(never));

        assertTrue(repo.findAll().isEmpty(), "a failed update must not create a row");
    }

    /**
     * Pins down the {@code if (application.getId() <= 0) return false;} guard in
     * {@code update}, which is otherwise invisible: with an empty table,
     * {@code UPDATE ... WHERE id = 0} matches zero rows and returns false anyway, so the
     * assertion above holds with or without the guard.
     *
     * <p>Planting real rows at id 0 and id -1 makes the difference observable. With the guard,
     * those rows come back byte-for-byte unchanged. Without it, the UPDATE would find them and
     * overwrite every column -- and would then return true, because one row was affected.
     */
    @Test
    void updateShortCircuitsOnNonPositiveIds() throws SQLException {
        insertRowWithExplicitId(0, "SentinelZero");
        insertRowWithExplicitId(-1, "SentinelNegative");

        assertEquals("SentinelZero", repo.findById(0).orElseThrow().getCompany(),
                "precondition: a row really is sitting at id 0");
        assertEquals("SentinelNegative", repo.findById(-1).orElseThrow().getCompany(),
                "precondition: a row really is sitting at id -1");

        JobApplication overwriter = new JobApplication("Ghost", "Nobody", "Nowhere",
                ApplicationStatus.OFFER, LocalDate.of(2026, 9, 9));
        assertEquals(0, overwriter.getId(), "a new application starts unpersisted at id 0");

        assertFalse(repo.update(overwriter), "id 0 must not reach the UPDATE");
        assertUntouched(0, "SentinelZero");

        overwriter.setId(-1);
        assertFalse(repo.update(overwriter), "a negative id must not reach the UPDATE");
        assertUntouched(-1, "SentinelNegative");
    }

    /** Asserts the planted row is exactly as {@link #insertRowWithExplicitId} left it. */
    private void assertUntouched(int id, String expectedCompany) {
        JobApplication row = repo.findById(id).orElseThrow();
        assertEquals(expectedCompany, row.getCompany(),
                "update(id=" + id + ") must not have run any SQL against this row");
        assertEquals("Keeper", row.getPosition());
        assertEquals("Nowhere", row.getLocation());
        assertEquals(ApplicationStatus.APPLIED, row.getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), row.getDateApplied());
    }

    @Test
    void deleteByIdRemovesTheRowAndReportsFalseTheSecondTime() {
        JobApplication saved = save("Acme", "Developer", "Remote",
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        assertTrue(repo.deleteById(saved.getId()));
        assertTrue(repo.findById(saved.getId()).isEmpty());

        assertFalse(repo.deleteById(saved.getId()));
    }

    @Test
    void deleteAllEmptiesTheTableButLeavesItUsable() {
        save("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        save("Beta", "Analyst", "NY", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 2));

        repo.deleteAll();
        assertTrue(repo.findAll().isEmpty());

        // The CSV import path calls deleteAll() and then saves again, so this must still work.
        JobApplication after = save("Gamma", "Tester", "LA",
                ApplicationStatus.OFFER, LocalDate.of(2026, 1, 3));
        assertTrue(after.getId() > 0);
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void nullTextFieldsAreStoredAsEmptyStrings() {
        // The columns are NOT NULL, so the repository maps null text to "" on the way in.
        JobApplication saved = save(null, null, null,
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        JobApplication reloaded = repo.findById(saved.getId()).orElseThrow();
        assertEquals("", reloaded.getCompany());
        assertEquals("", reloaded.getPosition());
        assertEquals("", reloaded.getLocation());
    }

    @Test
    void initIsSafeToRunAgainstAnExistingDatabase() throws SQLException {
        save("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        // Every launch calls init(); the CREATE ... IF NOT EXISTS must not wipe data.
        // dbFile is the field setUp populated -- re-deriving the name here would let this
        // test drift onto a different file and quietly stop testing anything.
        new Database(dbFile).init();

        assertEquals(1, repo.findAll().size());
    }
}
