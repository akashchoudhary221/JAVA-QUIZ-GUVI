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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;

/**
 * Admin screen for creating / updating / deleting users and changing roles.
 *
 * Demonstrates:
 *  - CRUD operations through UserService (interface UserOperations)
 *  - JTable + JScrollPane for the user list
 *  - JComboBox for role selection
 *  - custom exceptions shown with JOptionPane
 */
public class UserManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final UserService userService = new UserService();

    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField passwordField = new JTextField(18);
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());

    private final DefaultTableModel tableModel =
            UIUtil.emptyModel("ID", "Name", "Email", "Role", "Created");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private int selectedUserId = -1;   // -1 => "create mode"

    public UserManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());
        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gc, 0, "Name:", nameField);
        addRow(form, gc, 1, "Email:", emailField);
        addRow(form, gc, 2, "Password:", passwordField);
        addRow(form, gc, 3, "Role:", roleCombo);

        JLabel hint = new JLabel("<html><small>Password: leave empty on update to keep the old one. "
                + "Minimum 6 characters.</small></html>");
        hint.setFont(UIUtil.smallFont());
        hint.setForeground(Color.GRAY);
        gc.gridx = 1;
        gc.gridy = 4;
        form.add(hint, gc);
        return form;
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row, String label, java.awt.Component field) {
        gc.gridx = 0;
        gc.gridy = row;
        form.add(UIUtil.fieldLabel(label), gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton create = UIUtil.successButton("Create");
        JButton update = UIUtil.primaryButton("Update");
        JButton delete = UIUtil.dangerButton("Delete");
        JButton clear = UIUtil.neutralButton("Clear");
        JButton refresh = UIUtil.neutralButton("Refresh");

        create.addActionListener(e -> createUser());
        update.addActionListener(e -> updateUser());
        delete.addActionListener(e -> deleteUser());
        clear.addActionListener(e -> clearForm());
        refresh.addActionListener(e -> refreshTable());

        panel.add(create);
        panel.add(update);
        panel.add(delete);
        panel.add(clear);
        panel.add(refresh);
        return panel;
    }
    /** Reloads the JTable from the database (JDBC through the DAO layer). */
    private void refreshTable() {
        try {
            tableModel.setRowCount(0);
            for (User user : userService.getAllUsers()) {
                tableModel.addRow(new Object[]{
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().getLabel(),
                        user.getCreatedAt() == null ? "-" : DATE_FORMAT.format(user.getCreatedAt())
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load users:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        selectedUserId = (int) tableModel.getValueAt(row, 0);
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        emailField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        passwordField.setText("");   // never show stored hashes
        String roleText = String.valueOf(tableModel.getValueAt(row, 3));
        for (int i = 0; i < roleCombo.getItemCount(); i++) {
            if (roleCombo.getItemAt(i).getLabel().equals(roleText)) {
                roleCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clearForm() {
        selectedUserId = -1;
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        roleCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    /**
     * Builds a polymorphic User object from the form values -
     * the concrete class depends on the selected role.
     */
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
            // Demonstrates custom exception handling in the GUI
            JOptionPane.showMessageDialog(this, e.getMessage(), "Create failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUser() {
        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a user in the table first.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, e.getMessage(), "Update failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a user in the table first.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete user #" + selectedUserId + "? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            userService.deleteUser(selectedUserId);
            JOptionPane.showMessageDialog(this, "User deleted.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshTable();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Delete failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}