package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Quiz Creator dashboard - navigation:
 * Dashboard | Create Quiz | My Quizzes | Questions | Quiz Results |
 * Participant Feedback | Logout
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
        super(creator, "Quiz Creator Dashboard");

        createPanel = new CreateQuizPanel(this, creator);
        myQuizzesPanel = new MyQuizzesPanel(this, creator);
        questionPanel = new QuestionManagementPanel(this, creator);

        addCard("home", buildHomePanel());
        addCard("create", createPanel);
        addCard("myquizzes", myQuizzesPanel);
        addCard("questions", questionPanel);
        addCard("results", new ResultManagementPanel(creator));
        addCard("feedback", new FeedbackPanel(creator));

        addSidebarButton(createNavButton("Dashboard", "home"));
        addSidebarButton(createNavButton("Create Quiz", "create"));
        addSidebarButton(createNavButton("My Quizzes", "myquizzes"));
        addSidebarButton(createNavButton("Questions", "questions"));
        addSidebarButton(createNavButton("Quiz Results", "results"));
        addSidebarButton(createNavButton("Participant Feedback", "feedback"));
        addSidebarButton(createLogoutButton());

        showCard("home");
        refreshStatistics();
    }

    /** Called by CreateQuizPanel / MyQuizzesPanel to jump to the question editor. */
    public void openQuestionsFor(int quizId) {
        questionPanel.loadQuiz(quizId);
        showCard("questions");
    }

    /** Keeps the quiz combo of the question panel up to date. */
    public void refreshQuizSelector() {
        questionPanel.refreshQuizSelector();
        myQuizzesPanel.refreshTable();
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
        JPanel home = new JPanel(new BorderLayout(0, 8));
        home.setBackground(UIUtil.BACKGROUND);
        home.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        home.add(UIUtil.heading("My Quiz Statistics"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        grid.add(statCard("My Quizzes", statTotal));
        grid.add(statCard("Pending Approval", statPending));
        grid.add(statCard("Approved Quizzes", statApproved));
        grid.add(statCard("Results Received", statResults));
        home.add(grid, BorderLayout.CENTER);
        return home;
    }

    private void refreshStatistics() {
        try {
            QuizCreator creator = (QuizCreator) currentUser;
            statTotal.setText(String.valueOf(quizService.getQuizzesByCreator(creator.getId()).size()));
            statPending.setText(String.valueOf(
                    quizService.getQuizzesByCreator(creator.getId()).stream()
                            .filter(q -> q.getStatus() == QuizStatus.PENDING_APPROVAL).count()));
            statApproved.setText(String.valueOf(
                    quizService.getQuizzesByCreator(creator.getId()).stream()
                            .filter(q -> q.getStatus() == QuizStatus.APPROVED
                                    || q.getStatus() == QuizStatus.PUBLISHED).count()));
            statResults.setText(String.valueOf(
                    resultService.getResultsForCreator(creator.getId()).size()));
            setStatus("Statistics refreshed");
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load statistics:\n" + e.getMessage());
        }
    }
}
