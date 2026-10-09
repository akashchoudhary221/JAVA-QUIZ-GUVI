package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.service.AuthenticationService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Participant self-registration window (RegisterFrame).
 * Validation errors are thrown as InvalidUserException by the service
 * and shown with JOptionPane.
 */
public class RegisterFrame extends JFrame {

    private final LoginFrame parent;
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmField = new JPasswordField(20);
    private final JLabel messageLabel = new JLabel(" ");

    private final AuthenticationService authService = new AuthenticationService();

    public RegisterFrame(LoginFrame parent) {
        this.parent = parent;
        setTitle("Register - Java-Based Online Quiz Platform");
        setSize(560, 520);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel title = new JLabel("Create Participant Account");
        title.setFont(UIUtil.titleFont());
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);

        messageLabel.setFont(UIUtil.smallFont());
        messageLabel.setForeground(UIUtil.DANGER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 24, 12, 24));
        add(messageLabel, BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 50, 12, 50));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(9, 6, 9, 6);
        gc.anchor = GridBagConstraints.WEST;

        String[] labels = {"Full Name:", "Email:", "Password:", "Confirm Password:"};
        JPanel[] fields = {wrap(nameField), wrap(emailField), wrap(passwordField), wrap(confirmField)};
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0;
            gc.gridy = i;
            form.add(UIUtil.fieldLabel(labels[i]), gc);
            gc.gridx = 1;
            form.add(fields[i], gc);
        }

        JButton registerButton = UIUtil.successButton("Register");
        JButton backButton = UIUtil.neutralButton("Back to Login");
        gc.gridx = 0;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(20, 6, 6, 6);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(registerButton);
        buttons.add(backButton);
        form.add(buttons, gc);

        registerButton.addActionListener(e -> performRegister());
        backButton.addActionListener(e -> backToLogin());
        return form;
    }

    private JPanel wrap(JTextField field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void performRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        try {
            if (!password.equals(confirm)) {
                // Demonstrates custom exception handling for invalid input
                throw new InvalidUserException("Passwords do not match.");
            }
            authService.registerParticipant(name, email, password);
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Registration successful!\nYou can now login as: " + email,
                    "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } catch (InvalidUserException e) {
            messageLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            messageLabel.setText("Registration failed: " + e.getMessage());
        }
    }

    private void backToLogin() {
        dispose();
        parent.setVisible(true);
    }
}
