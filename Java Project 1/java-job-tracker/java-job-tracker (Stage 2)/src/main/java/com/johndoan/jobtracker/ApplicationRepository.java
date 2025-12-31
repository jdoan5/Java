package com.johndoan.jobtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApplicationRepository {

    private final List<JobApplication> applications = new ArrayList<>();
    private int nextId = 1;

    // NEW: where to persist data
    private final Path csvPath;

    // Stage 2 constructor – called from Main
    public ApplicationRepository(Path csvPath) {
        this.csvPath = csvPath;
        loadFromCsvIfPresent();
    }

    // Optional: keep a no-arg constructor for tests or Stage 1 behavior
    public ApplicationRepository() {
        this.csvPath = null;
    }

    public JobApplication save(JobApplication app) {
        // keep your existing ID logic here; example:
        app.setId(nextId++);        // or whatever you already had
        applications.add(app);
        return app;
    }

    public List<JobApplication> findAll() {
        return new ArrayList<>(applications);
    }

    public Optional<JobApplication> findById(int id) {
        return applications.stream()
                .filter(a -> a.getId() == id)
                .findFirst();
    }

    public List<JobApplication> findByStatus(ApplicationStatus status) {
        return applications.stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    // Called from ApplicationService.exportApplicationsToCsv()
    public void exportToCsv(List<JobApplication> apps) throws IOException {
        if (csvPath == null) {
            throw new IllegalStateException("CSV path not configured for repository");
        }

        Files.createDirectories(csvPath.getParent());

        try (var writer = Files.newBufferedWriter(csvPath)) {
            writer.write("id,company,position,location,status,appliedDate");
            writer.newLine();

            for (JobApplication app : apps) {
                String line = String.join(",",
                        String.valueOf(app.getId()),
                        escape(app.getCompany()),
                        escape(app.getPosition()),
                        escape(app.getLocation()),
                        app.getStatus().name(),
                        app.getDataApplied() != null ? app.getDataApplied().toString() : ""
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // ---------- private helpers ----------

    private void loadFromCsvIfPresent() {
        if (csvPath == null || !Files.exists(csvPath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            // skip header
            String line = reader.readLine();
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 6) continue;

                // We ignore the saved id and let save() assign a fresh one,
                // to avoid needing to modify JobApplication.
                String company   = unescape(parts[1]);
                String position  = unescape(parts[2]);
                String location  = unescape(parts[3]);
                ApplicationStatus status = ApplicationStatus.valueOf(parts[4]);
                LocalDate applied = parts[5].isEmpty()
                        ? null
                        : LocalDate.parse(parts[5]);

                JobApplication app =
                        new JobApplication(company, position, location, status, applied);
                save(app);   // uses the normal ID logic and increments nextId
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("Warning: could not load CSV data: " + e.getMessage());
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v + "\"";
        }
        return v;
    }

    private String unescape(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1).replace("\"\"", "\"");
        }
        return v;
    }
}