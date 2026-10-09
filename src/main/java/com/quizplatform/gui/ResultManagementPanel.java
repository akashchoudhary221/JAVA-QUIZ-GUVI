package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.Result;
import com.quizplatform.model.Role;
import com.quizplatform.model.User;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Results screen, re-used by BOTH roles:
 *  - Admin "Reports" -> all results of the platform
 *  - Quiz Creator    -> results of the creator's own quizzes
 *
 * Allows viewing the performance report (ScoreService.generateReport)
 * and writing feedback for a participant (ResultService.addFeedback).
 */
public class ResultManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final User currentUser;
    private final ResultService resultService = new ResultService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Participant", "Quiz", "Date", "Score", "Percentage", "Correct");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Result> results = new ArrayList<>();

    public ResultManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);
        String heading = currentUser.getRole() == Role.ADMIN
                ? "Platform Reports - All Participant Results"
                : "Quiz Results - Participants of my quizzes";
        top.add(UIUtil.heading(heading), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton report = UIUtil.primaryButton("View Report");
        JButton feedback = UIUtil.successButton("Give Feedback");
        JButton refresh = UIUtil.neutralButton("Refresh");

        report.addActionListener(e -> viewReport());
        feedback.addActionListener(e -> giveFeedback());
        refresh.addActionListener(e -> refreshTable());

        panel.add(report);
        panel.add(feedback);
        panel.add(refresh);
        return panel;
    }

    public void refreshTable() {
        try {
            results = currentUser.getRole() == Role.ADMIN
                    ? resultService.getAllResults()
                    : resultService.getResultsForCreator(currentUser.getId());
            tableModel.setRowCount(0);
            for (Result result : results) {
                tableModel.addRow(new Object[]{
                        result.getId(),
                        result.getParticipantName(),
                        result.getQuizTitle(),
                        result.getSubmittedAt() == null ? "-" : DATE_FORMAT.format(result.getSubmittedAt()),
                        result.getObtainedMarks() + "/" + result.getTotalMarks(),
                        result.getPercentage() + "%",
                        result.getCorrectAnswers() + "/" + result.getTotalQuestions()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load results:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Result selectedResult() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a result first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return results.get(row);
    }

    /** Detailed performance report in a scrollable dialog. */
    private void viewReport() {
        Result result = selectedResult();
        if (result == null) {
            return;
        }
        StringBuilder text = new StringBuilder(resultService.generateReport(result));
        try {
            List<Feedback> feedbackList = resultService.getFeedbackForResult(result.getId());
            if (!feedbackList.isEmpty()) {
                text.append("\nCREATOR / ADMIN FEEDBACK:\n");
                for (Feedback feedback : feedbackList) {
                    text.append("  - ").append(feedback.getComment())
                            .append("  (by ").append(feedback.getCommentedByName()).append(")\n");
                }
            }
        } catch (DatabaseException e) {
            text.append("\n[Could not load feedback: ").append(e.getMessage()).append("]\n");
        }

        JTextArea area = new JTextArea(text.toString());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(640, 480));
        JOptionPane.showMessageDialog(this, scroll, "Performance Report",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** Writes a comment that the participant will see on the result screen. */
    private void giveFeedback() {
        Result result = selectedResult();
        if (result == null) {
            return;
        }
        JTextArea comment = new JTextArea(5, 40);
        comment.setLineWrap(true);
        JPanel dialog = new JPanel(new BorderLayout(0, 8));
        dialog.add(new JLabel("Result #" + result.getId() + " - " + result.getQuizTitle()
                + " - Score: " + result.getPercentage() + "%"), BorderLayout.NORTH);
        dialog.add(new JScrollPane(comment), BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(this, dialog,
                "Feedback for " + result.getParticipantName(), JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String text = comment.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Feedback text must not be empty.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Feedback feedback = new Feedback();
            feedback.setResultId(result.getId());
            feedback.setQuizId(result.getQuizId());
            feedback.setParticipantId(result.getParticipantId());
            feedback.setCommentedBy(currentUser.getId());
            feedback.setComment(text);
            resultService.addFeedback(feedback);
            JOptionPane.showMessageDialog(this, "Feedback saved. The participant can now see it.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}