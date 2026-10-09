package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultService;

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
 * Quiz Creator dashboard workspace.
 * Navigation:
 * Dashboard | Create Quiz | My Quizzes | Questions | Quiz Results | Participant Feedback
 */
public class QuizCreatorDashboard extends DashboardFrame {

    private final QuizService quizService = new QuizService();
    private final ResultService resultService = new ResultService();

    private final JLabel statTotal = statValue();
    private final JLabel statPending = statValue();
    private final JLabel statApproved = statValue();
    private final JLabel statResults = statValue();

    private final CreateQuizPanel createPanel;
    private final MyQuizzesPanel myQuizzesPanel;
    private final QuestionManagementPanel questionPanel;

    public QuizCreatorDashboard(QuizCreator creator) {
        super(creator, "Quiz Creator Studio");

        createPanel = new CreateQuizPanel(this, creator);
        myQuizzesPanel = new MyQuizzesPanel(this, creator);
        questionPanel = new QuestionManagementPanel(this, creator);

        addCard("home", buildHomePanel());
        addCard("create", createPanel);
        addCard("myquizzes", myQuizzesPanel);
        addCard("questions", questionPanel);
        addCard("results", new ResultManagementPanel(creator));
        addCard("feedback", new FeedbackPanel(creator));

        addSidebarButton(createNavButton("📊 Dashboard", "home"));
        addSidebarButton(createNavButton("➕ Create Quiz", "create"));
        addSidebarButton(createNavButton("📚 My Quizzes", "myquizzes"));
        addSidebarButton(createNavButton("❓ Manage Questions", "questions"));
        addSidebarButton(createNavButton("📈 Student Results", "results"));
        addSidebarButton(createNavButton("💬 Feedback Review", "feedback"));

        showCard("home");
        refreshStatistics();
    }

    public void openQuestionsFor(int quizId) {
        questionPanel.loadQuiz(quizId);
        showCard("questions");
    }

    public void refreshQuizSelector() {
        questionPanel.refreshQuizSelector();
        myQuizzesPanel.refreshTable();
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

        JLabel welcome = new JLabel("Faculty Authoring Studio");
        welcome.setFont(UIUtil.titleFont());
        welcome.setForeground(UIUtil.TEXT);

        JLabel sub = new JLabel("Create quizzes, author question banks, submit for admin approval, and evaluate participants.");
        sub.setFont(UIUtil.normalFont());
        sub.setForeground(UIUtil.TEXT_MUTED);

        bannerText.add(welcome);
        bannerText.add(Box.createVerticalStrut(4));
        bannerText.add(sub);
        banner.add(bannerText, BorderLayout.WEST);

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        btnWrap.setBackground(Color.WHITE);

        JButton createBtn = UIUtil.primaryButton("+ Create New Quiz");
        createBtn.addActionListener(e -> showCard("create"));
        btnWrap.add(createBtn);

        JButton refreshBtn = UIUtil.secondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshStatistics());
        btnWrap.add(refreshBtn);

        banner.add(btnWrap, BorderLayout.EAST);
        home.add(banner, BorderLayout.NORTH);

        // 4 Metric cards
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);

        grid.add(UIUtil.metricCard("MY QUIZZES", statTotal, "Created by you", UIUtil.PRIMARY_LIGHT));
        grid.add(UIUtil.metricCard("PENDING APPROVAL", statPending, "Awaiting admin review", UIUtil.WARNING));
        grid.add(UIUtil.metricCard("APPROVED QUIZZES", statApproved, "Active and live for students", UIUtil.SUCCESS));
        grid.add(UIUtil.metricCard("RESULTS RECEIVED", statResults, "Completed student attempts", UIUtil.ACCENT));

        home.add(grid, BorderLayout.CENTER);
        return home;
    }

    private void refreshStatistics() {
        try {
            QuizCreator creator = (QuizCreator) currentUser;
            var creatorQuizzes = quizService.getQuizzesByCreator(creator.getId());
            statTotal.setText(String.valueOf(creatorQuizzes.size()));
            statPending.setText(String.valueOf(
                    creatorQuizzes.stream().filter(q -> q.getStatus() == QuizStatus.PENDING_APPROVAL).count()));
            statApproved.setText(String.valueOf(
                    creatorQuizzes.stream().filter(q -> q.getStatus() == QuizStatus.APPROVED
                            || q.getStatus() == QuizStatus.PUBLISHED).count()));
            statResults.setText(String.valueOf(
                    resultService.getResultsForCreator(creator.getId()).size()));
            setStatus("Creator metrics updated");
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load statistics:\n" + e.getMessage());
        }
    }
}
