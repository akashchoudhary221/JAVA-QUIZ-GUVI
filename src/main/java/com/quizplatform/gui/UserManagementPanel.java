package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.model.Admin;
import com.quizplatform.model.Participant;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.model.Role;
import com.quizplatform.model.User;
import com.quizplatform.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for creating, updating, searching, and deleting users.
 * Features search filtering, role badge rendering, and clean form layout.
 */
public class UserManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final UserService userService = new UserService();

    private final JTextField searchField = new JTextField(18);
    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField passwordField = new JTextField(18);
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());

    private final DefaultTableModel tableModel =
            UIUtil.emptyModel("ID", "Name", "Email Address", "Assigned Role", "Registered On");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private int selectedUserId = -1;

    public UserManagementPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(buildTopBar(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setBackground(UIUtil.BACKGROUND);
        centerPanel.add(buildForm(), BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        add(buildButtons(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        configureTableRenderer();
        refreshTable();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(UIUtil.BACKGROUND);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setBackground(UIUtil.BACKGROUND);
        titles.add(UIUtil.heading("User Account Management"), BorderLayout.NORTH);
        titles.add(UIUtil.mutedLabel("Manage platform users, update credentials, and assign administrative roles"), BorderLayout.SOUTH);
        bar.add(titles, BorderLayout.WEST);

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchWrap.setBackground(UIUtil.BACKGROUND);
        JLabel searchIcon = new JLabel("🔍 Filter Users:");
        searchIcon.setFont(UIUtil.normalFont());
        searchIcon.setForeground(UIUtil.TEXT_MUTED);
        searchField.setFont(UIUtil.normalFont());
        searchField.setPreferredSize(new Dimension(200, 32));
        searchWrap.add(searchIcon);
        searchWrap.add(searchField);
        bar.add(searchWrap, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        addRow(card, gc, 0, "Full Name:", nameField);
        addRow(card, gc, 1, "Email Address:", emailField);
        addRow(card, gc, 2, "Password:", passwordField);
        addRow(card, gc, 3, "Account Role:", roleCombo);

        JLabel hint = new JLabel("<html><small style='color: #64748B;'>Leave password empty on update to retain existing password. Minimum 6 characters.</small></html>");
        gc.gridx = 1;
        gc.gridy = 4;
        card.add(hint, gc);

        return card;
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row, String label, Component field) {
        gc.gridx = 0;
        gc.gridy = row;
        form.add(UIUtil.fieldLabel(label), gc);
        gc.gridx = 1;
        field.setFont(UIUtil.normalFont());
        if (field instanceof JTextField tf) {
            tf.setPreferredSize(new Dimension(240, 32));
        }
        form.add(field, gc);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton create = UIUtil.successButton("➕ Create User");
        JButton update = UIUtil.primaryButton("💾 Update User");
        JButton delete = UIUtil.dangerButton("🗑 Delete User");
        JButton clear = UIUtil.secondaryButton("Clear Form");
        JButton refresh = UIUtil.secondaryButton("Refresh");

        create.addActionListener(e -> createUser());
        update.addActionListener(e -> updateUser());
        delete.addActionListener(e -> deleteUser());
        clear.addActionListener(e -> clearForm());
        refresh.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        panel.add(create);
        panel.add(update);
        panel.add(delete);
        panel.add(clear);
        panel.add(refresh);
        return panel;
    }

    private void configureTableRenderer() {
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                String text = value != null ? value.toString() : "";
                JLabel badge = UIUtil.statusBadge(text);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setBackground(isSelected ? UIUtil.SELECTED_ROW : (row % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xFA, 0xFC)));
                p.add(badge);
                return p;
            }
        });
    }

    public void refreshTable() {
        try {
            allUsers = userService.getAllUsers();
            applyFilter();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load users:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String filter = searchField.getText().trim().toLowerCase();
        filteredUsers = allUsers.stream()
                .filter(u -> filter.isEmpty()
                        || u.getName().toLowerCase().contains(filter)
                        || u.getEmail().toLowerCase().contains(filter)
                        || u.getRole().name().toLowerCase().contains(filter))
                .toList();

        tableModel.setRowCount(0);
        for (User user : filteredUsers) {
            tableModel.addRow(new Object[]{
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().getLabel(),
                    user.getCreatedAt() == null ? "-" : DATE_FORMAT.format(user.getCreatedAt())
            });
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredUsers.size()) {
            return;
        }
        User selected = filteredUsers.get(row);
        selectedUserId = selected.getId();
        nameField.setText(selected.getName());
        emailField.setText(selected.getEmail());
        passwordField.setText("");
        roleCombo.setSelectedItem(selected.getRole());
    }

    private void clearForm() {
        selectedUserId = -1;
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        roleCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    private User buildUserFromForm(int id) throws InvalidUserException {
        Role role = (Role) roleCombo.getSelectedItem();
        if (role == null) {
            throw new InvalidUserException("Please select a role.");
        }
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        return switch (role) {
            case ADMIN -> new Admin(id, name, email, password);
            case QUIZ_CREATOR -> new QuizCreator(id, name, email, password);
            case PARTICIPANT -> new Participant(id, name, email, password);
        };
    }

    private void createUser() {
        try {
            User user = buildUserFromForm(0);
            userService.createUser(user);
            JOptionPane.showMessageDialog(this,
                    "User created successfully:\n" + user.getName() + " (" + user.getRole().getLabel() + ")",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshTable();
        } catch (InvalidUserException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Create Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUser() {
        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a user from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            User user = buildUserFromForm(selectedUserId);
            userService.updateUser(user);
            JOptionPane.showMessageDialog(this, "User updated successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshTable();
        } catch (InvalidUserException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a user from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete user #" + selectedUserId + " (" + emailField.getText() + ")?\nThis action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            userService.deleteUser(selectedUserId);
            JOptionPane.showMessageDialog(this, "User deleted successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshTable();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}