package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.service.QuizService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Admin screen: view submitted quizzes, inspect their questions and
 * APPROVE or REJECT them (quiz approval workflow).
 */
public class QuizManagementPanel extends JPanel {

    private final QuizService quizService = new QuizService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Title", "Creator", "Duration (min)", "Marks", "Status", "Questions");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private java.util.List<Quiz> quizzes = new java.util.ArrayList<>();

    public QuizManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Submitted Quizzes - Approval Workflow"), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton view = UIUtil.neutralButton("View Details");
        JButton approve = UIUtil.successButton("Approve");
        JButton reject = UIUtil.dangerButton("Reject");
        JButton refresh = UIUtil.primaryButton("Refresh");

        view.addActionListener(e -> viewDetails());
        approve.addActionListener(e -> changeStatus(true));
        reject.addActionListener(e -> changeStatus(false));
        refresh.addActionListener(e -> refreshTable());

        panel.add(view);
        panel.add(approve);
        panel.add(reject);
        panel.add(refresh);
        return panel;
    }

    private void refreshTable() {
        try {
            quizzes = quizService.getAllQuizzes();
            tableModel.setRowCount(0);
            for (Quiz quiz : quizzes) {
                tableModel.addRow(new Object[]{
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCreatorName(),
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
            JOptionPane.showMessageDialog(this, "Select a quiz in the table first.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return quizzes.get(row);
    }

    /** Shows the complete quiz with all questions, options and correct answers. */
    private void viewDetails() {
        Quiz quiz = selectedQuiz();
        if (quiz == null) {
            return;
        }
        try {
            Quiz full = quizService.getQuizWithQuestions(quiz.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("QUIZ DETAILS\n");
            sb.append("============\n");
            sb.append("Title       : ").append(full.getTitle()).append('\n');
            sb.append("Description : ").append(full.getDescription()).append('\n');
            sb.append("Creator     : ").append(full.getCreatorName()).append('\n');
            sb.append("Duration    : ").append(full.getDurationMinutes()).append(" minutes\n");
            sb.append("Total marks : ").append(full.getTotalMarks()).append('\n');
            sb.append("Status      : ").append(full.getStatus().getLabel()).append("\n\n");
            sb.append("QUESTIONS (").append(full.getQuestions().size()).append(")\n");
            int number = 1;
            for (Question question : full.getQuestions()) {
                sb.append(number++).append(". ").append(question.getText())
                        .append("  [").append(question.getMarks()).append(" mark(s)]\n");
                for (Option option : question.getOptions()) {
                    sb.append("    ").append(option.getLetter()).append(") ")
                            .append(option.getOptionText())
                            .append(option.isCorrect() ? "   <= CORRECT" : "").append('\n');
                }
                if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
                    sb.append("    Explanation: ").append(question.getExplanation()).append('\n');
                }
                sb.append('\n');
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            area.setEditable(false);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new java.awt.Dimension(640, 460));
            JOptionPane.showMessageDialog(this, scroll, "Quiz #" + full.getId() + " details",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (QuizNotFoundException | DatabaseException e) {
            // Demonstrates custom exception handling
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Admin approve / reject action. */
    private void changeStatus(boolean approve) {
        Quiz quiz = selectedQuiz();
        if (quiz == null) {
            return;
        }
        try {
            if (approve) {
                quizService.approveQuiz(quiz.getId());
                JOptionPane.showMessageDialog(this,
                        "Quiz approved - it is now visible to participants.", "Approved",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Reject quiz '" + quiz.getTitle() + "'?", "Confirm rejection",
                        JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                quizService.rejectQuiz(quiz.getId());
                JOptionPane.showMessageDialog(this, "Quiz rejected.", "Rejected",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            refreshTable();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Action failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}