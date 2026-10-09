package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Quiz;
import com.quizplatform.service.QuizService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Participant screen: lists approved quizzes and allows starting a quiz.
 * Includes search/filter capability and structured quiz details.
 */
public class QuizSelectionPanel extends JPanel {

    private final ParticipantDashboard dashboard;
    private final QuizService quizService = new QuizService();

    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Quiz Title", "Description", "Duration", "Marks", "Creator");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Quiz> allQuizzes = new ArrayList<>();
    private List<Quiz> filteredQuizzes = new ArrayList<>();

    public QuizSelectionPanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        refreshTable();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 8));
        header.setBackground(UIUtil.BACKGROUND);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setBackground(UIUtil.BACKGROUND);
        JLabel titleLabel = UIUtil.heading("Available Quizzes");
        JLabel descLabel = UIUtil.mutedLabel("Select an approved quiz below and click 'Start Selected Quiz'");
        titles.add(titleLabel, BorderLayout.NORTH);
        titles.add(descLabel, BorderLayout.SOUTH);
        header.add(titles, BorderLayout.WEST);

        // Search Bar on right
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBar.setBackground(UIUtil.BACKGROUND);
        JLabel searchIcon = new JLabel("🔍 Search:");
        searchIcon.setFont(UIUtil.normalFont());
        searchIcon.setForeground(UIUtil.TEXT_MUTED);
        searchField.setFont(UIUtil.normalFont());
        searchField.setPreferredSize(new Dimension(220, 32));
        searchBar.add(searchIcon);
        searchBar.add(searchField);
        header.add(searchBar, BorderLayout.EAST);

        return header;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton start = UIUtil.primaryButton("▶ Start Selected Quiz");
        JButton refresh = UIUtil.secondaryButton("Refresh");

        start.addActionListener(e -> startSelectedQuiz());
        refresh.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        panel.add(start);
        panel.add(refresh);
        return panel;
    }

    public void refreshTable() {
        try {
            allQuizzes = quizService.getApprovedQuizzes();
            applyFilter();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load quizzes:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String filter = searchField.getText().trim().toLowerCase();
        filteredQuizzes = allQuizzes.stream()
                .filter(q -> filter.isEmpty()
                        || q.getTitle().toLowerCase().contains(filter)
                        || (q.getDescription() != null && q.getDescription().toLowerCase().contains(filter))
                        || (q.getCreatorName() != null && q.getCreatorName().toLowerCase().contains(filter)))
                .toList();

        tableModel.setRowCount(0);
        for (Quiz quiz : filteredQuizzes) {
            String desc = quiz.getDescription() == null ? "" : quiz.getDescription();
            if (desc.length() > 65) {
                desc = desc.substring(0, 65) + "...";
            }
            tableModel.addRow(new Object[]{
                    quiz.getId(),
                    quiz.getTitle(),
                    desc,
                    quiz.getDurationMinutes() + " mins",
                    quiz.getTotalMarks() + " pts",
                    quiz.getCreatorName() == null ? "Faculty" : quiz.getCreatorName()
            });
        }
    }

    private void startSelectedQuiz() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredQuizzes.size()) {
            JOptionPane.showMessageDialog(this, "Please select a quiz from the table first.",
                    "No Quiz Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Quiz quiz = filteredQuizzes.get(row);

        String message = "Ready to start quiz:\n\n"
                + "• Title: " + quiz.getTitle() + "\n"
                + "• Duration: " + quiz.getDurationMinutes() + " minutes\n"
                + "• Total Marks: " + quiz.getTotalMarks() + "\n\n"
                + "Instructions:\n"
                + "1. The countdown timer starts immediately once you confirm.\n"
                + "2. Questions can be navigated via the question palette or Next/Previous buttons.\n"
                + "3. The quiz will auto-submit when the countdown timer reaches 00:00.\n\n"
                + "Do you wish to begin now?";

        if (UIUtil.confirm(this, message)) {
            dashboard.startQuiz(quiz);
        }
    }
}
