package com.johndoan.jobtracker;

import com.johndoan.jobtracker.persistence.Database;
import com.johndoan.jobtracker.persistence.JdbcApplicationRepository;
import com.johndoan.jobtracker.ui.JobTrackerFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stage 5 entry point (Swing UI + SQLite).
 *
 * IMPORTANT: This file must contain a normal Java class (no top-level statements),
 * otherwise you'll see errors like "compact source file should not have package declaration".
 */
public class UiMain {

    public static void main(String[] args) {
        // Nice app name on macOS (optional)
        System.setProperty("apple.awt.application.name", "Job Tracker");

        // Default DB location: ~/.jobtracker/job_tracker.db
        Path dbPath = Paths.get(System.getProperty("user.home"), ".jobtracker", "job_tracker.db");

        // Init DB schema (creates folder + db + tables if needed)
        Database db = new Database(dbPath);
        try {
            db.init();
        } catch (Exception e) {
            // If DB init fails, show a clear error and stop.
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to initialize SQLite database:\n" + dbPath + "\n\n" + e.getMessage(),
                    "Job Tracker - Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
            return;
        }

        ApplicationRepository repository = new JdbcApplicationRepository(db);
        ApplicationService service = new ApplicationService(repository);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }

            JobTrackerFrame frame = new JobTrackerFrame(service, "Job Application Tracker v2.1 (SQLite)");
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
