package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.Result;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Participant dashboard - navigation:
 * Dashboard | Available Quizzes | My Attempts | Performance | Profile | Logout
 *
 * Also coordinates the participant screens:
 * startQuiz() -> QuizAttemptPanel, showResult() -> ResultPanel.
 */
public class ParticipantDashboard extends DashboardFrame {

    private final Participant participant;
    private final QuizService quizService = new QuizService();
    private final ResultService resultService = new ResultService();

    private final QuizSelectionPanel quizSelectionPanel;
    private final QuizAttemptPanel attemptPanel;
    private final ResultPanel resultPanel;
    private final PerformanceHistoryPanel historyPanel;
    private final PerformancePanel performancePanel;
    private final ProfilePanel profilePanel;

    private final JLabel statQuizzes = statValue();
    private final JLabel statAttempts = statValue();
    private final JLabel statAverage = statValue();
    private final JLabel statBest = statValue();

    public ParticipantDashboard(Participant participant) {
        super(participant, "Participant Dashboard");
        this.participant = participant;

        quizSelectionPanel = new QuizSelectionPanel(this, participant);
        attemptPanel = new QuizAttemptPanel(this, participant);
        resultPanel = new ResultPanel(this);
        historyPanel = new PerformanceHistoryPanel(this, participant);
        performancePanel = new PerformancePanel(participant);
        profilePanel = new ProfilePanel(this, participant);

        addCard("home", buildHomePanel());
        addCard("quizzes", quizSelectionPanel);
        addCard("attempt", attemptPanel);
        addCard("result", resultPanel);
        addCard("attempts", historyPanel);
        addCard("performance", performancePanel);
        addCard("profile", profilePanel);

        addSidebarButton(createNavButton("Dashboard", "home"));
        addSidebarButton(createNavButton("Available Quizzes", "quizzes"));
        addSidebarButton(createNavButton("My Attempts", "attempts"));
        addSidebarButton(createNavButton("Performance", "performance"));
        addSidebarButton(createNavButton("Profile", "profile"));
        addSidebarButton(createLogoutButton());

        showCard("home");
        refreshStatistics();
    }

    // ---- navigation helpers used by the child panels ----

    /** Switches to the quiz attempt screen (the timer starts there). */
    public void startQuiz(Quiz quiz) {
        attemptPanel.prepareQuiz(quiz);
        showCard("attempt");
    }

    /** Shows the result screen after a submission (called by QuizAttemptPanel). */
    public void showResult(Result result, Quiz quiz, List<Question> questions, Map<Integer, Long> selected) {
        resultPanel.showResult(result, quiz, questions, selected);
        showCard("result");
    }

    /** Loads a stored attempt from the database and opens its detail view. */
    public void openHistoryDetail(Result result) {
        try {
            Quiz quiz = quizService.getQuizById(result.getQuizId());
            List<Question> questions = quizService.getQuestionsForQuiz(result.getQuizId());
            List<Answer> storedAnswers = resultService.getAnswersByAttempt(result.getAttemptId());
            Map<Integer, Long> selected = new HashMap<>();     // Demonstrates Java Collections: Map
            for (Answer answer : storedAnswers) {
                selected.put(answer.getQuestionId(), answer.getSelectedOptionId());
            }
            resultPanel.showResult(result, quiz, questions, selected);
            showCard("result");
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load attempt details:\n" + e.getMessage());
        }
    }

    /** Refreshes the statistics shown on the home card. */
    public void refreshStatistics() {
        try {
            int available = quizService.getApprovedQuizzes().size();
            List<Result> history = resultService.getHistory(participant.getId());
            double average = history.stream().mapToDouble(Result::getPercentage).average().orElse(0);
            double best = history.stream().mapToDouble(Result::getPercentage).max().orElse(0);

            statQuizzes.setText(String.valueOf(available));
            statAttempts.setText(String.valueOf(history.size()));
            statAverage.setText(String.format("%.1f%%", average));
            statBest.setText(String.format("%.1f%%", best));
        } catch (DatabaseException e) {
            setStatus("Statistics unavailable: " + e.getMessage());
        }
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
        home.add(UIUtil.heading("Welcome, " + participant.getName()
                + " - here is your performance overview"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        grid.add(statCard("Available Quizzes", statQuizzes));
        grid.add(statCard("My Attempts", statAttempts));
        grid.add(statCard("Average Score", statAverage));
        grid.add(statCard("Best Score", statBest));
        home.add(grid, BorderLayout.CENTER);
        return home;
    }

    public Participant getParticipant() {
        return participant;
    }
}