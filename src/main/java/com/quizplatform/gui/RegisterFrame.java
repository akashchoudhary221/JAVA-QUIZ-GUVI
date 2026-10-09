package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.service.AuthenticationService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Modern participant self-registration window (RegisterFrame).
 * Validation errors are thrown as InvalidUserException by the service
 * and shown with JOptionPane.
 */
public class RegisterFrame extends JFrame {

    private final LoginFrame parent;
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmField = new JPasswordField(20);
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    private final AuthenticationService authService = new AuthenticationService();

    public RegisterFrame(LoginFrame parent) {
        this.parent = parent;
        setTitle("Register Participant Account - Java-Based Online Quiz Platform");
        setSize(580, 540);
        setMinimumSize(new Dimension(540, 500));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtil.BACKGROUND);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JLabel title = new JLabel("Create Participant Account");
        title.setFont(UIUtil.titleFont());
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Register to take quizzes, track performance, and receive faculty feedback");
        subtitle.setFont(UIUtil.smallFont());
        subtitle.setForeground(new Color(0x94, 0xA3, 0xB8));

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIUtil.BACKGROUND);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 30, 8, 30));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(20, 28, 20, 28)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;

        String[] labels = {"Full Name:", "Email Address:", "Password:", "Confirm Password:"};
        JTextField[] fields = {nameField, emailField, passwordField, confirmField};

        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0;
            gc.gridy = i;
            card.add(UIUtil.fieldLabel(labels[i]), gc);
            gc.gridx = 1;
            fields[i].setFont(UIUtil.normalFont());
            fields[i].setPreferredSize(new Dimension(220, 32));
            card.add(fields[i], gc);
        }

        JButton registerButton = UIUtil.successButton("Register Now");
        JButton backButton = UIUtil.secondaryButton("Back to Sign In");

        gc.gridx = 0;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(18, 6, 6, 6);

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(registerButton);
        buttons.add(backButton);
        card.add(buttons, gc);

        wrapper.add(card);

        registerButton.addActionListener(e -> performRegister());
        backButton.addActionListener(e -> backToLogin());
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtil.BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        messageLabel.setFont(UIUtil.smallBoldFont());
        messageLabel.setForeground(UIUtil.DANGER);
        footer.add(messageLabel, BorderLayout.CENTER);
        return footer;
    }

    private void performRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        try {
            if (!password.equals(confirm)) {
                throw new InvalidUserException("Passwords do not match.");
            }
            authService.registerParticipant(name, email, password);
            JOptionPane.showMessageDialog(this,
                    "Registration successful!\nYou can now sign in with your email: " + email,
                    "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } catch (InvalidUserException e) {
            messageLabel.setText(e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (DatabaseException e) {
            messageLabel.setText("Registration failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Database failure:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void backToLogin() {
        dispose();
        parent.setVisible(true);
    }
}
