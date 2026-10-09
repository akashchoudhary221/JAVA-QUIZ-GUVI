package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.model.User;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Login window.
 *
 * Demonstrates:
 *  - Swing GUI (JFrame, JPanel, JTextField, JPasswordField, JButton)
 *  - exception handling (try / catch of custom exceptions -> JOptionPane)
 *  - RUNTIME POLYMORPHISM: after a successful login the User reference
 *    decides which dashboard window is opened (user.displayDashboard()).
 */
public class LoginFrame extends JFrame {

    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel messageLabel = new JLabel(" ");

    private final AuthenticationService authService = new AuthenticationService();

    public LoginFrame() {
        setTitle("Java-Based Online Quiz Platform - Login");
        setSize(560, 480);
        setMinimumSize(new Dimension(520, 440));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
        JLabel title = new JLabel("Java-Based Online Quiz Platform");
        title.setFont(UIUtil.titleFont());
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Sign in with your email and password");
        subtitle.setFont(UIUtil.smallFont());
        subtitle.setForeground(new Color(200, 220, 240));
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        add(buildForm(), BorderLayout.CENTER);

        messageLabel.setFont(UIUtil.smallFont());
        messageLabel.setForeground(UIUtil.DANGER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        add(messageLabel, BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(30, 60, 20, 60));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 6, 8, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        form.add(UIUtil.fieldLabel("Email:"), gc);
        gc.gridx = 1;
        form.add(emailField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        form.add(UIUtil.fieldLabel("Password:"), gc);
        gc.gridx = 1;
        form.add(passwordField, gc);

        JButton loginButton = UIUtil.primaryButton("Login");
        JButton registerButton = UIUtil.neutralButton("Register");
        JButton exitButton = UIUtil.dangerButton("Exit");

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(18, 6, 6, 6);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(loginButton);
        buttons.add(registerButton);
        buttons.add(exitButton);
        form.add(buttons, gc);

        JLabel hint = new JLabel("<html><center>Demo accounts:<br>"
                + "admin@quiz.com / admin123 &nbsp; | &nbsp; creator@quiz.com / creator123<br>"
                + "student@quiz.com / student123</center></html>");
        hint.setFont(UIUtil.smallFont());
        hint.setForeground(Color.GRAY);
        gc.gridy = 3;
        gc.insets = new Insets(20, 6, 6, 6);
        form.add(hint, gc);

        // action listeners
        loginButton.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());   // Enter key
        registerButton.addActionListener(e -> {
            new RegisterFrame(this).setVisible(true);
            setVisible(false);
        });
        exitButton.addActionListener(e -> System.exit(0));
        return form;
    }

    private void performLogin() {
        messageLabel.setText(" ");
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            // try / catch / throws with CUSTOM EXCEPTIONS
            User user = authService.login(email, password);

            // Demonstrates RUNTIME POLYMORPHISM:
            // user is declared as User, but the concrete class (Admin /
            // QuizCreator / Participant) decides which dashboard opens.
            user.displayDashboard();
            dispose();

        } catch (InvalidLoginException e) {
            messageLabel.setText(e.getMessage());                 // meaningful error message
            JOptionPaneError(e.getMessage());
        } catch (DatabaseException e) {
            messageLabel.setText("Database unavailable - check MySQL and config.properties.");
            JOptionPaneError(e.getMessage());
        }
    }

    private void JOptionPaneError(String message) {
        javax.swing.JOptionPane.showMessageDialog(this, message, "Login failed",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}
