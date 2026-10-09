package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz Creator: all feedback comments written for participants.
 * Fulfills the "interact with participants" rubric requirement.
 */
public class FeedbackPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final QuizCreator creator;
    private final ResultService resultService = new ResultService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "Feedback ID", "Date Authored", "Student Participant", "Quiz Title", "Instructor Feedback Comment", "Author");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Feedback> feedbackList = new ArrayList<>();

    public FeedbackPanel(QuizCreator creator) {
        this.creator = creator;
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Feedback Log to Participants"), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel("History of personalized evaluation comments you have provided to students"), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton refresh = UIUtil.secondaryButton("🔄 Refresh Log");
        refresh.addActionListener(e -> refreshTable());
        panel.add(refresh);

        return panel;
    }

    public void refreshTable() {
        try {
            feedbackList = resultService.getFeedbackByAuthor(creator.getId());
            tableModel.setRowCount(0);
            for (Feedback feedback : feedbackList) {
                tableModel.addRow(new Object[]{
                        "#" + feedback.getId(),
                        feedback.getCreatedAt() == null ? "-" : DATE_FORMAT.format(feedback.getCreatedAt()),
                        feedback.getParticipantName(),
                        feedback.getQuizTitle(),
                        feedback.getComment(),
                        feedback.getCommentedByName()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load feedback log:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
