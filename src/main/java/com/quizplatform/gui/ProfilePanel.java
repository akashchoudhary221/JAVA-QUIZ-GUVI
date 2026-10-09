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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Participant: view and manage personal profile data and update account password.
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
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Student Profile & Account Settings"), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel("Update your display name or set a new password. Email is fixed as your unique login ID."), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        loadValues();
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIUtil.BACKGROUND);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(22, 28, 22, 28)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;

        emailField.setEditable(false);
        emailField.setBackground(new Color(0xF1, 0xF5, 0xF9));

        addRow(card, gc, 0, "Display Name:", nameField);
        addRow(card, gc, 1, "Registered Email:", emailField);
        addRow(card, gc, 2, "Assigned Role:", UIUtil.statusBadge(participant.getRole().getLabel()));
        addRow(card, gc, 3, "New Password:", newPasswordField);
        addRow(card, gc, 4, "Confirm Password:", confirmPasswordField);

        JLabel hint = new JLabel("<html><small style='color: #64748B;'>Leave the password fields blank if you do not want to change your current password.</small></html>");
        gc.gridx = 1;
        gc.gridy = 5;
        card.add(hint, gc);

        wrapper.add(card);
        return wrapper;
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row, String label, java.awt.Component field) {
        gc.gridx = 0;
        gc.gridy = row;
        form.add(UIUtil.fieldLabel(label), gc);
        gc.gridx = 1;
        if (field instanceof JTextField tf) {
            tf.setFont(UIUtil.normalFont());
            tf.setPreferredSize(new Dimension(250, 32));
        }
        form.add(field, gc);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton save = UIUtil.successButton("💾 Save Profile Changes");
        JButton reload = UIUtil.secondaryButton("Reset Changes");

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
                throw new InvalidUserException("Password confirmation does not match new password.");
            }
            Participant updated = new Participant(participant.getId(),
                    nameField.getText().trim(), participant.getEmail(), newPassword);
            userService.updateUser(updated);

            participant.setName(updated.getName());
            JOptionPane.showMessageDialog(this, "Profile updated successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dashboard.refreshStatistics();
        } catch (InvalidUserException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Update Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
