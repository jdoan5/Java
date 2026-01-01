package com.johndoan.jobtracker;

import com.johndoan.jobtracker.ui.JobTrackerFrame;

import javax.swing.*;

public class UiMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ApplicationRepository repository = new ApplicationRepository();
                repository.setCsvPath(java.nio.file.Paths.get("CSV/job_applications.csv"));
                ApplicationService service = new ApplicationService(repository);

                // Load existing CSV data
                repository.loadFromCsvIfExists();
                System.out.println("Loaded existing applications from CSV.");

                // Add shutdown hook to save data on exit
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        service.exportApplicationsToDefaultCsv();
                        System.out.println("Auto-saved applications to CSV on shutdown.");
                    } catch (Exception e) {
                        System.err.println("Failed to auto-save on shutdown: " + e.getMessage());
                    }
                }));

                JobTrackerFrame frame = new JobTrackerFrame(service);
                frame.setLocationRelativeTo(null); // center on screen
                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to start Job Tracker UI: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}