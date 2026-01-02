package com.johndoan.jobtracker;

import com.johndoan.jobtracker.persistence.Database;
import com.johndoan.jobtracker.persistence.JdbcApplicationRepository;
import com.johndoan.jobtracker.ui.JobTrackerFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UiMain {

    private static final Logger log = LoggerFactory.getLogger(UiMain.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Path dbPath = defaultDbPath();
                Database db = new Database(dbPath);
                db.init();

                ApplicationRepository repo = new JdbcApplicationRepository(db);
                ApplicationService service = new ApplicationService(repo);

                String title = "Job Tracker v3.1.0 (SQLite • Search/Sort • Validation)";
                JobTrackerFrame frame = new JobTrackerFrame(service, title);
                frame.setVisible(true);

                log.info("UI started. DB={}", dbPath);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start app: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    private static Path defaultDbPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".jobtracker", "job_tracker.db");
    }
}
