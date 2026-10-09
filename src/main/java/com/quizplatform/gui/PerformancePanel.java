package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Participant: aggregated performance analytics view
 * (total attempts, average %, best %, passed/failed evaluations).
 */
public class PerformancePanel extends JPanel {

    private final int participantId;
    private final ResultService resultService = new ResultService();

    private final JLabel attemptsValue = big();
    private final JLabel averageValue = big();
    private final JLabel bestValue = big();
    private final JLabel passedValue = big();

    public PerformancePanel(Participant participant) {
        this.participantId = participant.getId();
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setBackground(UIUtil.BACKGROUND);
        titles.add(UIUtil.heading("Academic Performance Summary"), BorderLayout.NORTH);
        titles.add(UIUtil.mutedLabel("Aggregated assessment metrics calculated across all your submitted evaluations"), BorderLayout.SOUTH);
        top.add(titles, BorderLayout.WEST);

        JButton refreshBtn = UIUtil.secondaryButton("🔄 Refresh Metrics");
        refreshBtn.addActionListener(e -> refresh());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrap.setBackground(UIUtil.BACKGROUND);
        btnWrap.add(refreshBtn);
        top.add(btnWrap, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);

        grid.add(UIUtil.metricCard("TOTAL QUIZ ATTEMPTS", attemptsValue, "Completed quizzes submitted", UIUtil.PRIMARY_LIGHT));
        grid.add(UIUtil.metricCard("AVERAGE PERCENTAGE", averageValue, "Mean percentage across attempts", UIUtil.ACCENT));
        grid.add(UIUtil.metricCard("HIGHEST SCORE (BEST)", bestValue, "Peak score achieved so far", UIUtil.SUCCESS));
        grid.add(UIUtil.metricCard("PASSED ATTEMPTS (>= 60%)", passedValue, "Quizzes meeting graduation threshold", UIUtil.WARNING));

        add(grid, BorderLayout.CENTER);
        refresh();
    }

    private JLabel big() {
        JLabel label = new JLabel("-", SwingConstants.LEFT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 32));
        label.setForeground(UIUtil.TEXT);
        return label;
    }

    /** Recomputes values from the participant's stored results in MySQL. */
    public void refresh() {
        try {
            List<Result> results = resultService.getHistory(participantId);
            double average = results.stream().mapToDouble(Result::getPercentage).average().orElse(0);
            double best = results.stream().mapToDouble(Result::getPercentage).max().orElse(0);
            long passed = results.stream().filter(r -> r.getPercentage() >= 60).count();

            attemptsValue.setText(String.valueOf(results.size()));
            averageValue.setText(String.format("%.1f%%", average));
            bestValue.setText(String.format("%.1f%%", best));
            passedValue.setText(String.valueOf(passed));
        } catch (DatabaseException e) {
            attemptsValue.setText("!");
            UIUtil.error(this, "Could not load performance data:\n" + e.getMessage());
        }
    }
}
