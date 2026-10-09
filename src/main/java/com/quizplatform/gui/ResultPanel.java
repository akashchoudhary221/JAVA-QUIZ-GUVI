package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Modern result screen shown after quiz submission and during attempt review.
 * Features an analytics hero card, question breakdown table with status pills,
 * and comprehensive feedback.
 */
public class ResultPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final ParticipantDashboard dashboard;
    private final ResultService resultService = new ResultService();

    private final JLabel scoreHeroLabel = new JLabel("0/0", SwingConstants.CENTER);
    private final JLabel percentageLabel = new JLabel("0%", SwingConstants.CENTER);
    private final JLabel passFailBadge = new JLabel(" ");
    private final JProgressBar percentageBar = new JProgressBar(0, 100);

    private final JLabel statQuestions = new JLabel("-", SwingConstants.CENTER);
    private final JLabel statAttempted = new JLabel("-", SwingConstants.CENTER);
    private final JLabel statCorrect = new JLabel("-", SwingConstants.CENTER);
    private final JLabel statIncorrect = new JLabel("-", SwingConstants.CENTER);
    private final JLabel statTimeTaken = new JLabel("-", SwingConstants.CENTER);

    private final JTextArea feedbackArea = new JTextArea(3, 40);

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "#", "Question", "Your Answer", "Correct Answer", "Outcome");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    public ResultPanel(ParticipantDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(buildHeroCard(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        configureTableRenderer();
    }

    private JPanel buildHeroCard() {
        JPanel hero = new JPanel(new BorderLayout(16, 12));
        hero.setBackground(Color.WHITE);
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));

        // Top Row: Percentage + Score + Pass/Fail Pill
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(Color.WHITE);

        JPanel scoreBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        scoreBox.setBackground(Color.WHITE);

        percentageLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        percentageLabel.setForeground(UIUtil.ACCENT);

        scoreHeroLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        scoreHeroLabel.setForeground(UIUtil.TEXT);

        scoreBox.add(percentageLabel);
        scoreBox.add(scoreHeroLabel);
        scoreBox.add(passFailBadge);
        topRow.add(scoreBox, BorderLayout.WEST);

        hero.add(topRow, BorderLayout.NORTH);

        // Progress bar
        percentageBar.setStringPainted(true);
        percentageBar.setFont(UIUtil.smallBoldFont());
        percentageBar.setForeground(UIUtil.SUCCESS);
        percentageBar.setPreferredSize(new Dimension(100, 18));
        hero.add(percentageBar, BorderLayout.CENTER);

        // Metric row
        JPanel metricsGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        metricsGrid.setBackground(Color.WHITE);
        metricsGrid.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        metricsGrid.add(createMiniStat("Total Questions", statQuestions));
        metricsGrid.add(createMiniStat("Attempted", statAttempted));
        metricsGrid.add(createMiniStat("Correct", statCorrect));
        metricsGrid.add(createMiniStat("Incorrect", statIncorrect));
        metricsGrid.add(createMiniStat("Time Taken", statTimeTaken));

        hero.add(metricsGrid, BorderLayout.SOUTH);
        return hero;
    }

    private JPanel createMiniStat(String caption, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(new Color(0xF8, 0xFA, 0xFC));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel capLabel = new JLabel(caption, SwingConstants.CENTER);
        capLabel.setFont(UIUtil.smallFont());
        capLabel.setForeground(UIUtil.TEXT_MUTED);

        value.setFont(new Font("Segoe UI", Font.BOLD, 16));
        value.setForeground(UIUtil.TEXT);

        panel.add(capLabel, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(UIUtil.BACKGROUND);

        // Feedback Card
        JPanel fbCard = new JPanel(new BorderLayout(0, 6));
        fbCard.setBackground(Color.WHITE);
        fbCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel fbTitle = UIUtil.heading("Performance Evaluation & Feedback");
        fbCard.add(fbTitle, BorderLayout.NORTH);

        feedbackArea.setEditable(false);
        feedbackArea.setFont(UIUtil.normalFont());
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setBackground(new Color(0xF8, 0xFA, 0xFC));
        feedbackArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        fbCard.add(new JScrollPane(feedbackArea), BorderLayout.CENTER);

        center.add(fbCard, BorderLayout.NORTH);

        // Questions Review Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 8));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel tableTitle = UIUtil.heading("Detailed Question-by-Question Review");
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        center.add(tableCard, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        footer.setBackground(UIUtil.BACKGROUND);

        JButton backBtn = UIUtil.primaryButton("Available Quizzes");
        JButton historyBtn = UIUtil.secondaryButton("View My Attempt History");

        backBtn.addActionListener(e -> {
            dashboard.showCard("quizzes");
            dashboard.refreshStatistics();
        });

        historyBtn.addActionListener(e -> {
            dashboard.showCard("attempts");
            dashboard.refreshStatistics();
        });

        footer.add(backBtn);
        footer.add(historyBtn);
        return footer;
    }

    private void configureTableRenderer() {
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                String text = value != null ? value.toString() : "";
                JLabel badge = UIUtil.statusBadge(text);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setBackground(isSelected ? UIUtil.SELECTED_ROW : (row % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xFA, 0xFC)));
                p.add(badge);
                return p;
            }
        });
    }

    /**
     * Fills the result screen with calculated scores, answers, and feedback.
     */
    public void showResult(Result result, Quiz quiz, List<Question> questions, Map<Integer, Long> selected) {
        percentageLabel.setText(String.format("%.1f%%", result.getPercentage()));
        scoreHeroLabel.setText("Marks: " + result.getObtainedMarks() + " / " + result.getTotalMarks());
        percentageBar.setValue((int) Math.round(result.getPercentage()));

        boolean passed = result.getPercentage() >= 60.0;
        passFailBadge.setText(passed ? "  PASSED  " : "  NEEDS IMPROVEMENT  ");
        passFailBadge.setFont(UIUtil.smallBoldFont());
        passFailBadge.setOpaque(true);
        passFailBadge.setBackground(passed ? UIUtil.SUCCESS_LIGHT : UIUtil.DANGER_LIGHT);
        passFailBadge.setForeground(passed ? new Color(0x04, 0x78, 0x57) : new Color(0xB9, 0x1C, 0x1C));
        passFailBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(passed ? UIUtil.SUCCESS : UIUtil.DANGER, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        statQuestions.setText(String.valueOf(result.getTotalQuestions()));
        statAttempted.setText(String.valueOf(result.getAttemptedQuestions()));
        statCorrect.setText(String.valueOf(result.getCorrectAnswers()));
        statIncorrect.setText(String.valueOf(result.getIncorrectAnswers()));
        statTimeTaken.setText(result.getFormattedTimeTaken());

        // Question review rows
        tableModel.setRowCount(0);
        int num = 1;
        for (Question q : questions) {
            long chosen = selected.getOrDefault(q.getId(), -1L);
            String yourAnswer = "Not Answered";
            boolean isCorrect = false;
            boolean attempted = false;

            for (Option opt : q.getOptions()) {
                if (opt.getId() == chosen) {
                    yourAnswer = "(" + opt.getLetter() + ") " + opt.getOptionText();
                    isCorrect = opt.isCorrect();
                    attempted = true;
                }
            }

            Option correctOpt = q.getCorrectOption();
            String correctAnswer = correctOpt == null ? "-" : "(" + correctOpt.getLetter() + ") " + correctOpt.getOptionText();
            String outcome = !attempted ? "SKIPPED" : (isCorrect ? "CORRECT" : "WRONG");

            tableModel.addRow(new Object[]{num++, q.getText(), yourAnswer, correctAnswer, outcome});
        }

        StringBuilder fb = new StringBuilder();
        if (result.getFeedback() != null && !result.getFeedback().isBlank()) {
            fb.append("System Analysis: ").append(result.getFeedback());
        }

        try {
            List<Feedback> feedbackList = resultService.getFeedbackForResult(result.getId());
            if (!feedbackList.isEmpty()) {
                fb.append("\n\nFaculty & Creator Comments:");
                for (Feedback item : feedbackList) {
                    fb.append("\n• \"").append(item.getComment()).append("\" — by ")
                            .append(item.getCommentedByName());
                }
            }
        } catch (DatabaseException e) {
            fb.append("\n(Could not retrieve instructor feedback: ").append(e.getMessage()).append(")");
        }

        feedbackArea.setText(fb.toString());
        feedbackArea.setCaretPosition(0);
    }
}