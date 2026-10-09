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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Admin dashboard.
 * Navigation: Dashboard | Users | Quiz Approval | System Settings | Reports | Logout
 *
 * Home screen displays platform KPI metric cards:
 * total users, total quizzes, pending approval, approved, and total attempts.
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
        super(admin, "Admin Administration Suite");

        addCard("home", buildHomePanel());
        addCard("users", new UserManagementPanel());
        addCard("quizzes", new QuizManagementPanel());
        addCard("settings", new SystemSettingsPanel());
        addCard("reports", new ResultManagementPanel(admin));

        addSidebarButton(createNavButton("📊 Dashboard", "home"));
        addSidebarButton(createNavButton("👥 User Management", "users"));
        addSidebarButton(createNavButton("📝 Quiz Approval", "quizzes"));
        addSidebarButton(createNavButton("⚙️ System Settings", "settings"));
        addSidebarButton(createNavButton("📈 Platform Reports", "reports"));

        showCard("home");
        refreshStatistics();
    }

    private JLabel statValue() {
        JLabel label = new JLabel("-");
        label.setFont(new Font("Segoe UI", Font.BOLD, 30));
        label.setForeground(UIUtil.TEXT);
        return label;
    }

    private JPanel buildHomePanel() {
        JPanel home = new JPanel(new BorderLayout(0, 16));
        home.setBackground(UIUtil.BACKGROUND);
        home.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        // Top Banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Color.WHITE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));

        JPanel bannerText = new JPanel();
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        bannerText.setBackground(Color.WHITE);

        JLabel welcome = new JLabel("Platform Administration Overview");
        welcome.setFont(UIUtil.titleFont());
        welcome.setForeground(UIUtil.TEXT);

        JLabel sub = new JLabel("Real-time metrics, user management, and quiz approval workflows.");
        sub.setFont(UIUtil.normalFont());
        sub.setForeground(UIUtil.TEXT_MUTED);

        bannerText.add(welcome);
        bannerText.add(Box.createVerticalStrut(4));
        bannerText.add(sub);
        banner.add(bannerText, BorderLayout.WEST);

        JButton refreshBtn = UIUtil.primaryButton("🔄 Refresh Stats");
        refreshBtn.addActionListener(e -> refreshStatistics());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        btnWrap.setBackground(Color.WHITE);
        btnWrap.add(refreshBtn);
        banner.add(btnWrap, BorderLayout.EAST);

        home.add(banner, BorderLayout.NORTH);

        // Grid of Modern KPI Metric Cards
        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);

        grid.add(UIUtil.metricCard("TOTAL USERS", statUsers, "Faculty & student accounts", UIUtil.ACCENT));
        grid.add(UIUtil.metricCard("TOTAL QUIZZES", statQuizzes, "All platform quizzes", UIUtil.PRIMARY_LIGHT));
        grid.add(UIUtil.metricCard("PENDING APPROVAL", statPending, "Quizzes awaiting review", UIUtil.WARNING));
        grid.add(UIUtil.metricCard("APPROVED QUIZZES", statApproved, "Active and ready to attempt", UIUtil.SUCCESS));
        grid.add(UIUtil.metricCard("TOTAL ATTEMPTS", statAttempts, "Completed participant submissions", UIUtil.ACCENT));

        // Quick tips card
        JPanel tipsCard = new JPanel(new BorderLayout(0, 6));
        tipsCard.setBackground(Color.WHITE);
        tipsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel tipsTitle = UIUtil.heading("Administrative Quick Actions");
        tipsCard.add(tipsTitle, BorderLayout.NORTH);

        JLabel tipsText = new JLabel("<html><body style='font-family: Segoe UI; color: #64748B; font-size: 11pt; line-height: 1.4;'>"
                + "• Navigate to <b>Quiz Approval</b> to inspect and approve pending quizzes.<br>"
                + "• Go to <b>User Management</b> to assign roles or create demo users.<br>"
                + "• Check <b>Platform Reports</b> for detailed student results.</body></html>");
        tipsCard.add(tipsText, BorderLayout.CENTER);
        grid.add(tipsCard);

        home.add(grid, BorderLayout.CENTER);
        return home;
    }

    /** Loads all dashboard counters from the database via DAOs. */
    public void refreshStatistics() {
        try {
            statUsers.setText(String.valueOf(userService.getAllUsers().size()));
            statQuizzes.setText(String.valueOf(quizService.countAllQuizzes()));
            statPending.setText(String.valueOf(quizService.countQuizzesByStatus(QuizStatus.PENDING_APPROVAL.name())));
            statApproved.setText(String.valueOf(quizService.countQuizzesByStatus(QuizStatus.APPROVED.name())));
            statAttempts.setText(String.valueOf(attemptDAO.countAllAttempts()));
            setStatus("Platform metrics updated successfully");
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load statistics:\n" + e.getMessage());
        }
    }
}
