package com.johndoan.jobtracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationRepository {

    private final Map<Integer, JobApplication> store = new LinkedHashMap<>();
    private int nextId = 1;

    // Optional default CSV path for Stage 3 UI
    private Path csvPath; // may be null until configured

    public ApplicationRepository() {}

    public ApplicationRepository(Path csvPath) {
        this.csvPath = csvPath;
    }

    public void setCsvPath(Path csvPath) {
        this.csvPath = csvPath;
    }

    public Path getCsvPath() {
        return csvPath;
    }

    public JobApplication save(JobApplication app) {
        if (app.getId() <= 0) {
            app.setId(nextId++);
        } else {
            // If loading pre-assigned IDs, keep nextId ahead.
            nextId = Math.max(nextId, app.getId() + 1);
        }
        store.put(app.getId(), app);
        return app;
    }

    public List<JobApplication> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<JobApplication> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<JobApplication> findByStatus(ApplicationStatus status) {
        return store.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public boolean deleteById(int id) {
        return store.remove(id) != null;
    }

    // ---------------- CSV ----------------

    public void loadFromCsvIfExists() {
        if (csvPath == null) return;
        try {
            loadFromCsv(csvPath);
        } catch (IOException ignored) {
        }
    }

    public int loadFromCsv(Path path) throws IOException {
        if (!Files.exists(path)) return 0;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = reader.readLine(); // consume header
            if (header == null) return 0;

            int count = 0;
            int maxId = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Simple CSV parse (assumes fields without commas or already clean)
                String[] parts = splitCsvLine(line);
                // expected: id,company,position,location,status,appliedDate
                int id = Integer.parseInt(parts[0].trim());
                String company = unquote(parts[1]);
                String position = unquote(parts[2]);
                String location = unquote(parts[3]);
                ApplicationStatus status = ApplicationStatus.valueOf(parts[4].trim());
                LocalDate applied = parts[5].trim().isEmpty() ? null : LocalDate.parse(parts[5].trim());

                JobApplication app = new JobApplication(company, position, location, status, applied);
                app.setId(id);
                store.put(id, app);

                maxId = Math.max(maxId, id);
                count++;
            }

            nextId = Math.max(nextId, maxId + 1);
            return count;
        }
    }

    public int exportToCsv(Path path) throws IOException {
        Files.createDirectories(path.getParent());

        List<JobApplication> apps = findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("id,company,position,location,status,appliedDate");
            writer.newLine();

            for (JobApplication app : apps) {
                String applied = (app.getDateApplied() == null) ? "" : app.getDateApplied().toString();
                String row = String.join(",",
                        String.valueOf(app.getId()),
                        escapeCsv(app.getCompany()),
                        escapeCsv(app.getPosition()),
                        escapeCsv(app.getLocation()),
                        app.getStatus().name(),
                        applied
                );
                writer.write(row);
                writer.newLine();
            }
        }

        return apps.size();
    }

    public int exportToDefaultCsv() throws IOException {
        if (csvPath == null) {
            throw new IllegalStateException("CSV path not configured for repository");
        }
        return exportToCsv(csvPath);
    }

    // --- helpers ---

    private static String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private static String unquote(String value) {
        String v = value.trim();
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1).replace("\"\"", "\"");
        }
        return v;
    }

    // minimal CSV splitter supporting quoted commas
    private static String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' ) {
                inQuotes = !inQuotes;
                cur.append(c);
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