package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Participant: aggregated performance view
 * (total attempts, average %, best %, passed/failed using the
 *  "pass_percentage" system setting).
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
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(UIUtil.heading("My Performance - Overall Statistics"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setBackground(UIUtil.BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        grid.add(card("Total Attempts", attemptsValue));
        grid.add(card("Average Percentage", averageValue));
        grid.add(card("Best Percentage", bestValue));
        grid.add(card("Attempts with >= 60%", passedValue));
        add(grid, BorderLayout.CENTER);

        refresh();
    }

    private JLabel big() {
        JLabel label = new JLabel("-");
        label.setFont(new Font("Segoe UI", Font.BOLD, 34));
        label.setForeground(UIUtil.ACCENT);
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        return label;
    }

    private JPanel card(String caption, JLabel value) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(24, 10, 24, 10)));
        card.add(value);
        JLabel label = new JLabel(caption, javax.swing.SwingConstants.CENTER);
        label.setFont(UIUtil.normalFont());
        label.setForeground(UIUtil.TEXT);
        card.add(label);
        return card;
    }

    /** Recomputes all values from the participant's stored results (JDBC). */
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
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not load performance data:\n" + e.getMessage(),
                    "Database error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}
