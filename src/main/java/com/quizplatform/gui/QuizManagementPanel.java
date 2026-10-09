package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.service.QuizService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen: Quiz approval workflow.
 * Inspect submitted quizzes, review questions and options, and approve or reject.
 */
public class QuizManagementPanel extends JPanel {

    private final QuizService quizService = new QuizService();

    private final JComboBox<String> statusFilterCombo = new JComboBox<>(new String[]{
            "ALL STATUSES", "PENDING_APPROVAL", "APPROVED", "REJECTED", "DRAFT"
    });

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Title", "Faculty Creator", "Duration", "Marks", "Approval Status", "Questions");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Quiz> allQuizzes = new ArrayList<>();
    private List<Quiz> filteredQuizzes = new ArrayList<>();

    public QuizManagementPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        statusFilterCombo.addActionListener(e -> applyFilter());
        configureTableRenderer();
        refreshTable();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(UIUtil.BACKGROUND);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setBackground(UIUtil.BACKGROUND);
        titles.add(UIUtil.heading("Submitted Quizzes - Approval Workflow"), BorderLayout.NORTH);
        titles.add(UIUtil.mutedLabel("Review submitted quizzes from faculty authors and approve for participant availability"), BorderLayout.SOUTH);
        header.add(titles, BorderLayout.WEST);

        JPanel filterWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterWrap.setBackground(UIUtil.BACKGROUND);
        JLabel filterLabel = new JLabel("Filter Status:");
        filterLabel.setFont(UIUtil.normalFont());
        filterLabel.setForeground(UIUtil.TEXT_MUTED);
        statusFilterCombo.setFont(UIUtil.normalFont());
        statusFilterCombo.setPreferredSize(new Dimension(180, 32));
        filterWrap.add(filterLabel);
        filterWrap.add(statusFilterCombo);
        header.add(filterWrap, BorderLayout.EAST);

        return header;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton view = UIUtil.secondaryButton("🔍 Inspect Details");
        JButton approve = UIUtil.successButton("✔ Approve Quiz");
        JButton reject = UIUtil.dangerButton("✖ Reject Quiz");
        JButton refresh = UIUtil.secondaryButton("Refresh");

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

    private void configureTableRenderer() {
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
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
            allQuizzes = quizService.getAllQuizzes();
            applyFilter();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load quizzes:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String filter = (String) statusFilterCombo.getSelectedItem();
        filteredQuizzes = allQuizzes.stream()
                .filter(q -> filter == null || filter.equals("ALL STATUSES") || q.getStatus().name().equals(filter))
                .toList();

        tableModel.setRowCount(0);
        for (Quiz quiz : filteredQuizzes) {
            int qCount = 0;
            try {
                qCount = quizService.countQuestionsByQuiz(quiz.getId());
            } catch (DatabaseException ignored) {
            }
            tableModel.addRow(new Object[]{
                    quiz.getId(),
                    quiz.getTitle(),
                    quiz.getCreatorName() == null ? "Faculty" : quiz.getCreatorName(),
                    quiz.getDurationMinutes() + " mins",
                    quiz.getTotalMarks() + " pts",
                    quiz.getStatus().name(),
                    qCount + " question(s)"
            });
        }
    }

    private Quiz selectedQuiz() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredQuizzes.size()) {
            JOptionPane.showMessageDialog(this, "Select a quiz in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return filteredQuizzes.get(row);
    }

    private void viewDetails() {
        Quiz quiz = selectedQuiz();
        if (quiz == null) {
            return;
        }
        try {
            Quiz full = quizService.getQuizWithQuestions(quiz.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("QUIZ SPECIFICATION & QUESTION BANK\n");
            sb.append("=====================================================\n");
            sb.append("Quiz Title       : ").append(full.getTitle()).append('\n');
            sb.append("Description      : ").append(full.getDescription()).append('\n');
            sb.append("Author / Creator : ").append(full.getCreatorName()).append('\n');
            sb.append("Duration         : ").append(full.getDurationMinutes()).append(" minutes\n");
            sb.append("Total Marks      : ").append(full.getTotalMarks()).append('\n');
            sb.append("Current Status   : ").append(full.getStatus().name()).append("\n\n");
            sb.append("QUESTIONS (").append(full.getQuestions().size()).append(")\n");
            sb.append("-----------------------------------------------------\n");

            int number = 1;
            for (Question question : full.getQuestions()) {
                sb.append("Q").append(number++).append(": ").append(question.getText())
                        .append("  [").append(question.getMarks()).append(" mark(s)]\n");
                for (Option option : question.getOptions()) {
                    sb.append("   (").append(option.getLetter()).append(") ")
                            .append(option.getOptionText())
                            .append(option.isCorrect() ? "   <== [CORRECT ANSWER]" : "")
                            .append('\n');
                }
                if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
                    sb.append("   Explanation: ").append(question.getExplanation()).append('\n');
                }
                sb.append('\n');
            }

            JTextArea area = new JTextArea(sb.toString());
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            area.setEditable(false);
            area.setCaretPosition(0);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(680, 500));
            JOptionPane.showMessageDialog(this, scroll, "Quiz #" + full.getId() + " Inspection",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (QuizNotFoundException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeStatus(boolean approve) {
        Quiz quiz = selectedQuiz();
        if (quiz == null) {
            return;
        }
        try {
            if (approve) {
                if (quiz.getStatus() == QuizStatus.APPROVED) {
                    JOptionPane.showMessageDialog(this, "Quiz is already approved.", "Notice", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                quizService.approveQuiz(quiz.getId());
                JOptionPane.showMessageDialog(this,
                        "Quiz #" + quiz.getId() + " ('" + quiz.getTitle() + "') approved successfully.\nIt is now published and available to students.",
                        "Quiz Approved", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Reject quiz '" + quiz.getTitle() + "'?\nThe creator will see it as REJECTED.",
                        "Confirm Rejection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                quizService.rejectQuiz(quiz.getId());
                JOptionPane.showMessageDialog(this, "Quiz rejected.", "Quiz Rejected", JOptionPane.INFORMATION_MESSAGE);
            }
            refreshTable();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Action Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}