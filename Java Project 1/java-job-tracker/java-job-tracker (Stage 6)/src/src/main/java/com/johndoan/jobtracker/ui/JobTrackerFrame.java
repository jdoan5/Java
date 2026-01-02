package com.johndoan.jobtracker.ui;

import com.johndoan.jobtracker.ApplicationRepository;
import com.johndoan.jobtracker.ApplicationService;
import com.johndoan.jobtracker.ApplicationStatus;
import com.johndoan.jobtracker.JobApplication;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Stage 5:
 * - Search by company/position
 * - Sorting in UI (whitelisted fields)
 * - Stronger validation + messages
 */
public class JobTrackerFrame extends JFrame {

    private final ApplicationService service;

    private final DefaultTableModel tableModel;
    private final JTable table;

    // Filters / controls
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<ApplicationStatus> statusFilter = new JComboBox<>(ApplicationStatus.values());

    private final JComboBox<String> sortField = new JComboBox<>(new String[]{
            "ID", "Company", "Position", "Location", "Status", "Applied Date"
    });
    private final JCheckBox sortAscending = new JCheckBox("Ascending", false);

    // Form fields
    private final JTextField companyField = new JTextField(18);
    private final JTextField positionField = new JTextField(18);
    private final JTextField locationField = new JTextField(18);
    private final JComboBox<ApplicationStatus> statusField = new JComboBox<>(ApplicationStatus.values());
    private final JTextField dateField = new JTextField(12);

    public JobTrackerFrame(ApplicationService service) {
        super("Job Application Tracker (Stage 5 - UI + Search/Sort)");
        this.service = service;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 560));
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Company", "Position", "Location", "Status", "Applied"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setContentPane(buildLayout());

        // Stage 5 default: show everything on launch
        refreshTable();
    }

    private JPanel buildLayout() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Job Application Tracker");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        root.add(title, BorderLayout.NORTH);

        // Center table
        JScrollPane scroll = new JScrollPane(table);
        root.add(scroll, BorderLayout.CENTER);

        // South panel combines filter row + form row
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(buildFilterRow());
        south.add(Box.createVerticalStrut(8));
        south.add(buildFormRow());

        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildFilterRow() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        int col = 0;

        g.gridx = col++; g.gridy = 0;
        p.add(new JLabel("Search (company/role):"), g);

        g.gridx = col++; g.gridy = 0;
        p.add(searchField, g);

        g.gridx = col++; g.gridy = 0;
        p.add(new JLabel("Status:"), g);

        g.gridx = col++; g.gridy = 0;
        p.add(statusFilter, g);

        g.gridx = col++; g.gridy = 0;
        p.add(new JLabel("Sort by:"), g);

        g.gridx = col++; g.gridy = 0;
        p.add(sortField, g);

        g.gridx = col++; g.gridy = 0;
        p.add(sortAscending, g);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> applySearchAndFilter());

        JButton clearViewBtn = new JButton("Clear View");
        clearViewBtn.addActionListener(e -> clearView());

        g.gridx = col++; g.gridy = 0;
        p.add(refreshBtn, g);

        g.gridx = col++; g.gridy = 0;
        p.add(applyBtn, g);

        g.gridx = col++; g.gridy = 0;
        p.add(clearViewBtn, g);

        return p;
    }

    private JPanel buildFormRow() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 1
        g.gridx = 0; g.gridy = 0;
        p.add(new JLabel("Company"), g);
        g.gridx = 1; g.gridy = 0;
        p.add(companyField, g);

        g.gridx = 2; g.gridy = 0;
        p.add(new JLabel("Position"), g);
        g.gridx = 3; g.gridy = 0;
        p.add(positionField, g);

        // Row 2
        g.gridx = 0; g.gridy = 1;
        p.add(new JLabel("Location"), g);
        g.gridx = 1; g.gridy = 1;
        p.add(locationField, g);

        g.gridx = 2; g.gridy = 1;
        p.add(new JLabel("Status"), g);
        g.gridx = 3; g.gridy = 1;
        p.add(statusField, g);

        // Row 3
        g.gridx = 0; g.gridy = 2;
        p.add(new JLabel("Applied date (YYYY-MM-DD)"), g);
        g.gridx = 1; g.gridy = 2;
        p.add(dateField, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> handleAdd());

        JButton updateBtn = new JButton("Update Status");
        updateBtn.addActionListener(e -> handleUpdateStatus());

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> handleDeleteSelected());

        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);

        g.gridx = 3; g.gridy = 2;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.EAST;
        p.add(buttons, g);

        return p;
    }

    private void refreshTable() {
        try {
            ApplicationRepository.SortSpec sort = getSortSpec();
            List<JobApplication> apps = service.listAll(sort);
            setTableData(apps);
        } catch (Exception ex) {
            showError("Refresh failed", ex.getMessage());
        }
    }

    private void applySearchAndFilter() {
        try {
            String query = searchField.getText();
            ApplicationStatus status = (ApplicationStatus) statusFilter.getSelectedItem();
            ApplicationRepository.SortSpec sort = getSortSpec();

            // Treat "no filter" as null if user cleared search and wants all statuses:
            // We'll interpret a special case: if statusFilter is null, show all.
            // (Combo box won't be null, so keep as-is.)
            List<JobApplication> apps = service.search(query, status, sort);
            setTableData(apps);
        } catch (Exception ex) {
            showError("Search/Filter failed", ex.getMessage());
        }
    }

    private void clearView() {
        tableModel.setRowCount(0);
        table.clearSelection();
    }

    private void handleAdd() {
        try {
            String company = companyField.getText();
            String position = positionField.getText();
            String location = locationField.getText();
            ApplicationStatus status = (ApplicationStatus) statusField.getSelectedItem();

            LocalDate applied = parseDateOrNull(dateField.getText());

            service.addApplication(company, position, location, status, applied);

            // After adding, refresh using current view filters (feels best)
            applySearchAndFilter();
            clearForm();
        } catch (IllegalArgumentException ex) {
            showError("Validation", ex.getMessage());
        } catch (Exception ex) {
            showError("Add failed", ex.getMessage());
        }
    }

    private void handleUpdateStatus() {
        Integer id = getSelectedId();
        if (id == null) {
            showInfo("Select a row first.");
            return;
        }
        try {
            ApplicationStatus newStatus = (ApplicationStatus) statusField.getSelectedItem();
            boolean ok = service.updateStatus(id, newStatus);
            if (!ok) showInfo("No row updated (ID not found).");
            applySearchAndFilter();
        } catch (Exception ex) {
            showError("Update failed", ex.getMessage());
        }
    }

    private void handleDeleteSelected() {
        Integer id = getSelectedId();
        if (id == null) {
            showInfo("Select a row first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete application ID " + id + "?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = service.deleteById(id);
            if (!ok) showInfo("No row deleted (ID not found).");
            applySearchAndFilter();
        } catch (Exception ex) {
            showError("Delete failed", ex.getMessage());
        }
    }

    private ApplicationRepository.SortSpec getSortSpec() {
        String selected = (String) sortField.getSelectedItem();
        boolean asc = sortAscending.isSelected();

        ApplicationRepository.SortField field = switch (selected) {
            case "Company" -> ApplicationRepository.SortField.COMPANY;
            case "Position" -> ApplicationRepository.SortField.POSITION;
            case "Location" -> ApplicationRepository.SortField.LOCATION;
            case "Status" -> ApplicationRepository.SortField.STATUS;
            case "Applied Date" -> ApplicationRepository.SortField.DATE_APPLIED;
            case "ID" -> ApplicationRepository.SortField.ID;
            default -> ApplicationRepository.SortField.ID;
        };

        return ApplicationRepository.SortSpec.by(field, asc);
    }

    private void setTableData(List<JobApplication> apps) {
        tableModel.setRowCount(0);
        for (JobApplication app : apps) {
            tableModel.addRow(new Object[]{
                    safeId(app),
                    app.getCompany(),
                    app.getPosition(),
                    app.getLocation(),
                    app.getStatus() != null ? app.getStatus().name() : "",
                    app.getDateApplied() != null ? app.getDateApplied().toString() : ""
            });
        }
    }

    private Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;

        Object v = tableModel.getValueAt(row, 0);
        if (v instanceof Integer) return (Integer) v;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void clearForm() {
        companyField.setText("");
        positionField.setText("");
        locationField.setText("");
        dateField.setText("");
        statusField.setSelectedItem(ApplicationStatus.APPLIED);
    }

    private LocalDate parseDateOrNull(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
        }
    }

    private int safeId(JobApplication app) {
        try {
            // Prefer getter if present
            var m = app.getClass().getMethod("getId");
            Object v = m.invoke(app);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
