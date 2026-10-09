package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

/**
 * Result screen shown after a submission (and when reviewing history):
 * score, percentage, JProgressBar, correct/incorrect answers,
 * automatic feedback + creator/admin feedback.
 */
public class ResultPanel extends JPanel {

    private final ParticipantDashboard dashboard;
    private final ResultService resultService = new ResultService();

    private final JLabel scoreLabel = new JLabel("-");
    private final JLabel percentageLabel = new JLabel("-");
    private final JProgressBar percentageBar = new JProgressBar(0, 100);
    private final JTextArea summaryArea = new JTextArea(4, 60);
    private final JTextArea feedbackArea = new JTextArea(3, 60);

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "#", "Question", "Your Answer", "Correct Answer", "Result");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    public ResultPanel(ParticipantDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(UIUtil.BACKGROUND);

        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        scoreLabel.setForeground(UIUtil.TEXT);
        percentageLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        percentageLabel.setForeground(UIUtil.ACCENT);

        JPanel scores = new JPanel(new GridLayout(1, 2, 12, 0));
        scores.setBackground(UIUtil.BACKGROUND);
        scores.add(scoreLabel);
        scores.add(percentageLabel);
        header.add(scores, BorderLayout.CENTER);

        percentageBar.setStringPainted(true);
        percentageBar.setForeground(UIUtil.SUCCESS);
        percentageBar.setPreferredSize(new java.awt.Dimension(100, 22));
        header.add(percentageBar, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(UIUtil.BACKGROUND);

        summaryArea.setEditable(false);
        summaryArea.setFont(UIUtil.normalFont());
        feedbackArea.setEditable(false);
        feedbackArea.setFont(UIUtil.normalFont());
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);

        JPanel textCards = new JPanel(new GridLayout(1, 2, 10, 0));
        textCards.setBackground(UIUtil.BACKGROUND);
        textCards.add(wrap("Summary", new JScrollPane(summaryArea)));
        textCards.add(wrap("Feedback", new JScrollPane(feedbackArea)));

        center.add(textCards, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        return center;
    }

    private JPanel wrap(String title, JScrollPane scroll) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.add(UIUtil.heading(title), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 14, 4));
        footer.setBackground(UIUtil.BACKGROUND);
        JButton back = UIUtil.primaryButton("Back to Available Quizzes");
        JButton history = UIUtil.neutralButton("View My Attempts");
        back.addActionListener(e -> {
            dashboard.showCard("quizzes");
            dashboard.refreshStatistics();
        });
        history.addActionListener(e -> {
            dashboard.showCard("attempts");
            dashboard.refreshStatistics();
        });
        footer.add(back);
        footer.add(history);
        return footer;
    }

    /** Fills the whole result screen (summary + progress bar + question review + feedback). */
    public void showResult(Result result, Quiz quiz, List<Question> questions, Map<Integer, Long> selected) {
        java.time.format.DateTimeFormatter format =
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        scoreLabel.setText("Score: " + result.getObtainedMarks() + "/" + result.getTotalMarks());
        percentageLabel.setText(String.format("%.1f%%", result.getPercentage()));
        percentageBar.setValue((int) Math.round(result.getPercentage()));

        summaryArea.setText(String.join("\n",
                "Participant : " + result.getParticipantName(),
                "Quiz        : " + result.getQuizTitle(),
                "Date        : " + (result.getSubmittedAt() == null ? "-"
                        : format.format(result.getSubmittedAt())),
                "Total Questions     : " + result.getTotalQuestions(),
                "Attempted Questions : " + result.getAttemptedQuestions(),
                "Correct Answers     : " + result.getCorrectAnswers(),
                "Incorrect Answers   : " + result.getIncorrectAnswers(),
                "Time Taken          : " + result.getFormattedTimeTaken()));
        summaryArea.setCaretPosition(0);

        // question-wise review: your answer vs. correct answer
        tableModel.setRowCount(0);
        int number = 1;
        for (Question question : questions) {
            long chosen = selected.getOrDefault(question.getId(), -1L);
            String yourAnswer = "Not answered";
            boolean isCorrect = false;
            boolean attempted = false;
            for (Option option : question.getOptions()) {
                if (option.getId() == chosen) {
                    yourAnswer = option.getLetter() + ") " + option.getOptionText();
                    isCorrect = option.isCorrect();
                    attempted = true;
                }
            }
            Option correctOption = question.getCorrectOption();
            String correctAnswer = correctOption == null ? "-"
                    : correctOption.getLetter() + ") " + correctOption.getOptionText();
            String outcome = !attempted ? "SKIPPED" : (isCorrect ? "CORRECT" : "WRONG");
            tableModel.addRow(new Object[]{number++, question.getText(), yourAnswer,
                    correctAnswer, outcome});
        }

        StringBuilder feedback = new StringBuilder(result.getFeedback());
        try {
            List<Feedback> feedbackList = resultService.getFeedbackForResult(result.getId());
            if (!feedbackList.isEmpty()) {
                feedback.append("\n\nFeedback from your Quiz Creator:");
                for (Feedback item : feedbackList) {
                    feedback.append("\n  - ").append(item.getComment())
                            .append(" (").append(item.getCommentedByName()).append(")");
                }
            }
        } catch (DatabaseException e) {
            feedback.append("\n[Creator feedback unavailable: ").append(e.getMessage()).append("]");
        }
        feedbackArea.setText(feedback.toString());
        feedbackArea.setCaretPosition(0);
    }
}