package com.johndoan.jobtracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApplicationRepository {

    private final List<JobApplication> store = new ArrayList<>();
    private int nextId = 1;

    private static final Path EXPORT_PATH = Paths.get("CSV", "job_applications.csv");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public JobApplication save(JobApplication app) {
        if (app.getId() == 0) {
            app.setId(nextId++);
        }
        store.add(app);
        return app;
    }

    public List<JobApplication> findAll() {
        return new ArrayList<>(store);
    }

    public Optional<JobApplication> findById(int id) {
        return store.stream()
                .filter(a -> a.getId() == id)
                .findFirst();
    }

    public List<JobApplication> findByStatus(ApplicationStatus status) {
        List<JobApplication> result = new ArrayList<>();
        for (JobApplication app : store) {
            if (app.getStatus() == status) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * Export the given applications to CSV/job_applications.csv.
     */
    public void exportToCsv(List<JobApplication> applications) throws IOException {
        Files.createDirectories(EXPORT_PATH.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(EXPORT_PATH)) {
            // header
            writer.write("id,company,position,location,status,appliedDate");
            writer.newLine();

            for (JobApplication app : applications) {
                String dateStr = "";
                if (app.getDateApplied() != null) {
                    dateStr = app.getDateApplied().format(DATE_FMT);
                }

                String line = String.join(",",
                        String.valueOf(app.getId()),
                        escape(app.getCompany()),
                        escape(app.getPosition()),
                        escape(app.getLocation()),
                        app.getStatus().name(),
                        dateStr
                );
                writer.write(line);
                writer.newLine();
            }
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
}