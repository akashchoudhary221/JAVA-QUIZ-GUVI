package com.quizplatform.gui;

import com.quizplatform.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base window of every dashboard (Admin / Quiz Creator / Participant).
 *
 * Demonstrates INHERITANCE inside the GUI layer and implements a professional
 * dashboard architecture:
 *
 *      NORTH  -> Top header bar with title, user profile, role badge
 *      WEST   -> Sidebar with brand wordmark, active navigation state, logout
 *      CENTER -> CardLayout panel displaying role-specific screens
 *      SOUTH  -> Status bar with system readiness indicators
 */
public abstract class DashboardFrame extends JFrame {

    protected final User currentUser;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel navButtonContainer = new JPanel();
    private final JLabel statusLabel = new JLabel("System Ready");
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private String currentActiveCard = "";

    protected DashboardFrame(User user, String windowTitle) {
        this.currentUser = user;
        setTitle("Java-Based Online Quiz Platform - " + windowTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        contentPanel.setBackground(UIUtil.BACKGROUND);

        add(buildHeader(windowTitle), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader(String windowTitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtil.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        // Left: Title + subtitle
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(windowTitle);
        titleLabel.setFont(UIUtil.subTitleFont());
        titleLabel.setForeground(UIUtil.TEXT);

        JLabel subtitleLabel = new JLabel("Enterprise Academic Quiz Management Platform");
        subtitleLabel.setFont(UIUtil.smallFont());
        subtitleLabel.setForeground(UIUtil.TEXT_MUTED);

        left.add(titleLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitleLabel);
        header.add(left, BorderLayout.WEST);

        // Right: User Profile Chip + Role Badge
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(currentUser.getName());
        nameLabel.setFont(UIUtil.boldFont());
        nameLabel.setForeground(UIUtil.TEXT);

        JLabel emailLabel = new JLabel("(" + currentUser.getEmail() + ")");
        emailLabel.setFont(UIUtil.smallFont());
        emailLabel.setForeground(UIUtil.TEXT_MUTED);

        JLabel roleBadge = UIUtil.statusBadge(currentUser.getRole().getLabel());

        right.add(nameLabel);
        right.add(emailLabel);
        right.add(roleBadge);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIUtil.PRIMARY);
        sidebar.setPreferredSize(new Dimension(230, 100));

        // Brand Wordmark
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBackground(UIUtil.PRIMARY_DARK);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel brandTitle = new JLabel("⚡ QuizPlatform");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandTitle.setForeground(Color.WHITE);

        JLabel brandSub = new JLabel("Java Core & Swing Suite");
        brandSub.setFont(UIUtil.smallFont());
        brandSub.setForeground(new Color(0x94, 0xA3, 0xB8));

        brandPanel.add(brandTitle);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(brandSub);
        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Navigation button container in Center
        navButtonContainer.setLayout(new BoxLayout(navButtonContainer, BoxLayout.Y_AXIS));
        navButtonContainer.setBackground(UIUtil.PRIMARY);
        navButtonContainer.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        sidebar.add(navButtonContainer, BorderLayout.CENTER);

        // Bottom: Logout button
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UIUtil.PRIMARY);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));
        JButton logoutBtn = createLogoutButton();
        bottom.add(logoutBtn, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtil.BORDER),
                BorderFactory.createEmptyBorder(6, 20, 6, 20)));

        statusLabel.setFont(UIUtil.smallFont());
        statusLabel.setForeground(UIUtil.TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel footer = new JLabel("Java 17 / Swing / JDBC / MySQL / Servlets", SwingConstants.RIGHT);
        footer.setFont(UIUtil.smallFont());
        footer.setForeground(UIUtil.TEXT_MUTED);
        bar.add(footer, BorderLayout.EAST);
        return bar;
    }

    /** Registers a screen in the CardLayout. */
    protected void addCard(String cardName, JComponent card) {
        contentPanel.add(card, cardName);
    }

    /** Adds a button to the sidebar. */
    protected void addSidebarButton(JButton button) {
        navButtonContainer.add(button);
        navButtonContainer.add(Box.createVerticalStrut(6));
    }

    /** Sidebar navigation button with active state highlighting. */
    protected JButton createNavButton(String label, String cardName) {
        JButton button = new JButton(label);
        button.setFont(UIUtil.normalFont());
        button.setForeground(new Color(0xCB, 0xD5, 0xE1));
        button.setBackground(UIUtil.PRIMARY_LIGHT);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        if (cardName != null) {
            navButtons.put(cardName, button);
            button.addActionListener(e -> showCard(cardName));
        }

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!cardName.equals(currentActiveCard)) {
                    button.setBackground(UIUtil.PRIMARY_HOVER);
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!cardName.equals(currentActiveCard)) {
                    button.setBackground(UIUtil.PRIMARY_LIGHT);
                    button.setForeground(new Color(0xCB, 0xD5, 0xE1));
                }
            }
        });

        return button;
    }

    protected JButton createLogoutButton() {
        JButton button = new JButton("🚪 Logout");
        button.setFont(UIUtil.boldFont());
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0xDC, 0x26, 0x26));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        button.addActionListener(e -> {
            currentUser.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0xB9, 0x1C, 0x1C));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(0xDC, 0x26, 0x26));
            }
        });

        return button;
    }

    /** Shows the card and updates the active navigation state. */
    public void showCard(String cardName) {
        currentActiveCard = cardName;
        cardLayout.show(contentPanel, cardName);
        statusLabel.setText("Active View: " + cardName.toUpperCase());

        // Update active highlights across navigation buttons
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(cardName)) {
                btn.setBackground(UIUtil.ACCENT);
                btn.setForeground(Color.WHITE);
                btn.setFont(UIUtil.boldFont());
            } else {
                btn.setBackground(UIUtil.PRIMARY_LIGHT);
                btn.setForeground(new Color(0xCB, 0xD5, 0xE1));
                btn.setFont(UIUtil.normalFont());
            }
        }
    }

    protected void setStatus(String text) {
        statusLabel.setText(text);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
