package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizCreator;
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
 * Quiz Creator: list of own quizzes with the approval workflow actions
 * (Submit for Admin Approval / Delete / Manage Questions).
 */
public class MyQuizzesPanel extends JPanel {

    private final QuizCreatorDashboard dashboard;
    private final QuizService quizService = new QuizService();
    private final int creatorId;

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Title", "Duration (min)", "Marks", "Status", "Questions");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Quiz> quizzes = new ArrayList<>();

    public MyQuizzesPanel(QuizCreatorDashboard dashboard, QuizCreator creator) {
        this.dashboard = dashboard;
        this.creatorId = creator.getId();
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("My Quizzes"), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton submit = UIUtil.primaryButton("Submit for Approval");
        JButton manage = UIUtil.neutralButton("Manage Questions");
        JButton delete = UIUtil.dangerButton("Delete");
        JButton refresh = UIUtil.neutralButton("Refresh");

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

    public void refreshTable() {
        try {
            quizzes = quizService.getQuizzesByCreator(creatorId);
            tableModel.setRowCount(0);
            for (Quiz quiz : quizzes) {
                tableModel.addRow(new Object[]{
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getDurationMinutes(),
                        quiz.getTotalMarks(),
                        quiz.getStatus().getLabel(),
                        quizService.countQuestionsByQuiz(quiz.getId())
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load quizzes:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Quiz selectedQuiz() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a quiz first.", "No selection",
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
            // throws InvalidQuizException when the quiz has no questions
            quizService.submitForApproval(quiz.getId());
            JOptionPane.showMessageDialog(this,
                    "Quiz submitted to the admin for approval.", "Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            dashboard.refreshQuizSelector();
        } catch (InvalidQuizException | QuizNotFoundException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Submit failed",
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
        if (!UIUtil.confirm(this, "Delete quiz '" + quiz.getTitle() + "' and all its questions?")) {
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
