package com.johndoan.jobtracker.ui;

import com.johndoan.jobtracker.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class JobTrackerFrame extends JFrame {

    private final ApplicationService service;

    private final DefaultTableModel tableModel;
    private final JTable table;

    // Search controls (Stage 6)
    private final JTextField searchCompanyField = new JTextField();
    private final JTextField searchPositionField = new JTextField();
    private final JTextField searchLocationField = new JTextField();
    private final JComboBox<String> searchStatusBox = new JComboBox<>();
    private final JTextField dateFromField = new JTextField();
    private final JTextField dateToField = new JTextField();
    private final JComboBox<SortField> sortFieldBox = new JComboBox<>(SortField.values());
    private final JComboBox<SortDirection> sortDirBox = new JComboBox<>(SortDirection.values());

    // Entry controls
    private final JTextField companyField = new JTextField();
    private final JTextField positionField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JComboBox<ApplicationStatus> statusBox = new JComboBox<>(ApplicationStatus.values());
    private final JTextField dateField = new JTextField();

    // CSV controls (optional)
    private final JTextField csvPathField = new JTextField("CSV/job_applications.csv");

    public JobTrackerFrame(ApplicationService service, String title) {
        super(title);
        this.service = service;

        this.tableModel = new DefaultTableModel(new Object[]{
                "#", "ID", "Company", "Position", "Location", "Status", "Applied"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        this.table = new JTable(tableModel);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1050, 700);
        setLocationRelativeTo(null);

        buildLayout();
        // Start empty; user clicks Refresh/Search.
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("Job Application Tracker");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 28f));
        root.add(header, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        root.add(scrollPane, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(buildSearchPanel());
        south.add(Box.createVerticalStrut(8));
        south.add(buildCsvPanel());
        south.add(Box.createVerticalStrut(8));
        south.add(buildEntryPanel());

        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildSearchPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Search / Sort (Stage 6)"));

        searchStatusBox.addItem("ALL");
        for (ApplicationStatus s : ApplicationStatus.values()) {
            searchStatusBox.addItem(s.name());
        }
        sortFieldBox.setSelectedItem(SortField.DATE_APPLIED);
        sortDirBox.setSelectedItem(SortDirection.DESC);

        JButton searchBtn = new JButton("Search");
        JButton clearSearchBtn = new JButton("Clear Search");
        JButton refreshBtn = new JButton("Refresh");
        JButton clearViewBtn = new JButton("Clear View");

        searchBtn.addActionListener(e -> handleSearch());
        clearSearchBtn.addActionListener(e -> {
            searchCompanyField.setText("");
            searchPositionField.setText("");
            searchLocationField.setText("");
            searchStatusBox.setSelectedItem("ALL");
            dateFromField.setText("");
            dateToField.setText("");
            sortFieldBox.setSelectedItem(SortField.DATE_APPLIED);
            sortDirBox.setSelectedItem(SortDirection.DESC);
        });
        refreshBtn.addActionListener(e -> loadAllIntoTable());
        clearViewBtn.addActionListener(e -> clearTable());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        g.gridy = r;

        g.gridx = 0; p.add(new JLabel("Company contains:"), g);
        g.gridx = 1; g.weightx = 1; p.add(searchCompanyField, g);

        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Position contains:"), g);
        g.gridx = 3; g.weightx = 1; p.add(searchPositionField, g);

        r++;
        g.gridy = r;

        g.gridx = 0; g.weightx = 0; p.add(new JLabel("Location contains:"), g);
        g.gridx = 1; g.weightx = 1; p.add(searchLocationField, g);

        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Status:"), g);
        g.gridx = 3; g.weightx = 1; p.add(searchStatusBox, g);

        r++;
        g.gridy = r;

        g.gridx = 0; g.weightx = 0; p.add(new JLabel("Date from (YYYY-MM-DD):"), g);
        g.gridx = 1; g.weightx = 1; p.add(dateFromField, g);

        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Date to (YYYY-MM-DD):"), g);
        g.gridx = 3; g.weightx = 1; p.add(dateToField, g);

        r++;
        g.gridy = r;

        g.gridx = 0; g.weightx = 0; p.add(new JLabel("Sort by:"), g);
        g.gridx = 1; g.weightx = 1; p.add(sortFieldBox, g);

        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Direction:"), g);
        g.gridx = 3; g.weightx = 1; p.add(sortDirBox, g);

        r++;
        g.gridy = r;
        g.gridx = 0; g.gridwidth = 4; g.weightx = 1;

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.add(searchBtn);
        btnRow.add(refreshBtn);
        btnRow.add(clearViewBtn);
        btnRow.add(clearSearchBtn);

        p.add(btnRow, g);

        return p;
    }

    private JPanel buildCsvPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("CSV Import/Export (optional)"));

        JButton browseBtn = new JButton("Browse...");
        JButton loadBtn = new JButton("Load CSV");
        JButton saveBtn = new JButton("Save CSV");

        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File(csvPathField.getText()));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                csvPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        loadBtn.addActionListener(e -> handleLoadCsv());
        saveBtn.addActionListener(e -> handleSaveCsv());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        p.add(new JLabel("CSV path:"), g);

        g.gridx = 1; g.gridy = 0; g.weightx = 1;
        p.add(csvPathField, g);

        g.gridx = 2; g.gridy = 0; g.weightx = 0;
        p.add(browseBtn, g);

        g.gridx = 3; g.gridy = 0;
        p.add(loadBtn, g);

        g.gridx = 4; g.gridy = 0;
        p.add(saveBtn, g);

        return p;
    }

    private JPanel buildEntryPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Add / Update / Delete"));

        JButton addBtn = new JButton("Add");
        JButton updateStatusBtn = new JButton("Update Status");
        JButton deleteBtn = new JButton("Delete Selected");

        addBtn.addActionListener(e -> handleAdd());
        updateStatusBtn.addActionListener(e -> handleUpdateStatus());
        deleteBtn.addActionListener(e -> handleDelete());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        g.gridy = r;

        g.gridx = 0; g.weightx = 0; p.add(new JLabel("Company"), g);
        g.gridx = 1; g.weightx = 1; p.add(companyField, g);

        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Position"), g);
        g.gridx = 3; g.weightx = 1; p.add(positionField, g);

        r++;
        g.gridy = r;

        g.gridx = 0; g.weightx = 0; p.add(new JLabel("Location"), g);
        g.gridx = 1; g.weightx = 1; p.add(locationField, g);

        g.gridx = 2; g.weightx = 0; p.add(new JLabel("Status"), g);
        g.gridx = 3; g.weightx = 1; p.add(statusBox, g);

        r++;
        g.gridy = r;

        g.gridx = 0; g.weightx = 0; p.add(new JLabel("Applied date (YYYY-MM-DD)"), g);
        g.gridx = 1; g.weightx = 1; p.add(dateField, g);

        r++;
        g.gridy = r;
        g.gridx = 0; g.gridwidth = 4; g.weightx = 1;

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.add(addBtn);
        btnRow.add(updateStatusBtn);
        btnRow.add(deleteBtn);

        p.add(btnRow, g);

        return p;
    }

    private void handleSearch() {
        try {
            ApplicationStatus status = null;
            String statusSel = (String) searchStatusBox.getSelectedItem();
            if (statusSel != null && !"ALL".equalsIgnoreCase(statusSel)) {
                status = ApplicationStatus.valueOf(statusSel);
            }

            LocalDate from = parseOptionalDate(dateFromField.getText().trim(), "Date from");
            LocalDate to = parseOptionalDate(dateToField.getText().trim(), "Date to");

            SearchFilter filter = new SearchFilter(
                    blankToNull(searchCompanyField.getText()),
                    blankToNull(searchPositionField.getText()),
                    blankToNull(searchLocationField.getText()),
                    status,
                    from,
                    to,
                    (SortField) sortFieldBox.getSelectedItem(),
                    (SortDirection) sortDirBox.getSelectedItem()
            );

            List<JobApplication> results = service.search(filter);
            setTableRows(results);

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No results found.", "Search", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    private void handleAdd() {
        try {
            String company = companyField.getText();
            String position = positionField.getText();
            String location = locationField.getText();
            ApplicationStatus status = (ApplicationStatus) statusBox.getSelectedItem();
            LocalDate date = parseOptionalDate(dateField.getText().trim(), "Applied date");
            if (date == null) date = LocalDate.now();

            service.addApplication(company, position, location, status, date);
            clearEntryFields();
            loadAllIntoTable();

        } catch (Exception ex) {
            showError("Add failed: " + ex.getMessage());
        }
    }

    private void handleUpdateStatus() {
        Integer id = getSelectedId();
        if (id == null) return;

        ApplicationStatus newStatus = (ApplicationStatus) JOptionPane.showInputDialog(
                this,
                "New status:",
                "Update Status",
                JOptionPane.PLAIN_MESSAGE,
                null,
                ApplicationStatus.values(),
                ApplicationStatus.APPLIED
        );

        if (newStatus == null) return;

        try {
            boolean ok = service.updateStatus(id, newStatus);
            if (!ok) {
                showError("Row not found (it may have been deleted).");
            }
            loadAllIntoTable();
        } catch (Exception ex) {
            showError("Update failed: " + ex.getMessage());
        }
    }

    private void handleDelete() {
        Integer id = getSelectedId();
        if (id == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete application ID " + id + "?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = service.deleteApplication(id);
            if (!ok) {
                showError("Row not found (it may have been deleted).");
            }
            loadAllIntoTable();
        } catch (Exception ex) {
            showError("Delete failed: " + ex.getMessage());
        }
    }

    private void handleLoadCsv() {
        try {
            Path p = Path.of(csvPathField.getText().trim()).toAbsolutePath();
            int count = service.loadApplicationsFromCsv(p);
            loadAllIntoTable();
            JOptionPane.showMessageDialog(this, "Imported " + count + " rows from CSV.", "CSV Load", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("CSV load failed: " + ex.getMessage());
        } catch (Exception ex) {
            showError("CSV load failed: " + ex.getMessage());
        }
    }

    private void handleSaveCsv() {
        try {
            Path p = Path.of(csvPathField.getText().trim()).toAbsolutePath();
            int count = service.exportApplicationsToCsv(p);
            JOptionPane.showMessageDialog(this, "Exported " + count + " rows to CSV.", "CSV Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("CSV save failed: " + ex.getMessage());
        } catch (Exception ex) {
            showError("CSV save failed: " + ex.getMessage());
        }
    }

    public void loadAllIntoTable() {
        try {
            setTableRows(service.listAll());
        } catch (Exception ex) {
            showError("Refresh failed: " + ex.getMessage());
        }
    }

    private void setTableRows(List<JobApplication> apps) {
        clearTable();
        int rowNum = 1;
        for (JobApplication app : apps) {
            tableModel.addRow(new Object[]{
                    rowNum++,
                    app.getId(),
                    app.getCompany(),
                    app.getPosition(),
                    app.getLocation(),
                    app.getStatus().name(),
                    app.getDateApplied().toString()
            });
        }
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    private Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "No selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Object idObj = tableModel.getValueAt(row, 1); // column 1 is ID
        try {
            return Integer.parseInt(String.valueOf(idObj));
        } catch (Exception e) {
            showError("Invalid ID in selected row.");
            return null;
        }
    }

    private void clearEntryFields() {
        companyField.setText("");
        positionField.setText("");
        locationField.setText("");
        statusBox.setSelectedItem(ApplicationStatus.APPLIED);
        dateField.setText("");
    }

    private static LocalDate parseOptionalDate(String input, String label) {
        if (input == null || input.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(input.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " must be YYYY-MM-DD.");
        }
    }

    private static String blankToNull(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isEmpty() ? null : s;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
