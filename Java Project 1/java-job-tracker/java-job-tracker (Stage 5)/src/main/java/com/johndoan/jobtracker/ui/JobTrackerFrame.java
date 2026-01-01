package com.johndoan.jobtracker.ui;

import com.johndoan.jobtracker.ApplicationService;
import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.JobApplication;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stage 5 UI:
 * - Keeps Stage 4 layout (CRUD + CSV import/export)
 * - Adds client-side search (company/position) and sort controls
 * - Uses TableRowSorter so search + sort happen immediately on the table
 *
 * Notes:
 * - "Clear View" clears the table display only (does NOT delete DB rows).
 * - "Refresh" reloads from the database (via ApplicationService.listAll()).
 */
public class JobTrackerFrame extends JFrame {

    private final ApplicationService service;

    // Table
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    // Filter/search/sort controls
    private final JComboBox<ApplicationStatus> statusFilterCombo;
    private final JButton refreshBtn;
    private final JButton clearViewBtn;
    private final JButton applyStatusFilterBtn;

    private final JTextField companySearchField;
    private final JTextField positionSearchField;
    private final JButton applySearchBtn;
    private final JButton clearSearchBtn;

    private final JComboBox<String> sortByCombo;
    private final JComboBox<String> sortDirCombo;
    private final JButton applySortBtn;

    // CSV controls
    private final JTextField csvPathField;
    private final JButton browseCsvBtn;
    private final JButton loadCsvBtn;
    private final JButton saveCsvBtn;

    // Form controls (Add)
    private final JTextField companyField;
    private final JTextField positionField;
    private final JTextField locationField;
    private final JComboBox<ApplicationStatus> statusCombo;
    private final JTextField dateField;

    // Update/Delete
    private final JComboBox<ApplicationStatus> updateStatusCombo;
    private final JButton addBtn;
    private final JButton updateStatusBtn;
    private final JButton deleteBtn;

    public JobTrackerFrame(ApplicationService service, String title) {
        super(title);
        this.service = service;

        // Table model
        tableModel = new DefaultTableModel(new Object[]{"ID", "Company", "Position", "Location", "Status", "Applied"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Top controls
        statusFilterCombo = new JComboBox<>(ApplicationStatus.values());
        statusFilterCombo.setSelectedItem(ApplicationStatus.APPLIED);

        refreshBtn = new JButton("Refresh");
        clearViewBtn = new JButton("Clear View");
        applyStatusFilterBtn = new JButton("Apply Filter");

        companySearchField = new JTextField(10);
        positionSearchField = new JTextField(10);
        applySearchBtn = new JButton("Search");
        clearSearchBtn = new JButton("Clear Search");

        sortByCombo = new JComboBox<>(new String[]{"ID", "Company", "Position", "Location", "Status", "Applied"});
        sortDirCombo = new JComboBox<>(new String[]{"Asc", "Desc"});
        applySortBtn = new JButton("Sort");

        // CSV controls
        csvPathField = new JTextField("CSV/job_applications.csv", 26);
        browseCsvBtn = new JButton("Browse...");
        loadCsvBtn = new JButton("Load");
        saveCsvBtn = new JButton("Save");

        // Form controls
        companyField = new JTextField(18);
        positionField = new JTextField(18);
        locationField = new JTextField(18);
        statusCombo = new JComboBox<>(ApplicationStatus.values());
        statusCombo.setSelectedItem(ApplicationStatus.APPLIED);
        dateField = new JTextField(12);

        updateStatusCombo = new JComboBox<>(ApplicationStatus.values());
        updateStatusCombo.setSelectedItem(ApplicationStatus.APPLIED);

        addBtn = new JButton("Add");
        updateStatusBtn = new JButton("Update Status");
        deleteBtn = new JButton("Delete Selected");

        buildLayout();
        wireActions();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 640);
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("Job Application Tracker");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 26f));
        root.add(header, BorderLayout.NORTH);

        // Center: table
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(980, 320));

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(scrollPane, BorderLayout.CENTER);

        // Controls area (below table)
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        // Row 1: status filter + refresh/clear
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Filter status:"));
        row1.add(statusFilterCombo);
        row1.add(refreshBtn);
        row1.add(clearViewBtn);
        row1.add(applyStatusFilterBtn);
        controls.add(row1);

        // Row 2: search + sort
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Search company:"));
        row2.add(companySearchField);
        row2.add(new JLabel("Search position:"));
        row2.add(positionSearchField);
        row2.add(applySearchBtn);
        row2.add(clearSearchBtn);

        row2.add(Box.createHorizontalStrut(12));
        row2.add(new JLabel("Sort by:"));
        row2.add(sortByCombo);
        row2.add(sortDirCombo);
        row2.add(applySortBtn);
        controls.add(row2);

        // Row 3: CSV path
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new JLabel("CSV path:"));
        row3.add(csvPathField);
        row3.add(browseCsvBtn);
        row3.add(loadCsvBtn);
        row3.add(saveCsvBtn);
        controls.add(row3);

        center.add(controls, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        // Bottom: add/update/delete form
        JPanel bottom = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Left column: Company/Location/Date
        gbc.gridx = 0; gbc.gridy = 0; bottom.add(new JLabel("Company"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; bottom.add(companyField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; bottom.add(new JLabel("Location"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; bottom.add(locationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; bottom.add(new JLabel("Applied date (YYYY-MM-DD)"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; bottom.add(dateField, gbc);

        // Right column: Position/Status
        gbc.gridx = 2; gbc.gridy = 0; bottom.add(new JLabel("Position"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; bottom.add(positionField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; bottom.add(new JLabel("Status"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; bottom.add(statusCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 2; bottom.add(new JLabel("Update status"), gbc);
        gbc.gridx = 3; gbc.gridy = 2; bottom.add(updateStatusCombo, gbc);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.add(addBtn);
        btnRow.add(updateStatusBtn);
        btnRow.add(deleteBtn);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.EAST;
        bottom.add(btnRow, gbc);

        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> refreshTable());
        applyStatusFilterBtn.addActionListener(e -> applyStatusFilter());
        clearViewBtn.addActionListener(e -> clearViewOnly());

        applySearchBtn.addActionListener(e -> applyClientSearchFilter());
        clearSearchBtn.addActionListener(e -> {
            companySearchField.setText("");
            positionSearchField.setText("");
            applyClientSearchFilter();
        });

        applySortBtn.addActionListener(e -> applyClientSort());

        browseCsvBtn.addActionListener(e -> browseCsv());
        loadCsvBtn.addActionListener(e -> loadCsv());
        saveCsvBtn.addActionListener(e -> saveCsv());

        addBtn.addActionListener(e -> addApplication());
        updateStatusBtn.addActionListener(e -> updateSelectedStatus());
        deleteBtn.addActionListener(e -> deleteSelected());

        // Convenience: re-apply search filter while typing (lightweight)
        companySearchField.getDocument().addDocumentListener((SimpleDocumentListener) e -> applyClientSearchFilter());
        positionSearchField.getDocument().addDocumentListener((SimpleDocumentListener) e -> applyClientSearchFilter());
    }

    public void refreshTable() {
        List<JobApplication> apps = service.listAll();
        loadIntoTable(apps);
        applyClientSort();
        applyClientSearchFilter();
    }

    private void applyStatusFilter() {
        ApplicationStatus status = (ApplicationStatus) statusFilterCombo.getSelectedItem();
        if (status == null) {
            refreshTable();
            return;
        }
        List<JobApplication> apps = service.listByStatus(status);
        loadIntoTable(apps);
        applyClientSort();
        applyClientSearchFilter();
    }

    private void clearViewOnly() {
        tableModel.setRowCount(0);
        table.clearSelection();
        // Keep search/sort state intact, but there are no rows to act on.
    }

    private void loadIntoTable(List<JobApplication> apps) {
        tableModel.setRowCount(0);
        for (JobApplication app : apps) {
            tableModel.addRow(new Object[]{
                    app.getId(),
                    app.getCompany(),
                    app.getPosition(),
                    app.getLocation(),
                    app.getStatus().name(),
                    app.getDateApplied() != null ? app.getDateApplied().toString() : ""
            });
        }
    }

    private void applyClientSearchFilter() {
        String companyTerm = companySearchField.getText().trim();
        String positionTerm = positionSearchField.getText().trim();

        RowFilter<DefaultTableModel, Object> rf = null;

        try {
            RowFilter<DefaultTableModel, Object> companyFilter = null;
            RowFilter<DefaultTableModel, Object> positionFilter = null;

            if (!companyTerm.isEmpty()) {
                String pat = "(?i)" + Pattern.quote(companyTerm);
                // Company column index = 1
                companyFilter = RowFilter.regexFilter(pat, 1);
            }
            if (!positionTerm.isEmpty()) {
                String pat = "(?i)" + Pattern.quote(positionTerm);
                // Position column index = 2
                positionFilter = RowFilter.regexFilter(pat, 2);
            }

            if (companyFilter != null && positionFilter != null) {
                rf = RowFilter.andFilter(List.of(companyFilter, positionFilter));
            } else if (companyFilter != null) {
                rf = companyFilter;
            } else if (positionFilter != null) {
                rf = positionFilter;
            }
        } catch (Exception ex) {
            // If regex filter fails (shouldn't with Pattern.quote), clear filter safely.
            rf = null;
        }

        sorter.setRowFilter(rf);
    }

    private void applyClientSort() {
        int col = switch ((String) sortByCombo.getSelectedItem()) {
            case "Company" -> 1;
            case "Position" -> 2;
            case "Location" -> 3;
            case "Status" -> 4;
            case "Applied" -> 5;
            case "ID" -> 0;
            default -> 0;
        };

        SortOrder order = "Desc".equals(sortDirCombo.getSelectedItem())
                ? SortOrder.DESCENDING
                : SortOrder.ASCENDING;

        sorter.setSortKeys(List.of(new RowSorter.SortKey(col, order)));
        sorter.sort();
    }

    private void browseCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose CSV file");
        chooser.setSelectedFile(new java.io.File(csvPathField.getText()));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            csvPathField.setText(chooser.getSelectedFile().getPath());
        }
    }

    private void loadCsv() {
        String pathText = csvPathField.getText().trim();
        if (pathText.isEmpty()) {
            showError("CSV path is empty.");
            return;
        }
        try {
            int count = service.loadApplicationsFromCsv(Path.of(pathText));
            refreshTable();
            showInfo("Loaded " + count + " applications from CSV.");
        } catch (IOException e) {
            showError("Failed to load CSV: " + e.getMessage());
        }
    }

    private void saveCsv() {
        String pathText = csvPathField.getText().trim();
        if (pathText.isEmpty()) {
            showError("CSV path is empty.");
            return;
        }
        try {
            int count = service.exportApplicationsToCsv(Path.of(pathText));
            showInfo("Saved " + count + " applications to CSV.");
        } catch (IOException e) {
            showError("Failed to save CSV: " + e.getMessage());
        }
    }

    private void addApplication() {
        String company = companyField.getText().trim();
        String position = positionField.getText().trim();
        String location = locationField.getText().trim();
        ApplicationStatus status = (ApplicationStatus) statusCombo.getSelectedItem();
        if (status == null) status = ApplicationStatus.APPLIED;

        if (company.isEmpty() || position.isEmpty() || location.isEmpty()) {
            showError("Company, Position, and Location are required.");
            return;
        }

        LocalDate applied = LocalDate.now();
        String dateText = dateField.getText().trim();
        if (!dateText.isEmpty()) {
            try {
                applied = LocalDate.parse(dateText);
            } catch (DateTimeParseException e) {
                showError("Invalid date. Use YYYY-MM-DD.");
                return;
            }
        }

        JobApplication created = service.addApplication(company, position, location, status, applied);
        refreshTable();
        clearForm();
        showInfo("Added application #" + created.getId() + ".");
    }

    private void clearForm() {
        companyField.setText("");
        positionField.setText("");
        locationField.setText("");
        dateField.setText("");
        statusCombo.setSelectedItem(ApplicationStatus.APPLIED);
        updateStatusCombo.setSelectedItem(ApplicationStatus.APPLIED);
    }

    private void updateSelectedStatus() {
        Integer id = getSelectedId();
        if (id == null) {
            showError("Select a row to update.");
            return;
        }
        ApplicationStatus newStatus = (ApplicationStatus) updateStatusCombo.getSelectedItem();
        if (newStatus == null) newStatus = ApplicationStatus.APPLIED;

        boolean ok = service.updateStatus(id, newStatus);
        if (!ok) {
            showError("Application ID " + id + " not found.");
            return;
        }
        refreshTable();
        showInfo("Updated application #" + id + " to " + newStatus + ".");
    }

    private void deleteSelected() {
        Integer id = getSelectedId();
        if (id == null) {
            showError("Select a row to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete application #" + id + "?",
                "Confirm delete",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (confirm != JOptionPane.OK_OPTION) return;

        boolean ok = service.deleteApplication(id);
        if (!ok) {
            showError("Application ID " + id + " not found.");
            return;
        }
        refreshTable();
        showInfo("Deleted application #" + id + ".");
    }

    private Integer getSelectedId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object val = tableModel.getValueAt(modelRow, 0);
        if (val == null) return null;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Small helper to avoid boilerplate DocumentListener.
     */
    @FunctionalInterface
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);

        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
