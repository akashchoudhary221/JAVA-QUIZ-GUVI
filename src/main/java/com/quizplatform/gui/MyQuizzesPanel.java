package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.service.QuizService;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz Creator: list of own authored quizzes with approval workflow actions
 * (Submit for Admin Approval / Manage Questions / Delete).
 */
public class MyQuizzesPanel extends JPanel {

    private final QuizCreatorDashboard dashboard;
    private final QuizService quizService = new QuizService();
    private final int creatorId;

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Quiz Title", "Duration", "Total Marks", "Current Status", "Questions");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Quiz> quizzes = new ArrayList<>();

    public MyQuizzesPanel(QuizCreatorDashboard dashboard, QuizCreator creator) {
        this.dashboard = dashboard;
        this.creatorId = creator.getId();
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("My Authored Quizzes"), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel("Author questions, save drafts, and submit finalized quizzes for admin approval"), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        configureTableRenderer();
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton submit = UIUtil.primaryButton("🚀 Submit for Approval");
        JButton manage = UIUtil.secondaryButton("❓ Manage Questions");
        JButton delete = UIUtil.dangerButton("🗑 Delete Quiz");
        JButton refresh = UIUtil.secondaryButton("Refresh");

        submit.addActionListener(e -> submitForApproval());
        manage.addActionListener(e -> manageQuestions());
        delete.addActionListener(e -> deleteQuiz());
        refresh.addActionListener(e -> refreshTable());

        panel.add(submit);
        panel.add(manage);
        panel.add(delete);
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
                JLabel badge = UIUtil.statusBadge(text);
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
            quizzes = quizService.getQuizzesByCreator(creatorId);
            tableModel.setRowCount(0);
            for (Quiz quiz : quizzes) {
                int qCount = 0;
                try {
                    qCount = quizService.countQuestionsByQuiz(quiz.getId());
                } catch (DatabaseException ignored) {
                }
                tableModel.addRow(new Object[]{
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getDurationMinutes() + " mins",
                        quiz.getTotalMarks() + " pts",
                        quiz.getStatus().name(),
                        qCount + " question(s)"
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load quizzes:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Quiz selectedQuiz() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= quizzes.size()) {
            JOptionPane.showMessageDialog(this, "Select a quiz first from the table.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return quizzes.get(row);
    }

    private void submitForApproval() {
        Quiz quiz = selectedQuiz();
        if (quiz == null) {
            return;
        }
        try {
            quizService.submitForApproval(quiz.getId());
            JOptionPane.showMessageDialog(this,
                    "Quiz '" + quiz.getTitle() + "' submitted to the Administrator for approval.\n"
                            + "Once approved, students can take this quiz.", "Submitted Successfully",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            dashboard.refreshQuizSelector();
        } catch (InvalidQuizException | QuizNotFoundException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Submission Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void manageQuestions() {
        Quiz quiz = selectedQuiz();
        if (quiz != null) {
            dashboard.openQuestionsFor(quiz.getId());
        }
    }

    private void deleteQuiz() {
        Quiz quiz = selectedQuiz();
        if (quiz == null) {
            return;
        }
        if (!UIUtil.confirm(this, "Delete quiz '" + quiz.getTitle() + "' and all its questions?\nThis action cannot be undone.")) {
            return;
        }
        try {
            quizService.deleteQuiz(quiz.getId());
            JOptionPane.showMessageDialog(this, "Quiz deleted.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            dashboard.refreshQuizSelector();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Delete failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
