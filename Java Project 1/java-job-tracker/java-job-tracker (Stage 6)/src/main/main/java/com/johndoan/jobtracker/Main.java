package com.johndoan.jobtracker;

import com.johndoan.jobtracker.persistence.Database;
import com.johndoan.jobtracker.persistence.JdbcApplicationRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Optional console runner. UI is primary in Stage 6. */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        Path dbPath = Paths.get(System.getProperty("user.home"), ".jobtracker", "job_tracker.db");
        Database db = new Database(dbPath);
        db.init();

        ApplicationRepository repo = new JdbcApplicationRepository(db);
        ApplicationService service = new ApplicationService(repo);

        System.out.println("=== Job Application Tracker (Stage 6 - SQLite) ===");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleAdd(service);
                case "2" -> handleListAll(service);
                case "3" -> handleFilterByStatus(service);
                case "4" -> handleUpdateStatus(service);
                case "5" -> running = false;
                default -> System.out.println("Unknown option, please try again.");
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("Choose an option:");
        System.out.println("  1) Add application");
        System.out.println("  2) List all applications");
        System.out.println("  3) List applications by status");
        System.out.println("  4) Update application status");
        System.out.println("  5) Exit");
        System.out.print("Your choice: ");
    }

    private static void handleAdd(ApplicationService service) {
        System.out.print("Company: ");
        String company = scanner.nextLine().trim();

        System.out.print("Position: ");
        String position = scanner.nextLine().trim();

        System.out.print("Location: ");
        String location = scanner.nextLine().trim();

        ApplicationStatus status = askStatus("Initial status (APPLIED/INTERVIEW/OFFER/REJECTED): ");
        LocalDate date = askDate("Date applied (YYYY-MM-DD, blank for today): ");

        JobApplication app = service.addApplication(company, position, location, status, date);
        System.out.println("Added: " + app);
    }

    private static void handleListAll(ApplicationService service) {
        List<JobApplication> apps = service.listAll();
        if (apps.isEmpty()) {
            System.out.println("No applications yet.");
            return;
        }
        for (JobApplication app : apps) {
            System.out.println(app);
        }
    }

    private static void handleFilterByStatus(ApplicationService service) {
        ApplicationStatus status = askStatus("Filter by status (APPLIED/INTERVIEW/OFFER/REJECTED): ");
        List<JobApplication> apps = service.listByStatus(status);
        if (apps.isEmpty()) {
            System.out.println("No applications with status " + status + ".");
            return;
        }
        for (JobApplication app : apps) {
            System.out.println(app);
        }
    }

    private static void handleUpdateStatus(ApplicationService service) {
        System.out.print("Enter application ID to update: ");
        String idInput = scanner.nextLine().trim();
        int id;
        try {
            id = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }

        ApplicationStatus newStatus = askStatus("New status (APPLIED/INTERVIEW/OFFER/REJECTED): ");
        boolean updated = service.updateStatus(id, newStatus);
        if (updated) {
            System.out.println("Updated application #" + id + " to status " + newStatus + ".");
        } else {
            System.out.println("Application with ID " + id + " not found.");
        }
    }

    private static ApplicationStatus askStatus(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return ApplicationStatus.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid status. Valid values: APPLIED, INTERVIEW, OFFER, REJECTED.");
            }
        }
    }

    private static LocalDate askDate(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format, using today instead.");
            return LocalDate.now();
        }
    }
}
