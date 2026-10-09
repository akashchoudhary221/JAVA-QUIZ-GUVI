package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Participant: performance history (all previous attempts) in a styled JTable.
 * "View Full Detail" re-opens the complete result screen with question breakdown.
 */
public class PerformanceHistoryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final ParticipantDashboard dashboard;
    private final int participantId;
    private final ResultService resultService = new ResultService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "Attempt ID", "Quiz Title", "Submission Date", "Score", "Percentage", "Accuracy", "Time Taken");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Result> results = new ArrayList<>();

    public PerformanceHistoryPanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        this.participantId = participant.getId();
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("My Quiz Attempt History"), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel("Chronological log of all evaluations with instant access to detailed question review"), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        configureTableRenderer();
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton detail = UIUtil.primaryButton("🔍 View Full Report & Review");
        JButton refresh = UIUtil.secondaryButton("Refresh History");

        detail.addActionListener(e -> viewDetail());
        refresh.addActionListener(e -> refreshTable());

        panel.add(detail);
        panel.add(refresh);
        return panel;
    }

    private void configureTableRenderer() {
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
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
            results = resultService.getHistory(participantId);
            tableModel.setRowCount(0);
            for (Result result : results) {
                tableModel.addRow(new Object[]{
                        "#" + result.getId(),
                        result.getQuizTitle(),
                        result.getSubmittedAt() == null ? "-" : DATE_FORMAT.format(result.getSubmittedAt()),
                        result.getObtainedMarks() + " / " + result.getTotalMarks(),
                        String.format("%.1f%%", result.getPercentage()),
                        result.getCorrectAnswers() + " / " + result.getTotalQuestions() + " Correct",
                        result.getFormattedTimeTaken()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load history:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewDetail() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= results.size()) {
            JOptionPane.showMessageDialog(this, "Please select an attempt from the table to inspect.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        dashboard.openHistoryDetail(results.get(row));
    }
}
