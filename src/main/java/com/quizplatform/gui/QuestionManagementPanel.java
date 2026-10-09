package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.service.QuizService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz Creator: author, edit, and delete questions and option sets for quizzes.
 *
 * Demonstrates:
 *  - JRadioButton + ButtonGroup for selecting the single correct answer
 *  - Collections: List<Question>, List<Quiz>
 *  - Generic and Custom Exception Handling (InvalidQuizException)
 */
public class QuestionManagementPanel extends JPanel {

    private final QuizCreatorDashboard dashboard;
    private final int creatorId;
    private final QuizService quizService = new QuizService();

    private final JComboBox<String> quizCombo = new JComboBox<>();
    private List<Quiz> quizList = new ArrayList<>();
    private Quiz currentQuiz;

    // ---- Questions table ----
    private final DefaultTableModel tableModel =
            UIUtil.emptyModel("ID", "Question Text", "Marks", "Correct Answer");
    private final JTable table = UIUtil.readOnlyTable(tableModel);
    private List<Question> questions = new ArrayList<>();
    private int selectedQuestionId = -1;

    // ---- Question form ----
    private final JTextArea questionArea = new JTextArea(3, 40);
    private final JTextField marksField = new JTextField("1", 6);
    private final JTextField[] optionFields = {
            new JTextField(32), new JTextField(32), new JTextField(32), new JTextField(32)};
    private final JRadioButton[] correctButtons = {
            new JRadioButton("A"), new JRadioButton("B"),
            new JRadioButton("C"), new JRadioButton("D")};
    private final ButtonGroup correctGroup = new ButtonGroup();
    private final JTextField explanationField = new JTextField(40);

    public QuestionManagementPanel(QuizCreatorDashboard dashboard, QuizCreator creator) {
        this.dashboard = dashboard;
        this.creatorId = creator.getId();
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(buildSelector(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                cardWrap("Questions in Selected Quiz", new JScrollPane(table)),
                cardWrap("Question & Options Editor", buildQuestionForm()));
        split.setResizeWeight(0.42);
        split.setBorder(null);
        split.setBackground(UIUtil.BACKGROUND);
        add(split, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        quizCombo.addActionListener(e -> loadSelectedQuiz());
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        refreshQuizSelector();
    }

    private JPanel cardWrap(String title, java.awt.Component content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.add(UIUtil.heading(title), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSelector() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JLabel label = UIUtil.fieldLabel("Target Quiz:");
        panel.add(label);

        quizCombo.setFont(UIUtil.normalFont());
        quizCombo.setPreferredSize(new Dimension(440, 32));
        panel.add(quizCombo);

        JButton load = UIUtil.primaryButton("🔄 Reload Questions");
        load.addActionListener(e -> loadSelectedQuiz());
        panel.add(load);

        return panel;
    }

    private JPanel buildQuestionForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.anchor = GridBagConstraints.WEST;

        questionArea.setFont(UIUtil.normalFont());
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        JScrollPane qScroll = new JScrollPane(questionArea);
        qScroll.setPreferredSize(new Dimension(480, 60));
        addRow(form, gc, 0, "Question Text:", qScroll);

        marksField.setFont(UIUtil.normalFont());
        addRow(form, gc, 1, "Marks Value:", marksField);

        for (int i = 0; i < 4; i++) {
            JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            optionRow.setBackground(Color.WHITE);

            correctButtons[i].setFont(UIUtil.boldFont());
            correctButtons[i].setFocusPainted(false);
            correctGroup.add(correctButtons[i]);
            optionRow.add(correctButtons[i]);

            JLabel optLabel = new JLabel("Option " + (char) ('A' + i) + ":");
            optLabel.setFont(UIUtil.normalFont());
            optionRow.add(optLabel);

            optionFields[i].setFont(UIUtil.normalFont());
            optionFields[i].setPreferredSize(new Dimension(360, 28));
            optionRow.add(optionFields[i]);

            addRow(form, gc, 2 + i, i == 0 ? "Options (A-D):" : "", optionRow);
        }
        correctButtons[0].setSelected(true);

        explanationField.setFont(UIUtil.normalFont());
        explanationField.setPreferredSize(new Dimension(480, 28));
        addRow(form, gc, 6, "Explanation:", explanationField);

        JLabel hint = new JLabel("<html><small style='color:#64748B;'>Select the radio button (A-D) representing the correct answer. Provide optional explanation for review.</small></html>");
        gc.gridx = 1;
        gc.gridy = 7;
        form.add(hint, gc);

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row, String label, java.awt.Component field) {
        gc.gridx = 0;
        gc.gridy = row;
        if (!label.isEmpty()) {
            form.add(UIUtil.fieldLabel(label), gc);
        } else {
            form.add(new JLabel(""), gc);
        }
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        form.add(field, gc);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton add = UIUtil.successButton("➕ Add Question");
        JButton update = UIUtil.primaryButton("💾 Update Question");
        JButton delete = UIUtil.dangerButton("🗑 Delete Question");
        JButton clear = UIUtil.secondaryButton("Clear Form");

        add.addActionListener(e -> addQuestion());
        update.addActionListener(e -> updateQuestion());
        delete.addActionListener(e -> deleteQuestion());
        clear.addActionListener(e -> clearForm());

        panel.add(add);
        panel.add(update);
        panel.add(delete);
        panel.add(clear);
        return panel;
    }

    public void refreshQuizSelector() {
        try {
            quizList = quizService.getQuizzesByCreator(creatorId);
            quizCombo.removeAllItems();
            for (Quiz quiz : quizList) {
                quizCombo.addItem("#" + quiz.getId() + " - " + quiz.getTitle()
                        + " [" + quiz.getStatus().name() + "]");
            }
            if (!quizList.isEmpty()) {
                quizCombo.setSelectedIndex(0);
            } else {
                currentQuiz = null;
                tableModel.setRowCount(0);
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load quizzes:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadQuiz(int quizId) {
        refreshQuizSelector();
        for (int i = 0; i < quizList.size(); i++) {
            if (quizList.get(i).getId() == quizId) {
                if (quizCombo.getSelectedIndex() == i) {
                    loadSelectedQuiz();
                } else {
                    quizCombo.setSelectedIndex(i);
                }
                return;
            }
        }
    }

    private void loadSelectedQuiz() {
        int index = quizCombo.getSelectedIndex();
        if (index < 0 || index >= quizList.size()) {
            currentQuiz = null;
            tableModel.setRowCount(0);
            return;
        }
        currentQuiz = quizList.get(index);
        try {
            questions = quizService.getQuestionsForQuiz(currentQuiz.getId());
            tableModel.setRowCount(0);
            for (Question question : questions) {
                Option correct = question.getCorrectOption();
                tableModel.addRow(new Object[]{
                        question.getId(),
                        question.getText(),
                        question.getMarks() + " mark(s)",
                        correct == null ? "-" : "(" + correct.getLetter() + ") " + correct.getOptionText()
                });
            }
            clearForm();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load questions:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= questions.size()) {
            return;
        }
        selectedQuestionId = (int) tableModel.getValueAt(row, 0);
        for (Question question : questions) {
            if (question.getId() == selectedQuestionId) {
                questionArea.setText(question.getText());
                marksField.setText(String.valueOf(question.getMarks()));
                explanationField.setText(question.getExplanation() == null ? "" : question.getExplanation());
                for (int i = 0; i < 4; i++) {
                    optionFields[i].setText("");
                    correctButtons[i].setSelected(false);
                }
                for (Option option : question.getOptions()) {
                    int index = option.getOptionOrder() - 1;
                    if (index >= 0 && index < 4) {
                        optionFields[index].setText(option.getOptionText());
                        if (option.isCorrect()) {
                            correctButtons[index].setSelected(true);
                        }
                    }
                }
                break;
            }
        }
    }

    private Question buildQuestionFromForm(int questionId) throws InvalidQuizException {
        if (currentQuiz == null) {
            throw new InvalidQuizException("Please select a quiz first before authoring questions.");
        }
        int marks;
        try {
            marks = Integer.parseInt(marksField.getText().trim());
        } catch (NumberFormatException e) {
            throw new InvalidQuizException("Marks must be a whole positive integer.");
        }

        String text = questionArea.getText().trim();
        if (text.isEmpty()) {
            throw new InvalidQuizException("Question text cannot be blank.");
        }

        Question question = new Question(text, marks, explanationField.getText().trim());
        question.setId(questionId);
        question.setQuizId(currentQuiz.getId());

        int correctIndex = 0;
        for (int i = 0; i < 4; i++) {
            if (correctButtons[i].isSelected()) {
                correctIndex = i;
            }
            String optText = optionFields[i].getText().trim();
            if (!optText.isEmpty()) {
                question.addOption(new Option(optText, i + 1, i == correctIndex));
            }
        }
        return question;
    }

    private void addQuestion() {
        if (currentQuiz == null) {
            JOptionPane.showMessageDialog(this, "Select a quiz first.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Question question = buildQuestionFromForm(0);
            quizService.addQuestion(question);
            JOptionPane.showMessageDialog(this, "Question added successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadSelectedQuiz();
        } catch (InvalidQuizException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could Not Add Question",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateQuestion() {
        if (selectedQuestionId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a question in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Question question = buildQuestionFromForm(selectedQuestionId);
            quizService.updateQuestion(question);
            JOptionPane.showMessageDialog(this, "Question updated successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadSelectedQuiz();
        } catch (InvalidQuizException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could Not Update Question",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteQuestion() {
        if (selectedQuestionId <= 0 || currentQuiz == null) {
            JOptionPane.showMessageDialog(this, "Select a question in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!UIUtil.confirm(this, "Delete selected question #" + selectedQuestionId + "?")) {
            return;
        }
        try {
            quizService.deleteQuestion(selectedQuestionId, currentQuiz.getId());
            JOptionPane.showMessageDialog(this, "Question deleted.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadSelectedQuiz();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Delete failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedQuestionId = -1;
        questionArea.setText("");
        marksField.setText("1");
        explanationField.setText("");
        for (int i = 0; i < 4; i++) {
            optionFields[i].setText("");
        }
        correctButtons[0].setSelected(true);
        table.clearSelection();
    }
}
