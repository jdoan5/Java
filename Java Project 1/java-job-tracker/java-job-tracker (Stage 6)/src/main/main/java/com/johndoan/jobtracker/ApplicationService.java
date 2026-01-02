package com.johndoan.jobtracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public JobApplication addApplication(String company,
                                         String position,
                                         String location,
                                         ApplicationStatus status,
                                         LocalDate dateApplied) {
        Validation.requireNotBlank(company, "Company");
        Validation.requireNotBlank(position, "Position");
        Validation.requireNotBlank(location, "Location");
        if (status == null) throw new IllegalArgumentException("Status is required.");
        if (dateApplied == null) dateApplied = LocalDate.now();

        JobApplication created = repository.add(new JobApplication(company.trim(),
                position.trim(),
                location.trim(),
                status,
                dateApplied));

        log.info("Added application id={} company={} position={}", created.getId(), company, position);
        return created;
    }

    public List<JobApplication> listAll() {
        return repository.findAll();
    }

    public List<JobApplication> listByStatus(ApplicationStatus status) {
        return repository.findByStatus(status);
    }

    public List<JobApplication> search(SearchFilter filter) {
        if (filter == null) {
            return repository.findAll();
        }
        Validation.requireDateRange(filter.dateFrom(), filter.dateTo());
        return repository.search(filter);
    }

    public boolean updateStatus(int id, ApplicationStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("New status is required.");
        boolean ok = repository.updateStatus(id, newStatus);
        if (ok) log.info("Updated status id={} -> {}", id, newStatus);
        return ok;
    }

    public boolean deleteApplication(int id) {
        boolean ok = repository.deleteById(id);
        if (ok) log.info("Deleted application id={}", id);
        return ok;
    }

    /** CSV export (still useful for portability even though SQLite is primary). */
    public int exportApplicationsToCsv(Path csvPath) throws IOException {
        if (csvPath == null) throw new IllegalArgumentException("CSV path is required.");
        Files.createDirectories(csvPath.toAbsolutePath().getParent());

        List<JobApplication> apps = repository.findAll();
        try (BufferedWriter w = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            w.write("id,company,position,location,status,date_applied");
            w.newLine();
            for (JobApplication a : apps) {
                w.write(a.getId() + "," +
                        csvEscape(a.getCompany()) + "," +
                        csvEscape(a.getPosition()) + "," +
                        csvEscape(a.getLocation()) + "," +
                        a.getStatus() + "," +
                        a.getDateApplied());
                w.newLine();
            }
        }
        log.info("Exported {} rows to {}", apps.size(), csvPath);
        return apps.size();
    }

    /**
     * CSV import. Replaces all DB rows with the CSV content.
     * IDs in CSV are ignored (DB will autogenerate new IDs).
     */
    public int loadApplicationsFromCsv(Path csvPath) throws IOException {
        if (csvPath == null) throw new IllegalArgumentException("CSV path is required.");
        if (!Files.exists(csvPath)) throw new IOException("CSV file not found: " + csvPath);

        List<JobApplication> apps = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null) throw new IOException("CSV is empty.");

            String line;
            int lineNo = 1;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsvLine(line);
                if (parts.length < 6) {
                    throw new IOException("Invalid CSV at line " + lineNo + " (expected 6 columns).");
                }

                String company = parts[1];
                String position = parts[2];
                String location = parts[3];
                String statusStr = parts[4];
                String dateStr = parts[5];

                ApplicationStatus status;
                try {
                    status = ApplicationStatus.valueOf(statusStr.trim().toUpperCase());
                } catch (Exception e) {
                    throw new IOException("Invalid status at line " + lineNo + ": " + statusStr);
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateStr.trim());
                } catch (DateTimeParseException e) {
                    throw new IOException("Invalid date at line " + lineNo + ": " + dateStr);
                }

                Validation.requireNotBlank(company, "Company");
                Validation.requireNotBlank(position, "Position");
                Validation.requireNotBlank(location, "Location");

                apps.add(new JobApplication(company.trim(), position.trim(), location.trim(), status, date));
            }
        }

        repository.replaceAll(apps);
        log.info("Imported {} rows from {}", apps.size(), csvPath);
        return apps.size();
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        String v = s.replace(""", """");
        if (v.contains(",") || v.contains(""") || v.contains("\n")) {
            return """ + v + """;
        }
        return v;
    }

    // Minimal CSV split supporting quoted commas.
    private static String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
