package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.model.Participant;
import com.quizplatform.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Participant: view profile data and change name / password.
 * The service keeps the old password hash when the password fields stay empty.
 */
public class ProfilePanel extends JPanel {

    private final ParticipantDashboard dashboard;
    private final Participant participant;
    private final UserService userService = new UserService();

    private final JTextField nameField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField newPasswordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);

    public ProfilePanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        this.participant = participant;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        loadValues();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(9, 6, 9, 6);
        gc.anchor = GridBagConstraints.WEST;

        emailField.setEditable(false);
        emailField.setBackground(new Color(240, 240, 240));

        addRow(form, gc, 0, "Name:", nameField);
        addRow(form, gc, 1, "Email (login):", emailField);
        addRow(form, gc, 2, "Role:", new JLabel(participant.getRole().getLabel()));
        addRow(form, gc, 3, "New Password:", newPasswordField);
        addRow(form, gc, 4, "Confirm Password:", confirmPasswordField);

        JLabel hint = new JLabel("<html><small>Leave the password fields empty to keep your "
                + "current password.</small></html>");
        hint.setFont(UIUtil.smallFont());
        hint.setForeground(Color.GRAY);
        gc.gridx = 1;
        gc.gridy = 5;
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
        JButton save = UIUtil.successButton("Save Changes");
        JButton reload = UIUtil.neutralButton("Reload");
        save.addActionListener(e -> saveProfile());
        reload.addActionListener(e -> loadValues());
        panel.add(save);
        panel.add(reload);
        return panel;
    }

    private void loadValues() {
        nameField.setText(participant.getName());
        emailField.setText(participant.getEmail());
        newPasswordField.setText("");
        confirmPasswordField.setText("");
    }

    private void saveProfile() {
        try {
            String newPassword = new String(newPasswordField.getPassword());
            String confirm = new String(confirmPasswordField.getPassword());
            if (!newPassword.isEmpty() && !newPassword.equals(confirm)) {
                throw new InvalidUserException("Passwords do not match.");
            }
            // blank password -> UserService keeps the existing hash
            Participant updated = new Participant(participant.getId(),
                    nameField.getText().trim(), participant.getEmail(), newPassword);
            userService.updateUser(updated);

            participant.setName(updated.getName());
            JOptionPane.showMessageDialog(this, "Profile updated successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dashboard.refreshStatistics();
        } catch (InvalidUserException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Update failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
