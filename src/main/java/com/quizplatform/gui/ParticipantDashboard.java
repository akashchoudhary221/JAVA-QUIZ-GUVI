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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern participant dashboard.
 * Navigation:
 * Dashboard | Available Quizzes | My Attempts | Performance | Profile
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
        super(participant, "Participant Learning Portal");
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

        addSidebarButton(createNavButton("📊 Dashboard", "home"));
        addSidebarButton(createNavButton("📝 Available Quizzes", "quizzes"));
        addSidebarButton(createNavButton("📜 My Attempts", "attempts"));
        addSidebarButton(createNavButton("📈 Performance Analysis", "performance"));
        addSidebarButton(createNavButton("👤 Student Profile", "profile"));

        showCard("home");
        refreshStatistics();
    }

    public void startQuiz(Quiz quiz) {
        attemptPanel.prepareQuiz(quiz);
        showCard("attempt");
    }

    public void showResult(Result result, Quiz quiz, List<Question> questions, Map<Integer, Long> selected) {
        resultPanel.showResult(result, quiz, questions, selected);
        showCard("result");
    }

    public void openHistoryDetail(Result result) {
        try {
            Quiz quiz = quizService.getQuizById(result.getQuizId());
            List<Question> questions = quizService.getQuestionsForQuiz(result.getQuizId());
            List<Answer> storedAnswers = resultService.getAnswersByAttempt(result.getAttemptId());
            Map<Integer, Long> selected = new HashMap<>();
            for (Answer answer : storedAnswers) {
                selected.put(answer.getQuestionId(), answer.getSelectedOptionId());
            }
            resultPanel.showResult(result, quiz, questions, selected);
            showCard("result");
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load attempt details:\n" + e.getMessage());
        }
    }

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
            setStatus("Ready");
        } catch (DatabaseException e) {
            setStatus("Statistics unavailable: " + e.getMessage());
        }
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

        // Top Welcome Banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Color.WHITE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));

        JPanel bannerText = new JPanel();
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        bannerText.setBackground(Color.WHITE);

        JLabel welcome = new JLabel("Welcome back, " + participant.getName() + " 👋");
        welcome.setFont(UIUtil.titleFont());
        welcome.setForeground(UIUtil.TEXT);

        JLabel sub = new JLabel("Test your Java knowledge with timed evaluations, instant scoring, and detailed review.");
        sub.setFont(UIUtil.normalFont());
        sub.setForeground(UIUtil.TEXT_MUTED);

        bannerText.add(welcome);
        bannerText.add(Box.createVerticalStrut(4));
        bannerText.add(sub);
        banner.add(bannerText, BorderLayout.WEST);

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        btnWrap.setBackground(Color.WHITE);

        JButton takeQuizBtn = UIUtil.primaryButton("▶ Take a Quiz Now");
        takeQuizBtn.addActionListener(e -> showCard("quizzes"));
        btnWrap.add(takeQuizBtn);

        JButton refreshBtn = UIUtil.secondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshStatistics());
        btnWrap.add(refreshBtn);

        banner.add(btnWrap, BorderLayout.EAST);
        home.add(banner, BorderLayout.NORTH);

        // Grid of 4 Metric cards
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);

        grid.add(UIUtil.metricCard("AVAILABLE QUIZZES", statQuizzes, "Approved for testing", UIUtil.PRIMARY_LIGHT));
        grid.add(UIUtil.metricCard("MY ATTEMPTS", statAttempts, "Completed evaluations", UIUtil.ACCENT));
        grid.add(UIUtil.metricCard("AVERAGE SCORE", statAverage, "Overall platform average", UIUtil.WARNING));
        grid.add(UIUtil.metricCard("BEST SCORE", statBest, "Highest achieved score", UIUtil.SUCCESS));

        home.add(grid, BorderLayout.CENTER);
        return home;
    }

    public Participant getParticipant() {
        return participant;
    }
}