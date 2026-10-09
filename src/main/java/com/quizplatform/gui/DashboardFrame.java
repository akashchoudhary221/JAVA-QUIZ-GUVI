package com.quizplatform.gui;

import com.quizplatform.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Base window of every dashboard (Admin / Quiz Creator / Participant).
 *
 * Demonstrates INHERITANCE inside the GUI layer as well and uses the
 * classic Swing navigation architecture:
 *
 *      NORTH  -> header bar (user name + role)
 *      WEST   -> sidebar with navigation buttons
 *      CENTER -> JPanel(CardLayout) holding one card per screen
 */
public abstract class DashboardFrame extends JFrame {

    protected final User currentUser;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel();
    private final JLabel statusLabel = new JLabel("Ready");

    protected DashboardFrame(User user, String windowTitle) {
        this.currentUser = user;
        setTitle("Java-Based Online Quiz Platform - " + windowTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 740);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        contentPanel.setBackground(UIUtil.BACKGROUND);

        add(buildHeader(windowTitle), BorderLayout.NORTH);
        sidebar.setLayout(new GridLayout(0, 1, 0, 6));
        sidebar.setBackground(UIUtil.PRIMARY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        sidebar.setPreferredSize(new Dimension(210, 100));
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader(String windowTitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel titleLabel = new JLabel(windowTitle);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);

        // role information is always visible - makes role-based access obvious in the viva
        JLabel userLabel = new JLabel(currentUser.getName() + "  |  "
                + currentUser.getRole().getLabel() + "  |  " + currentUser.getEmail(),
                SwingConstants.RIGHT);
        userLabel.setFont(UIUtil.smallFont());
        userLabel.setForeground(new Color(200, 220, 240));
        header.add(userLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(225, 228, 232));
        bar.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        statusLabel.setFont(UIUtil.smallFont());
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel footer = new JLabel("Java Swing + JDBC + MySQL  |  Java-Based Online Quiz Platform",
                SwingConstants.RIGHT);
        footer.setFont(UIUtil.smallFont());
        bar.add(footer, BorderLayout.EAST);
        return bar;
    }

    /** Registers a screen in the CardLayout. */
    protected void addCard(String cardName, JComponent card) {
        contentPanel.add(card, cardName);
    }

    /** Adds a button to the sidebar (called by the concrete dashboards). */
    protected void addSidebarButton(JButton button) {
        sidebar.add(button);
    }

    /** Sidebar navigation button - Demonstrates CardLayout navigation. */
    protected JButton createNavButton(String label, String cardName) {
        JButton button = new JButton(label);
        button.setFont(UIUtil.normalFont());
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(52, 73, 94));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        button.addActionListener(e -> showCard(cardName));
        return button;
    }

    protected JButton createLogoutButton() {
        JButton button = createNavButton("Logout", null);
        button.setBackground(UIUtil.DANGER);
        button.addActionListener(e -> {
            currentUser.logout();                       // Demonstrates Authenticatable.logout()
            dispose();
            new LoginFrame().setVisible(true);
        });
        return button;
    }

    /** Shows the card with the given name (called by panels as well). */
    public void showCard(String cardName) {
        cardLayout.show(contentPanel, cardName);
        statusLabel.setText("Screen: " + cardName);
    }

    protected void setStatus(String text) {
        statusLabel.setText(text);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
