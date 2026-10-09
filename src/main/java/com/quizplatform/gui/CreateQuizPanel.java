package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.SystemSettingsService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Quiz Creator form: title, description and duration.
 * The quiz is created as status DRAFT. The author can then populate
 * questions in the Question Management Studio before submitting for approval.
 */
public class CreateQuizPanel extends JPanel {

    private final QuizCreatorDashboard dashboard;
    private final QuizCreator creator;
    private final QuizService quizService = new QuizService();

    private final JTextField titleField = new JTextField(26);
    private final JTextArea descriptionArea = new JTextArea(4, 26);
    private final JTextField durationField = new JTextField(8);

    public CreateQuizPanel(QuizCreatorDashboard dashboard, QuizCreator creator) {
        this.dashboard = dashboard;
        this.creator = creator;
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Create New Quiz"), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel("Fill in quiz metadata. Newly created quizzes start in 'Draft' mode for question authoring."), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        try {
            durationField.setText(String.valueOf(new SystemSettingsService().getDefaultDurationMinutes()));
        } catch (DatabaseException e) {
            durationField.setText("10");
        }
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIUtil.BACKGROUND);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(22, 28, 22, 28)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        card.add(UIUtil.fieldLabel("Quiz Title:"), gc);
        gc.gridx = 1;
        titleField.setFont(UIUtil.normalFont());
        titleField.setPreferredSize(new Dimension(300, 32));
        card.add(titleField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        card.add(UIUtil.fieldLabel("Description:"), gc);
        gc.gridx = 1;
        descriptionArea.setFont(UIUtil.normalFont());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setPreferredSize(new Dimension(300, 80));
        card.add(descScroll, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        card.add(UIUtil.fieldLabel("Duration (Minutes):"), gc);
        gc.gridx = 1;
        durationField.setFont(UIUtil.normalFont());
        durationField.setPreferredSize(new Dimension(100, 32));
        card.add(durationField, gc);

        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBackground(new Color(0xF8, 0xFA, 0xFC));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel infoTitle = new JLabel("💡 Authoring Workflow:");
        infoTitle.setFont(UIUtil.smallBoldFont());
        infoTitle.setForeground(UIUtil.TEXT);

        JLabel infoText = new JLabel("<html><small style='color:#64748B;'>"
                + "1. Click 'Save Quiz Draft' to create the quiz container.<br>"
                + "2. You will be automatically redirected to add questions and options.<br>"
                + "3. Once all questions are added, submit it from 'My Quizzes' for admin approval."
                + "</small></html>");
        infoText.setFont(UIUtil.smallFont());

        infoBox.add(infoTitle);
        infoBox.add(Box.createVerticalStrut(2));
        infoBox.add(infoText);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.insets = new Insets(14, 8, 4, 8);
        card.add(infoBox, gc);

        wrapper.add(card);
        return wrapper;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton create = UIUtil.successButton("💾 Save Quiz Draft & Add Questions");
        JButton clear = UIUtil.secondaryButton("Clear Form");

        create.addActionListener(e -> createQuiz());
        clear.addActionListener(e -> clearForm());

        panel.add(create);
        panel.add(clear);
        return panel;
    }

    private void createQuiz() {
        try {
            String title = titleField.getText().trim();
            String desc = descriptionArea.getText().trim();
            String durStr = durationField.getText().trim();

            if (title.isEmpty()) {
                throw new InvalidQuizException("Quiz title cannot be blank.");
            }
            int duration = Integer.parseInt(durStr);

            Quiz quiz = new Quiz(title, desc, duration);
            quiz.setCreatedBy(creator.getId());
            quizService.createQuiz(quiz);

            JOptionPane.showMessageDialog(this,
                    "Quiz '" + title + "' created successfully (Status: DRAFT).\n"
                            + "Now proceeding to question authoring studio.",
                    "Draft Created", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            dashboard.refreshQuizSelector();
            dashboard.openQuestionsFor(quiz.getId());

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Duration must be a positive integer in minutes.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidQuizException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could Not Create Quiz",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        titleField.setText("");
        descriptionArea.setText("");
        durationField.setText("10");
    }
}
