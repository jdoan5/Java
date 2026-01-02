package com.johndoan.jobtracker;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 5 unit tests: service validation + happy paths.
 *
 * Uses an in-memory fake repository.
 */
public class ApplicationServiceTest {

    private static class FakeRepo implements ApplicationRepository {
        private int nextId = 1;
        private final java.util.Map<Integer, JobApplication> store = new java.util.LinkedHashMap<>();

        @Override
        public JobApplication save(JobApplication app) {
            try {
                var m = app.getClass().getMethod("setId", int.class);
                m.invoke(app, nextId);
            } catch (Exception ignored) {}
            store.put(nextId, app);
            nextId++;
            return app;
        }

        @Override
        public List<JobApplication> findAll(SortSpec sort) {
            return store.values().stream().toList();
        }

        @Override
        public List<JobApplication> findByStatus(ApplicationStatus status, SortSpec sort) {
            return store.values().stream().filter(a -> a.getStatus() == status).toList();
        }

        @Override
        public List<JobApplication> search(String query, SortSpec sort) {
            String q = query.toLowerCase();
            return store.values().stream().filter(a ->
                    a.getCompany().toLowerCase().contains(q) ||
                    a.getPosition().toLowerCase().contains(q)
            ).toList();
        }

        @Override
        public List<JobApplication> search(String query, ApplicationStatus status, SortSpec sort) {
            String q = query.toLowerCase();
            return store.values().stream().filter(a ->
                    a.getStatus() == status &&
                    (a.getCompany().toLowerCase().contains(q) || a.getPosition().toLowerCase().contains(q))
            ).toList();
        }

        @Override
        public java.util.Optional<JobApplication> findById(int id) {
            return java.util.Optional.ofNullable(store.get(id));
        }

        @Override
        public boolean updateStatus(int id, ApplicationStatus newStatus) {
            JobApplication a = store.get(id);
            if (a == null) return false;
            a.setStatus(newStatus);
            return true;
        }

        @Override
        public boolean deleteById(int id) {
            return store.remove(id) != null;
        }
    }

    @Test
    void addApplication_requiresCompanyPositionLocation() {
        ApplicationService s = new ApplicationService(new FakeRepo());

        assertThrows(IllegalArgumentException.class, () ->
                s.addApplication("", "Dev", "NY", ApplicationStatus.APPLIED, LocalDate.now()));

        assertThrows(IllegalArgumentException.class, () ->
                s.addApplication("Acme", "", "NY", ApplicationStatus.APPLIED, LocalDate.now()));

        assertThrows(IllegalArgumentException.class, () ->
                s.addApplication("Acme", "Dev", "", ApplicationStatus.APPLIED, LocalDate.now()));
    }

    @Test
    void addAndSearchAndDelete_work() {
        ApplicationService s = new ApplicationService(new FakeRepo());

        s.addApplication("Acme", "Developer", "Remote", ApplicationStatus.APPLIED, LocalDate.of(2026, 1, 1));
        s.addApplication("Beta", "Analyst", "NY", ApplicationStatus.INTERVIEW, LocalDate.of(2026, 1, 2));

        List<JobApplication> r1 = s.search("acme", null, ApplicationRepository.SortSpec.defaultSort());
        assertEquals(1, r1.size());

        boolean ok = s.deleteById(1);
        assertTrue(ok);

        List<JobApplication> all = s.listAll(ApplicationRepository.SortSpec.defaultSort());
        assertEquals(1, all.size());
    }
}
