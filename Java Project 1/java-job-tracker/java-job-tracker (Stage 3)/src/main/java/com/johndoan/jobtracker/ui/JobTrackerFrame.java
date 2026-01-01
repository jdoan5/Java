package com.johndoan.jobtracker.ui;

import com.johndoan.jobtracker.ApplicationService;
import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.JobApplication;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class JobTrackerFrame extends JFrame {

    private static final String DEFAULT_CSV_PATH = "CSV/job_applications.csv";

    private final ApplicationService service;

    private final DefaultTableModel tableModel;
    private final JTable table;

    private final JTextField companyField = new JTextField(18);
    private final JTextField positionField = new JTextField(18);
    private final JTextField locationField = new JTextField(18);
    private final JComboBox<ApplicationStatus> statusField = new JComboBox<>(ApplicationStatus.values());
    private final JTextField appliedDateField = new JTextField(10); // YYYY-MM-DD

    private final JComboBox<ApplicationStatus> statusFilter = new JComboBox<>(ApplicationStatus.values());

    private final JTextField csvPathField = new JTextField(28);

    public JobTrackerFrame(ApplicationService service) {
        super("Job Application Tracker (Stage 3 - UI)");
        this.service = service;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Company", "Position", "Location", "Status", "Applied"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        csvPathField.setText(DEFAULT_CSV_PATH);

        buildLayout();
        refreshTable();
    }

    private void buildLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Header =====
        JLabel title = new JLabel("Job Application Tracker");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        mainPanel.add(title, BorderLayout.NORTH);

        // ===== Center: table =====
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(scrollPane, BorderLayout.CENTER);

        // ===== Filter + CSV controls =====
        center.add(buildToolbar(), BorderLayout.SOUTH);

        mainPanel.add(center, BorderLayout.CENTER);

        // ===== Bottom: add / update / delete controls =====
        mainPanel.add(buildActionsPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));

        // Row 1: filter controls
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Filter status:"));
        row1.add(statusFilter);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTable());
        row1.add(refreshBtn);

        JButton applyFilterBtn = new JButton("Apply Filter");
        applyFilterBtn.addActionListener(e -> {
            ApplicationStatus status = (ApplicationStatus) statusFilter.getSelectedItem();
            if (status == null) {
                refreshTable();
                return;
            }
            refreshTable(service.listByStatus(status));
        });
        row1.add(applyFilterBtn);

        toolbar.add(row1, BorderLayout.NORTH);

        // Row 2: CSV controls
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("CSV path:"));
        row2.add(csvPathField);

        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> pickCsvPath());
        row2.add(browseBtn);

        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> handleLoadCsv());
        row2.add(loadBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> handleSaveCsv());
        row2.add(saveBtn);

        toolbar.add(row2, BorderLayout.SOUTH);

        return toolbar;
    }

    private JPanel buildActionsPanel() {
        JPanel actions = new JPanel(new BorderLayout(8, 8));

        // Add form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Company"), gc);
        gc.gridx = 1; gc.gridy = r; form.add(companyField, gc);

        gc.gridx = 2; gc.gridy = r; form.add(new JLabel("Position"), gc);
        gc.gridx = 3; gc.gridy = r; form.add(positionField, gc);

        r++;
        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Location"), gc);
        gc.gridx = 1; gc.gridy = r; form.add(locationField, gc);

        gc.gridx = 2; gc.gridy = r; form.add(new JLabel("Status"), gc);
        gc.gridx = 3; gc.gridy = r; form.add(statusField, gc);

        r++;
        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Applied date (YYYY-MM-DD)"), gc);
        gc.gridx = 1; gc.gridy = r; form.add(appliedDateField, gc);

        actions.add(form, BorderLayout.CENTER);

        // Buttons row
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> handleAdd());
        buttons.add(addBtn);

        JButton updateStatusBtn = new JButton("Update Status");
        updateStatusBtn.addActionListener(e -> handleUpdateStatus());
        buttons.add(updateStatusBtn);

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> handleDeleteSelected());
        buttons.add(deleteBtn);

        actions.add(buttons, BorderLayout.SOUTH);

        return actions;
    }

    // ============================
    // Actions
    // ============================

    private void handleAdd() {
        String company = companyField.getText().trim();
        String position = positionField.getText().trim();
        String location = locationField.getText().trim();
        ApplicationStatus status = (ApplicationStatus) statusField.getSelectedItem();

        if (company.isEmpty() || position.isEmpty() || location.isEmpty() || status == null) {
            showError("Please fill company, position, location, and status.");
            return;
        }

        LocalDate appliedDate = parseDateOrToday(appliedDateField.getText().trim());

        try {
            service.addApplication(company, position, location, status, appliedDate);
            clearAddForm();
            refreshTable();
        } catch (Exception ex) {
            showError("Add failed: " + ex.getMessage());
        }
    }

    private void handleUpdateStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a row to update.");
            return;
        }

        Object idObj = tableModel.getValueAt(row, 0);
        if (!(idObj instanceof Integer)) {
            showError("Invalid ID value in table.");
            return;
        }
        int id = (Integer) idObj;

        ApplicationStatus newStatus = (ApplicationStatus) JOptionPane.showInputDialog(
                this,
                "Select new status:",
                "Update Status",
                JOptionPane.PLAIN_MESSAGE,
                null,
                ApplicationStatus.values(),
                tableModel.getValueAt(row, 4)
        );

        if (newStatus == null) return;

        try {
            boolean ok = service.updateStatus(id, newStatus);
            if (!ok) {
                showError("Application #" + id + " not found.");
                return;
            }
            refreshTable();
        } catch (Exception ex) {
            showError("Update failed: " + ex.getMessage());
        }
    }

    private void handleDeleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a row to delete.");
            return;
        }

        Object idObj = tableModel.getValueAt(row, 0);
        if (!(idObj instanceof Integer)) {
            showError("Invalid ID value in table.");
            return;
        }
        int id = (Integer) idObj;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete application #" + id + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = service.deleteApplication(id);
            if (!ok) {
                showError("Application #" + id + " not found.");
                return;
            }
            refreshTable();
        } catch (Exception ex) {
            showError("Delete failed: " + ex.getMessage());
        }
    }

    private void handleLoadCsv() {
        Path csvPath = getCsvPathFromField();

        if (!Files.exists(csvPath)) {
            showError("CSV file not found: " + csvPath);
            return;
        }

        try {
            service.loadApplicationsFromCsv(csvPath);
            refreshTable();
            showInfo("Loaded from " + csvPath);
        } catch (Exception ex) {
            showError("Load failed: " + ex.getMessage());
        }
    }

    private void handleSaveCsv() {
        Path csvPath = getCsvPathFromField();

        try {
            if (csvPath.getParent() != null) {
                Files.createDirectories(csvPath.getParent());
            }

            service.exportApplicationsToCsv(csvPath);

            int count = service.listAll().size();
            showInfo("Saved " + count + " rows to " + csvPath);
        } catch (Exception ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }

    private void pickCsvPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a CSV file");
        chooser.setSelectedFile(Paths.get(csvPathField.getText().trim().isEmpty() ? DEFAULT_CSV_PATH : csvPathField.getText().trim()).toFile());
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            csvPathField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    // ============================
    // Table refresh
    // ============================

    private void refreshTable() {
        refreshTable(service.listAll());
    }

    private void refreshTable(List<JobApplication> apps) {
        tableModel.setRowCount(0);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (JobApplication app : apps) {
            tableModel.addRow(new Object[]{
                    app.getId(),
                    app.getCompany(),
                    app.getPosition(),
                    app.getLocation(),
                    app.getStatus(),
                    app.getDateApplied() == null ? "" : app.getDateApplied().format(fmt)
            });
        }
    }

    // ============================
    // Helpers
    // ============================

    private void clearAddForm() {
        companyField.setText("");
        positionField.setText("");
        locationField.setText("");
        statusField.setSelectedItem(ApplicationStatus.APPLIED);
        appliedDateField.setText("");
    }

    private LocalDate parseDateOrToday(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            showError("Invalid date (use YYYY-MM-DD). Using today instead.");
            return LocalDate.now();
        }
    }

    private Path getCsvPathFromField() {
        String raw = csvPathField.getText().trim();
        if (raw.isEmpty()) raw = DEFAULT_CSV_PATH;
        return Paths.get(raw);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
