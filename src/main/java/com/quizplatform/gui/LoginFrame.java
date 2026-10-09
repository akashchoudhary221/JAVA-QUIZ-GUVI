package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.model.User;
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
 * Modern login window.
 *
 * Demonstrates:
 *  - Swing GUI (JFrame, JPanel, JTextField, JPasswordField, JButton)
 *  - Exception handling (try / catch of custom exceptions -> JOptionPane)
 *  - RUNTIME POLYMORPHISM: after a successful login the User reference
 *    decides which dashboard window is opened (user.displayDashboard()).
 */
public class LoginFrame extends JFrame {

    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    private final AuthenticationService authService = new AuthenticationService();

    public LoginFrame() {
        setTitle("Java-Based Online Quiz Platform - Sign In");
        setSize(580, 520);
        setMinimumSize(new Dimension(540, 480));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Java-Based Online Quiz Platform");
        title.setFont(UIUtil.titleFont());
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Sign in to access your dashboard, quizzes, and performance analytics");
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
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(24, 30, 24, 30)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        card.add(UIUtil.fieldLabel("Email Address:"), gc);
        gc.gridx = 1;
        emailField.setFont(UIUtil.normalFont());
        emailField.setPreferredSize(new Dimension(240, 34));
        card.add(emailField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        card.add(UIUtil.fieldLabel("Password:"), gc);
        gc.gridx = 1;
        passwordField.setFont(UIUtil.normalFont());
        passwordField.setPreferredSize(new Dimension(240, 34));
        card.add(passwordField, gc);

        JButton loginButton = UIUtil.primaryButton("Sign In");
        JButton registerButton = UIUtil.successButton("Register");
        JButton exitButton = UIUtil.neutralButton("Exit");

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(18, 6, 8, 6);

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(loginButton);
        buttons.add(registerButton);
        buttons.add(exitButton);
        card.add(buttons, gc);

        JPanel hintCard = new JPanel();
        hintCard.setLayout(new BoxLayout(hintCard, BoxLayout.Y_AXIS));
        hintCard.setBackground(new Color(0xF8, 0xFA, 0xFC));
        hintCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel hintTitle = new JLabel("Demo Accounts:");
        hintTitle.setFont(UIUtil.smallBoldFont());
        hintTitle.setForeground(UIUtil.TEXT);

        JLabel hintText = new JLabel("<html>Admin: <b>admin@quiz.com</b> / admin123 &nbsp;|&nbsp; "
                + "Creator: <b>creator@quiz.com</b> / creator123<br>"
                + "Participant: <b>student@quiz.com</b> / student123</html>");
        hintText.setFont(UIUtil.smallFont());
        hintText.setForeground(UIUtil.TEXT_MUTED);

        hintCard.add(hintTitle);
        hintCard.add(Box.createVerticalStrut(2));
        hintCard.add(hintText);

        gc.gridy = 3;
        gc.insets = new Insets(16, 6, 4, 6);
        card.add(hintCard, gc);

        wrapper.add(card);

        // Action listeners
        loginButton.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());
        registerButton.addActionListener(e -> {
            new RegisterFrame(this).setVisible(true);
            setVisible(false);
        });
        exitButton.addActionListener(e -> System.exit(0));

        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtil.BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));

        messageLabel.setFont(UIUtil.smallBoldFont());
        messageLabel.setForeground(UIUtil.DANGER);
        footer.add(messageLabel, BorderLayout.CENTER);
        return footer;
    }

    private void performLogin() {
        messageLabel.setText(" ");
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            User user = authService.login(email, password);

            // Demonstrates RUNTIME POLYMORPHISM:
            // user is declared as User, but the concrete class (Admin /
            // QuizCreator / Participant) decides which dashboard opens.
            user.displayDashboard();
            dispose();

        } catch (InvalidLoginException e) {
            messageLabel.setText(e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sign In Failed", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseException e) {
            messageLabel.setText("Database unavailable - check MySQL connection.");
            JOptionPane.showMessageDialog(this,
                    "Database connection failure:\n" + e.getMessage() + "\nPlease check config.properties.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
