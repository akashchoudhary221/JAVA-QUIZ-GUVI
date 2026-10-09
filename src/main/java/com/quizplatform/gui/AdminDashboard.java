package com.quizplatform.gui;

import com.quizplatform.dao.AttemptDAO;
import com.quizplatform.dao.impl.AttemptDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Admin;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultService;
import com.quizplatform.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Admin dashboard - navigation:
 * Dashboard | Users | Quiz Approval | System Settings | Reports | Logout
 *
 * The home card shows overall platform statistics
 * (total users, total quizzes, pending, approved, total attempts).
 */
public class AdminDashboard extends DashboardFrame {

    private final UserService userService = new UserService();
    private final QuizService quizService = new QuizService();
    private final ResultService resultService = new ResultService();
    private final AttemptDAO attemptDAO = new AttemptDAOImpl();

    private final JLabel statUsers = statValue();
    private final JLabel statQuizzes = statValue();
    private final JLabel statPending = statValue();
    private final JLabel statApproved = statValue();
    private final JLabel statAttempts = statValue();

    public AdminDashboard(Admin admin) {
        super(admin, "Admin Dashboard");

        addCard("home", buildHomePanel());
        addCard("users", new UserManagementPanel());
        addCard("quizzes", new QuizManagementPanel());
        addCard("settings", new SystemSettingsPanel());
        addCard("reports", new ResultManagementPanel(admin));

        addSidebarButton(createNavButton("Dashboard", "home"));
        addSidebarButton(createNavButton("Users", "users"));
        addSidebarButton(createNavButton("Quiz Approval", "quizzes"));
        addSidebarButton(createNavButton("System Settings", "settings"));
        addSidebarButton(createNavButton("Reports", "reports"));
        addSidebarButton(createLogoutButton());

        showCard("home");
        refreshStatistics();
    }

    private JLabel statValue() {
        JLabel label = new JLabel("-");
        label.setFont(new Font("Segoe UI", Font.BOLD, 30));
        label.setForeground(UIUtil.ACCENT);
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        return label;
    }

    private JPanel statCard(String caption, JLabel value) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(18, 10, 18, 10)));
        card.add(value);
        JLabel captionLabel = new JLabel(caption, javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(UIUtil.smallFont());
        captionLabel.setForeground(UIUtil.TEXT);
        card.add(captionLabel);
        return card;
    }

    private JPanel buildHomePanel() {
        JPanel home = new JPanel(new java.awt.BorderLayout(0, 8));
        home.setBackground(UIUtil.BACKGROUND);
        home.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel heading = UIUtil.heading("Platform Statistics");
        home.add(heading, java.awt.BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        grid.add(statCard("Total Users", statUsers));
        grid.add(statCard("Total Quizzes", statQuizzes));
        grid.add(statCard("Pending Quizzes", statPending));
        grid.add(statCard("Approved Quizzes", statApproved));
        grid.add(statCard("Total Attempts", statAttempts));

        JButton refresh = UIUtil.primaryButton("Refresh Statistics");
        refresh.addActionListener(e -> refreshStatistics());
        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        south.setBackground(UIUtil.BACKGROUND);
        south.add(refresh);
        grid.add(south);

        home.add(grid, java.awt.BorderLayout.CENTER);
        return home;
    }

    /** Loads all dashboard counters from the database (JDBC through the DAOs). */
    private void refreshStatistics() {
        try {
            statUsers.setText(String.valueOf(userService.getAllUsers().size()));
            statQuizzes.setText(String.valueOf(quizService.countAllQuizzes()));
            statPending.setText(String.valueOf(
                    quizService.countQuizzesByStatus(QuizStatus.PENDING_APPROVAL.name())));
            statApproved.setText(String.valueOf(
                    quizService.countQuizzesByStatus(QuizStatus.APPROVED.name())));
            statAttempts.setText(String.valueOf(attemptDAO.countAllAttempts()));
            setStatus("Statistics refreshed");
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load statistics:\n" + e.getMessage());
        }
    }
}
