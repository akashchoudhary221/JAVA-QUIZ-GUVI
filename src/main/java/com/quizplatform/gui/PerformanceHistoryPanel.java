package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Participant: performance history (all previous attempts) in a JTable.
 * "View Detail" re-opens the full result screen for the selected attempt.
 */
public class PerformanceHistoryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final ParticipantDashboard dashboard;
    private final int participantId;
    private final ResultService resultService = new ResultService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Quiz", "Date", "Score", "Percentage", "Correct", "Time Taken");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Result> results = new ArrayList<>();

    public PerformanceHistoryPanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        this.participantId = participant.getId();
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("My Attempts - Performance History"), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);
        JButton detail = UIUtil.successButton("View Full Detail");
        JButton refresh = UIUtil.primaryButton("Refresh");
        detail.addActionListener(e -> viewDetail());
        refresh.addActionListener(e -> refreshTable());
        panel.add(detail);
        panel.add(refresh);
        return panel;
    }

    private void refreshTable() {
        try {
            results = resultService.getHistory(participantId);
            tableModel.setRowCount(0);
            for (Result result : results) {
                tableModel.addRow(new Object[]{
                        result.getId(),
                        result.getQuizTitle(),
                        result.getSubmittedAt() == null ? "-" : DATE_FORMAT.format(result.getSubmittedAt()),
                        result.getObtainedMarks() + "/" + result.getTotalMarks(),
                        result.getPercentage() + "%",
                        result.getCorrectAnswers() + "/" + result.getTotalQuestions(),
                        result.getFormattedTimeTaken()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load history:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewDetail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an attempt first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        dashboard.openHistoryDetail(results.get(row));
    }
}
