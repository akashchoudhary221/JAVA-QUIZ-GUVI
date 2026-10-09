package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.SystemSettingsService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Quiz Creator form: title, description and duration.
 * The quiz is stored with status DRAFT and can then be filled with
 * questions in the Questions screen.
 */
public class CreateQuizPanel extends JPanel {

    private final QuizCreatorDashboard dashboard;
    private final QuizCreator creator;
    private final QuizService quizService = new QuizService();

    private final JTextField titleField = new JTextField(24);
    private final JTextArea descriptionArea = new JTextArea(4, 24);
    private final JTextField durationField = new JTextField(6);

    public CreateQuizPanel(QuizCreatorDashboard dashboard, QuizCreator creator) {
        this.dashboard = dashboard;
        this.creator = creator;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        // default duration comes from the admin "System Settings" (system_settings table)
        try {
            durationField.setText(String.valueOf(new SystemSettingsService().getDefaultDurationMinutes()));
        } catch (DatabaseException e) {
            durationField.setText("10");
        }
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(9, 6, 9, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        form.add(UIUtil.fieldLabel("Quiz Title:"), gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        form.add(titleField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        form.add(UIUtil.fieldLabel("Description:"), gc);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        form.add(new JScrollPane(descriptionArea), gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        form.add(UIUtil.fieldLabel("Duration (minutes):"), gc);
        gc.gridx = 1;
        gc.anchor = GridBagConstraints.WEST;
        form.add(durationField, gc);

        JLabel hint = new JLabel("<html><small>Allowed duration: 1 - 480 minutes. "
                + "The quiz starts as <b>Draft</b>; add questions, then submit it for admin approval.</small></html>");
        hint.setFont(UIUtil.smallFont());
        hint.setForeground(Color.GRAY);
        gc.gridx = 1;
        gc.gridy = 3;
        form.add(hint, gc);
        return form;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);
        JButton create = UIUtil.successButton("Create Quiz (Draft)");
        JButton clear = UIUtil.neutralButton("Clear");
        create.addActionListener(e -> createQuiz());
        clear.addActionListener(e -> clearForm());
        panel.add(create);
        panel.add(clear);
        return panel;
    }

    private void createQuiz() {
        try {
            Quiz quiz = new Quiz(titleField.getText().trim(),
                    descriptionArea.getText().trim(),
                    Integer.parseInt(durationField.getText().trim()));
            quiz.setCreatedBy(creator.getId());
            quizService.createQuiz(quiz);      // throws InvalidQuizException / DatabaseException

            JOptionPane.showMessageDialog(this,
                    "Quiz created successfully (status: Draft).\nNow add questions to it.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            dashboard.refreshQuizSelector();
            dashboard.openQuestionsFor(quiz.getId());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Duration must be a whole number of minutes.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidQuizException | DatabaseException e) {
            // Demonstrates custom exception handling
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not create quiz",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        titleField.setText("");
        descriptionArea.setText("");
        durationField.setText("10");
    }
}
