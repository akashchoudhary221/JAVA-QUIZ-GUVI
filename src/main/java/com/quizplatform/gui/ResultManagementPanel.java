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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Results and reports workspace, shared by Admin and Quiz Creator:
 *  - Admin: All results across the entire platform
 *  - Quiz Creator: Submissions on quizzes authored by the creator
 */
public class ResultManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final User currentUser;
    private final ResultService resultService = new ResultService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Participant Name", "Quiz Title", "Submission Date", "Score", "Percentage", "Accuracy");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Result> results = new ArrayList<>();

    public ResultManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        String heading = currentUser.getRole() == Role.ADMIN
                ? "Platform Assessment Reports (All Participants)"
                : "Quiz Submissions & Participant Performance";
        String subtitle = currentUser.getRole() == Role.ADMIN
                ? "Institution-wide assessment records with detailed reports and feedback tools"
                : "Participant scores on quizzes you authored with the ability to write direct instructor feedback";
        top.add(UIUtil.heading(heading), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel(subtitle), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        configureTableRenderer();
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton report = UIUtil.primaryButton("📄 View Assessment Report");
        JButton feedback = UIUtil.successButton("💬 Add Instructor Feedback");
        JButton refresh = UIUtil.secondaryButton("Refresh");

        report.addActionListener(e -> viewReport());
        feedback.addActionListener(e -> giveFeedback());
        refresh.addActionListener(e -> refreshTable());

        panel.add(report);
        panel.add(feedback);
        panel.add(refresh);
        return panel;
    }

    private void configureTableRenderer() {
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                String text = value != null ? value.toString() : "";
                double pct = 0;
                try {
                    pct = Double.parseDouble(text.replace("%", "").trim());
                } catch (Exception ignored) {
                }
                JLabel badge = UIUtil.badge(text, pct >= 60 ? UIUtil.SUCCESS_LIGHT : UIUtil.DANGER_LIGHT,
                        pct >= 60 ? new Color(0x04, 0x78, 0x57) : new Color(0xB9, 0x1C, 0x1C));
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setBackground(isSelected ? UIUtil.SELECTED_ROW : (row % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xFA, 0xFC)));
                p.add(badge);
                return p;
            }
        });
    }

    public void refreshTable() {
        try {
            results = currentUser.getRole() == Role.ADMIN
                    ? resultService.getAllResults()
                    : resultService.getResultsForCreator(currentUser.getId());
            tableModel.setRowCount(0);
            for (Result result : results) {
                tableModel.addRow(new Object[]{
                        "#" + result.getId(),
                        result.getParticipantName(),
                        result.getQuizTitle(),
                        result.getSubmittedAt() == null ? "-" : DATE_FORMAT.format(result.getSubmittedAt()),
                        result.getObtainedMarks() + " / " + result.getTotalMarks(),
                        String.format("%.1f%%", result.getPercentage()),
                        result.getCorrectAnswers() + " / " + result.getTotalQuestions()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load results:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Result selectedResult() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= results.size()) {
            JOptionPane.showMessageDialog(this, "Select a participant result in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return results.get(row);
    }

    private void viewReport() {
        Result result = selectedResult();
        if (result == null) {
            return;
        }
        StringBuilder text = new StringBuilder(resultService.generateReport(result));
        try {
            List<Feedback> feedbackList = resultService.getFeedbackForResult(result.getId());
            if (!feedbackList.isEmpty()) {
                text.append("\n=====================================================\n");
                text.append("FACULTY & INSTRUCTOR FEEDBACK LOG:\n");
                for (Feedback fb : feedbackList) {
                    text.append(" • \"").append(fb.getComment()).append("\"\n")
                            .append("   — by ").append(fb.getCommentedByName())
                            .append(" on ").append(fb.getCreatedAt() != null ? DATE_FORMAT.format(fb.getCreatedAt()) : "-")
                            .append("\n\n");
                }
            }
        } catch (DatabaseException e) {
            text.append("\n(Feedback log unavailable: ").append(e.getMessage()).append(")\n");
        }

        JTextArea area = new JTextArea(text.toString());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(680, 500));
        JOptionPane.showMessageDialog(this, scroll,
                "Assessment Report - " + result.getParticipantName() + " (" + result.getQuizTitle() + ")",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void giveFeedback() {
        Result result = selectedResult();
        if (result == null) {
            return;
        }
        String comment = JOptionPane.showInputDialog(this,
                "Enter constructive feedback for participant '" + result.getParticipantName() + "':",
                "Faculty Feedback", JOptionPane.PLAIN_MESSAGE);
        if (comment == null || comment.trim().isEmpty()) {
            return;
        }
        try {
            resultService.addFeedback(result.getId(), result.getQuizId(),
                    result.getParticipantId(), currentUser.getId(), comment.trim());
            JOptionPane.showMessageDialog(this, "Feedback submitted successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could Not Add Feedback",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}