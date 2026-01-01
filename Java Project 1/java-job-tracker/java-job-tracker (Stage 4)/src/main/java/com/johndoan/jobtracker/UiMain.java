package com.johndoan.jobtracker;

import com.johndoan.jobtracker.persistence.JdbcApplicationRepository;
import com.johndoan.jobtracker.ui.JobTrackerFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stage 4 entry point: Swing UI + SQLite persistence.
 */
public class UiMain {

    public static void main(String[] args) throws Exception {
        // Store DB in a user-local application folder (Mac-friendly)
        Path appDir = Paths.get(System.getProperty("user.home"), ".jobtracker");
        Files.createDirectories(appDir);

        Path dbPath = appDir.resolve("job_tracker.db");

        com.johndoan.jobtracker.persistence.Database database = new com.johndoan.jobtracker.persistence.Database(dbPath);
        database.init();

        ApplicationRepository repository = new JdbcApplicationRepository(database);
        ApplicationService service = new ApplicationService(repository);

        SwingUtilities.invokeLater(() -> {
            JobTrackerFrame frame = new JobTrackerFrame(service);

            // Stage 4: UI starts empty; use Refresh/Apply Filter to load records from SQLite.
            frame.setVisible(true);

        });
    }
}
