package com.johndoan.jobtracker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 5 unit tests for the service layer, driven by an in-memory fake repository.
 *
 * The fake deliberately hands back <em>copies</em> from {@code findById}/{@code findAll},
 * the way the JDBC repository maps a fresh object out of each {@code ResultSet}. That
 * keeps {@link ApplicationService#updateStatus} honest: mutating the returned object is
 * not enough, the service has to call {@code update} for the change to stick.
 *
 * Note that {@code addApplication} performs no validation of its own in this build --
 * blank company/position/location are rejected by the Swing form, not by the service --
 * so these tests cover the behaviour that actually exists rather than asserting rules
 * the service does not enforce.
 */
public class ApplicationServiceTest {

    /** In-memory stand-in for the SQLite-backed repository. */
    private static class FakeRepo implements ApplicationRepository {

        private int nextId = 1;
        private final Map<Integer, JobApplication> store = new LinkedHashMap<>();

        @Override
        public JobApplication save(JobApplication application) {
            application.setId(nextId++);
            store.put(application.getId(), copyOf(application));
            return application;
        }

        @Override
        public List<JobApplication> findAll() {
            return store.values().stream().map(FakeRepo::copyOf).toList();
        }

        @Override
        public List<JobApplication> findByStatus(ApplicationStatus status) {
            return store.values().stream()
                    .filter(a -> a.getStatus() == status)
                    .map(FakeRepo::copyOf)
                    .toList();
        }

        @Override
        public Optional<JobApplication> findById(int id) {
            return Optional.ofNullable(store.get(id)).map(FakeRepo::copyOf);
        }

        @Override
        public boolean update(JobApplication application) {
            if (application.getId() <= 0 || !store.containsKey(application.getId())) return false;
            store.put(application.getId(), copyOf(application));
            return true;
        }

        @Override
        public boolean deleteById(int id) {
            return store.remove(id) != null;
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        private static JobApplication copyOf(JobApplication a) {
            return new JobApplication(a.getId(), a.getCompany(), a.getPosition(),
                    a.getLocation(), a.getStatus(), a.getDateApplied());
        }
    }

    private final FakeRepo repo = new FakeRepo();
    private final ApplicationService service = new ApplicationService(repo);

    @Test
    void addApplicationAssignsAnIdAndStoresEveryField() {
        JobApplication created = service.addApplication(
                "Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        assertTrue(created.getId() > 0, "repository should assign a generated id");

        List<JobApplication> all = service.listAll();
        assertEquals(1, all.size());

        JobApplication stored = all.get(0);
        assertEquals(created.getId(), stored.getId());
        assertEquals("Acme", stored.getCompany());
        assertEquals("Developer", stored.getPosition());
        assertEquals("Remote", stored.getLocation());
        assertEquals(ApplicationStatus.APPLIED, stored.getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), stored.getDateApplied());
    }

    @Test
    void listByStatusReturnsOnlyMatchingApplications() {
        service.addApplication("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        service.addApplication("Beta", "Analyst", "NY", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 2));
        service.addApplication("Gamma", "Tester", "LA", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 3));

        List<JobApplication> interviews = service.listByStatus(ApplicationStatus.INTERVIEW);

        assertEquals(2, interviews.size());
        assertTrue(interviews.stream().allMatch(a -> a.getStatus() == ApplicationStatus.INTERVIEW));
        assertEquals(List.of("Beta", "Gamma"),
                interviews.stream().map(JobApplication::getCompany).sorted().toList());

        assertEquals(1, service.listByStatus(ApplicationStatus.APPLIED).size());
        assertEquals(0, service.listByStatus(ApplicationStatus.OFFER).size());
    }

    @Test
    void updateStatusWritesTheNewStatusBackToTheRepository() {
        JobApplication created = service.addApplication(
                "Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        assertTrue(service.updateStatus(created.getId(), ApplicationStatus.OFFER));

        // Re-read through the repository: the change must have been persisted, not
        // merely applied to the transient object the service loaded.
        JobApplication reloaded = repo.findById(created.getId()).orElseThrow();
        assertEquals(ApplicationStatus.OFFER, reloaded.getStatus());

        // Everything else is left alone.
        assertEquals("Acme", reloaded.getCompany());
        assertEquals(LocalDate.of(2026, 1, 1), reloaded.getDateApplied());
    }

    @Test
    void updateStatusReturnsFalseForAnUnknownId() {
        service.addApplication("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        assertFalse(service.updateStatus(999, ApplicationStatus.REJECTED));

        // The existing row is untouched.
        assertEquals(ApplicationStatus.APPLIED, service.listAll().get(0).getStatus());
    }

    @Test
    void deleteApplicationRemovesTheRowAndIsNotIdempotent() {
        JobApplication created = service.addApplication(
                "Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        assertTrue(service.deleteApplication(created.getId()));
        assertTrue(service.listAll().isEmpty());

        // Second delete reports that nothing was removed.
        assertFalse(service.deleteApplication(created.getId()));
    }

    @Test
    void csvRoundTripPreservesEveryFieldAndReplacesExistingRows(@TempDir Path tempDir) throws IOException {
        service.addApplication("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        service.addApplication("Beta", "Analyst", "NY", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 2));

        Path csv = tempDir.resolve("export").resolve("applications.csv");
        assertEquals(2, service.exportApplicationsToCsv(csv));
        assertTrue(Files.exists(csv), "export should create missing parent directories");

        // A third row that is NOT in the CSV: import replaces the contents, so it must vanish.
        service.addApplication("Gamma", "Tester", "LA", ApplicationStatus.OFFER, LocalDate.of(2026, 1, 3));
        assertEquals(3, service.listAll().size());

        assertEquals(2, service.loadApplicationsFromCsv(csv));

        List<JobApplication> reloaded = service.listAll().stream()
                .sorted(Comparator.comparing(JobApplication::getCompany))
                .toList();
        assertEquals(2, reloaded.size());

        assertEquals("Acme", reloaded.get(0).getCompany());
        assertEquals("Developer", reloaded.get(0).getPosition());
        assertEquals("Remote", reloaded.get(0).getLocation());
        assertEquals(ApplicationStatus.APPLIED, reloaded.get(0).getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), reloaded.get(0).getDateApplied());

        assertEquals("Beta", reloaded.get(1).getCompany());
        assertEquals(ApplicationStatus.INTERVIEW, reloaded.get(1).getStatus());
        assertEquals(LocalDate.of(2026, 1, 2), reloaded.get(1).getDateApplied());
    }

    @Test
    void csvExportQuotesFieldsContainingCommasAndQuotes(@TempDir Path tempDir) throws IOException {
        service.addApplication("Acme, Inc.", "Developer \"II\"", "Remote",
                ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));

        Path csv = tempDir.resolve("quoted.csv");
        service.exportApplicationsToCsv(csv);

        String written = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(written.contains("\"Acme, Inc.\""),
                "a value containing a comma must be quoted, got: " + written);
        assertTrue(written.contains("\"Developer \"\"II\"\"\""),
                "embedded quotes must be doubled, got: " + written);

        // And the parser has to undo exactly what the writer did.
        service.loadApplicationsFromCsv(csv);
        JobApplication reloaded = service.listAll().get(0);
        assertEquals("Acme, Inc.", reloaded.getCompany());
        assertEquals("Developer \"II\"", reloaded.getPosition());
    }

    @Test
    void loadApplicationsFromCsvThrowsWhenTheFileIsMissing(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nope.csv");

        IOException thrown = assertThrows(IOException.class, () -> service.loadApplicationsFromCsv(missing));
        assertTrue(thrown.getMessage().contains("CSV not found"));
    }

    @Test
    void csvImportFallsBackOnUnreadableStatusAndDate(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("messy.csv");
        Files.writeString(csv, """
                id,company,position,location,status,dateApplied
                1,Acme,Developer,Remote,NOT_A_STATUS,2026-01-01
                2,Beta,Analyst,NY,OFFER,not-a-date
                """, StandardCharsets.UTF_8);

        // loadApplicationsFromCsv evaluates LocalDate.now() itself while parsing. Bracketing
        // the call and asserting on the resulting interval -- rather than calling
        // LocalDate.now() a second time at assertion time -- keeps a run that straddles local
        // midnight from failing spuriously.
        LocalDate before = LocalDate.now();
        assertEquals(2, service.loadApplicationsFromCsv(csv));
        LocalDate after = LocalDate.now();

        List<JobApplication> rows = service.listAll().stream()
                .sorted(Comparator.comparing(JobApplication::getCompany))
                .toList();

        // Unparseable status falls back to APPLIED, keeping the rest of the row.
        assertEquals(ApplicationStatus.APPLIED, rows.get(0).getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), rows.get(0).getDateApplied());

        // Unparseable date falls back to today, keeping the parsed status.
        assertEquals(ApplicationStatus.OFFER, rows.get(1).getStatus());

        LocalDate fallbackDate = rows.get(1).getDateApplied();
        assertNotNull(fallbackDate, "an unparseable date must still yield a date");
        assertFalse(fallbackDate.isBefore(before),
                () -> "fallback date " + fallbackDate + " is before the import started (" + before + ")");
        assertFalse(fallbackDate.isAfter(after),
                () -> "fallback date " + fallbackDate + " is after the import finished (" + after + ")");
    }

    /**
     * Scope note -- what this test does and does not prove.
     *
     * <p>It proves that neither a blank line nor a line with too few columns becomes a row:
     * only the two well-formed six-column lines survive.
     *
     * <p>It deliberately does <em>not</em> claim to exercise the {@code line.isBlank()} guard
     * in {@code loadApplicationsFromCsv} specifically. A blank line contains only whitespace,
     * so it contains no commas, so {@code splitCsvLine} hands back a single-element array and
     * the later {@code cols.length < 6} check rejects it regardless. That makes the two guards
     * indistinguishable from outside the method for <em>any</em> input -- a line with six or
     * more columns needs at least five commas, and a line containing a comma is not blank --
     * so deleting the {@code isBlank()} guard is unobservable through the public API and no
     * test here can pin it down.
     *
     * <p>The column-count guard, by contrast, genuinely is under test: the three-column line
     * and the five-column line (one column short -- the off-by-one boundary) both have to be
     * dropped for the assertions below to hold.
     */
    @Test
    void csvImportSkipsBlankAndTruncatedLines(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("ragged.csv");
        Files.writeString(csv, """
                id,company,position,location,status,dateApplied
                1,Acme,Developer,Remote,APPLIED,2026-01-01

                2,Beta,Analyst
                4,Delta,Manager,SF,OFFER
                3,Gamma,Tester,LA,OFFER,2026-01-03
                """, StandardCharsets.UTF_8);

        assertEquals(2, service.loadApplicationsFromCsv(csv));
        assertEquals(List.of("Acme", "Gamma"),
                service.listAll().stream().map(JobApplication::getCompany).sorted().toList());
    }

    @Test
    void exportingAnEmptyRepositoryWritesHeaderOnly(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("empty.csv");

        assertEquals(0, service.exportApplicationsToCsv(csv));

        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(List.of("id,company,position,location,status,dateApplied"), lines);

        // Re-importing a header-only file clears the table and reports zero rows.
        service.addApplication("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        assertEquals(0, service.loadApplicationsFromCsv(csv));
        assertTrue(service.listAll().isEmpty());
    }
}
