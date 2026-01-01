package com.johndoan.jobtracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer.
 *
 * Stage 4: persistence is SQLite via JdbcApplicationRepository, but we keep CSV
 * import/export as a backup + migration tool.
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
        if (maybe.isEmpty()) return false;

        JobApplication app = maybe.get();
        app.setStatus(newStatus);
        return repository.update(app);
    }

    public boolean deleteApplication(int id) {
        return repository.deleteById(id);
    }

    /**
     * Export current DB contents to CSV.
     */
    public int exportApplicationsToCsv(Path csvPath) throws IOException {
        List<JobApplication> apps = repository.findAll();

        Path parent = csvPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write("id,company,position,location,status,dateApplied");
            writer.newLine();

            for (JobApplication app : apps) {
                String line = String.join(",",
                        String.valueOf(app.getId()),
                        csvEscape(app.getCompany()),
                        csvEscape(app.getPosition()),
                        csvEscape(app.getLocation()),
                        app.getStatus().name(),
                        app.getDateApplied() != null ? app.getDateApplied().toString() : ""
                );
                writer.write(line);
                writer.newLine();
            }
        }
        return apps.size();
    }

    /**
     * Import applications from CSV and REPLACE current DB contents.
     *
     * Notes:
     * - We do not preserve the CSV id; SQLite will assign new ids.
     */
    public int loadApplicationsFromCsv(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV not found: " + csvPath.toAbsolutePath());
        }

        List<JobApplication> parsed = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // discard header
            if (header == null) return 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] cols = splitCsvLine(line);
                // Expect: id,company,position,location,status,dateApplied
                if (cols.length < 6) continue;

                String company = cols[1];
                String position = cols[2];
                String location = cols[3];

                ApplicationStatus status;
                try {
                    status = ApplicationStatus.valueOf(cols[4].trim().toUpperCase());
                } catch (Exception e) {
                    status = ApplicationStatus.APPLIED;
                }

                LocalDate dateApplied;
                try {
                    dateApplied = LocalDate.parse(cols[5].trim());
                } catch (DateTimeParseException e) {
                    dateApplied = LocalDate.now();
                }

                parsed.add(new JobApplication(company, position, location, status, dateApplied));
            }
        }

        repository.deleteAll();
        for (JobApplication app : parsed) {
            repository.save(app);
        }

        return parsed.size();
    }

    // ---------------- helpers ----------------

    private static String csvEscape(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v + "\"";
        }
        return v;
    }

    /**
     * Minimal CSV splitting that supports quoted fields with commas.
     */
    private static String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') {
                // double quote inside quotes -> literal quote
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cur.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());

        return out.toArray(new String[0]);
    }
}
