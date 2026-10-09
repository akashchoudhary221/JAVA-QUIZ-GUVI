package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Quiz;
import com.quizplatform.service.QuizService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Participant screen: lists every APPROVED quiz and starts a selected quiz.
 * Only quizzes with status APPROVED/PUBLISHED are visible here
 * (QuizService.getApprovedQuizzes()).
 */
public class QuizSelectionPanel extends JPanel {

    private final ParticipantDashboard dashboard;
    private final QuizService quizService = new QuizService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Title", "Description", "Duration (min)", "Marks", "Creator");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Quiz> quizzes = new ArrayList<>();

    public QuizSelectionPanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Available Quizzes (approved by Admin)"), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton start = UIUtil.successButton("Start Selected Quiz");
        JButton refresh = UIUtil.primaryButton("Refresh");

        start.addActionListener(e -> startSelectedQuiz());
        refresh.addActionListener(e -> refreshTable());

        panel.add(start);
        panel.add(refresh);
        return panel;
    }

    public void refreshTable() {
        try {
            quizzes = quizService.getApprovedQuizzes();
            tableModel.setRowCount(0);
            for (Quiz quiz : quizzes) {
                String description = quiz.getDescription() == null ? "" : quiz.getDescription();
                if (description.length() > 60) {
                    description = description.substring(0, 60) + "...";
                }
                tableModel.addRow(new Object[]{
                        quiz.getId(),
                        quiz.getTitle(),
                        description,
                        quiz.getDurationMinutes(),
                        quiz.getTotalMarks(),
                        quiz.getCreatorName()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load quizzes:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startSelectedQuiz() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a quiz first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Quiz quiz = quizzes.get(row);
        int choice = JOptionPane.showConfirmDialog(this,
                "Start quiz '" + quiz.getTitle() + "'?\nDuration: " + quiz.getDurationMinutes()
                        + " minutes  |  Total marks: " + quiz.getTotalMarks()
                        + "\nThe timer starts immediately and the quiz is auto-submitted at 00:00.",
                "Start quiz", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dashboard.startQuiz(quiz);
        }
    }
}
