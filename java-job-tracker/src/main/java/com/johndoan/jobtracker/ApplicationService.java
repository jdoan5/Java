package com.johndoan.jobtracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
        if (maybe.isPresent()) {
            JobApplication app = maybe.get();
            app.setStatus(newStatus);
            return true;
        }
        return false;
    }

    /**
     * Export all applications to a CSV file.
     *
     * @param fileName output file name
     * @return number of applications written
     */
    public int exportToCsv(String fileName) throws IOException {
        List<JobApplication> apps = listAll();
        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            // header row
            writer.write("id,company,position,location,status,appliedDate");
            writer.newLine();

            for (JobApplication app : apps) {
                String line = String.join(",",
                        String.valueOf(app.getId()),
                        escape(app.getCompany()),
                        escape(app.getPosition()),
                        escape(app.getLocation()),
                        app.getStatus().name(),
                        app.getDataApplied() != null ? app.getDataApplied().format(dateFmt) : ""
                );
                writer.write(line);
                writer.newLine();
            }
        }

        return apps.size();
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