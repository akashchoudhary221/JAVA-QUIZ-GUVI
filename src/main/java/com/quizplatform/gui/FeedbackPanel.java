package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.service.ResultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz Creator: all feedback (comments) that this creator has written
 * for participants - the "interact with participants" requirement.
 */
public class FeedbackPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final QuizCreator creator;
    private final ResultService resultService = new ResultService();

    private final DefaultTableModel tableModel = UIUtil.emptyModel(
            "ID", "Date", "Participant", "Quiz", "Comment", "Written By");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    private List<Feedback> feedbackList = new ArrayList<>();

    public FeedbackPanel(QuizCreator creator) {
        this.creator = creator;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Feedback Given to Participants"), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        refreshTable();
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);
        JButton refresh = UIUtil.primaryButton("Refresh");
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
                        feedback.getId(),
                        feedback.getCreatedAt() == null ? "-" : DATE_FORMAT.format(feedback.getCreatedAt()),
                        feedback.getParticipantName(),
                        feedback.getQuizTitle(),
                        feedback.getComment(),
                        feedback.getCommentedByName()
                });
            }
        } catch (DatabaseException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not load feedback:\n" + e.getMessage(),
                    "Database error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}
