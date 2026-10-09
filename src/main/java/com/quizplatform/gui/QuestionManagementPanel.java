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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz Creator: add / edit / delete questions of a selected quiz.
 *
 * Demonstrates:
 *  - JRadioButton + ButtonGroup for choosing the correct answer (A-D)
 *  - Collections: List<Question>, List<Quiz>
 *  - custom exceptions from the service layer (InvalidQuizException)
 */
public class QuestionManagementPanel extends JPanel {

    private final QuizCreatorDashboard dashboard;
    private final int creatorId;
    private final QuizService quizService = new QuizService();

    private final JComboBox<String> quizCombo = new JComboBox<>();
    private List<Quiz> quizList = new ArrayList<>();
    private Quiz currentQuiz;

    // ---- questions table ----
    private final DefaultTableModel tableModel =
            UIUtil.emptyModel("ID", "Question", "Marks", "Correct Answer");
    private final JTable table = UIUtil.readOnlyTable(tableModel);
    private List<Question> questions = new ArrayList<>();
    private int selectedQuestionId = -1;

    // ---- question form ----
    private final JTextArea questionArea = new JTextArea(3, 40);
    private final JTextField marksField = new JTextField("1", 5);
    private final JTextField[] optionFields = {
            new JTextField(30), new JTextField(30), new JTextField(30), new JTextField(30)};
    private final JRadioButton[] correctButtons = {
            new JRadioButton("A"), new JRadioButton("B"),
            new JRadioButton("C"), new JRadioButton("D")};
    private final ButtonGroup correctGroup = new ButtonGroup();
    private final JTextField explanationField = new JTextField(40);

    public QuestionManagementPanel(QuizCreatorDashboard dashboard, QuizCreator creator) {
        this.dashboard = dashboard;
        this.creatorId = creator.getId();
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildSelector(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                cardWrap("Questions of selected quiz", new JScrollPane(table)),
                cardWrap("Question details", buildQuestionForm()));
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setBackground(UIUtil.BACKGROUND);
        add(split, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        quizCombo.addActionListener(e -> loadSelectedQuiz());
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        refreshQuizSelector();
    }

    private JPanel cardWrap(String title, java.awt.Component content) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.add(UIUtil.heading(title), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSelector() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);
        panel.add(UIUtil.fieldLabel("Select quiz:"));
        quizCombo.setPreferredSize(new java.awt.Dimension(420, 30));
        panel.add(quizCombo);
        JButton load = UIUtil.primaryButton("Load Questions");
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

        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        addRow(form, gc, 0, "Question:", new JScrollPane(questionArea));
        addRow(form, gc, 1, "Marks:", marksField);

        for (int i = 0; i < 4; i++) {
            JPanel optionRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
            optionRow.setBackground(Color.WHITE);
            correctGroup.add(correctButtons[i]);
            optionRow.add(correctButtons[i]);
            optionRow.add(new JLabel("Option " + (char) ('A' + i) + ":"));
            optionRow.add(optionFields[i]);
            addRow(form, gc, 2 + i, i == 0 ? "Options:" : "", optionRow);
        }
        correctButtons[0].setSelected(true);

        addRow(form, gc, 6, "Explanation / feedback:", explanationField);

        JLabel hint = new JLabel("<html><small>Select the radio button of the CORRECT option. "
                + "At least 2 options are required.</small></html>");
        hint.setFont(UIUtil.smallFont());
        hint.setForeground(Color.GRAY);
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
        }
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        form.add(field, gc);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);

        JButton add = UIUtil.successButton("Add Question");
        JButton update = UIUtil.primaryButton("Update Question");
        JButton delete = UIUtil.dangerButton("Delete Question");
        JButton clear = UIUtil.neutralButton("Clear Form");

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

    /** Reloads the quiz combo box (called after creating/deleting a quiz). */
    public void refreshQuizSelector() {
        try {
            quizList = quizService.getQuizzesByCreator(creatorId);
            quizCombo.removeAllItems();
            for (Quiz quiz : quizList) {
                quizCombo.addItem("#" + quiz.getId() + "  " + quiz.getTitle()
                        + "  [" + quiz.getStatus().getLabel() + "]");
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

    /** Opens the question editor for a specific quiz (called by the dashboard). */
    public void loadQuiz(int quizId) {
        refreshQuizSelector();
        for (int i = 0; i < quizList.size(); i++) {
            if (quizList.get(i).getId() == quizId) {
                if (quizCombo.getSelectedIndex() == i) {
                    loadSelectedQuiz();   // selection unchanged -> load manually
                } else {
                    quizCombo.setSelectedIndex(i);   // combo listener loads the questions
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
                        question.getMarks(),
                        correct == null ? "-" : correct.getLetter() + ") " + correct.getOptionText()
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
        if (row < 0) {
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

    /** Builds a Question (with its 4 options) from the form values. */
    private Question buildQuestionFromForm(int questionId) throws InvalidQuizException {
        if (currentQuiz == null) {
            throw new InvalidQuizException("Select a quiz before entering a question.");
        }
        int marks;
        try {
            marks = Integer.parseInt(marksField.getText().trim());
        } catch (NumberFormatException e) {
            throw new InvalidQuizException("Marks must be a whole number.");
        }

        Question question = new Question(questionArea.getText().trim(), marks,
                explanationField.getText().trim());
        question.setId(questionId);
        question.setQuizId(currentQuiz.getId());

        int correctIndex = 0;
        for (int i = 0; i < 4; i++) {
            if (correctButtons[i].isSelected()) {
                correctIndex = i;
            }
            String text = optionFields[i].getText().trim();
            if (!text.isEmpty()) {          // empty trailing options are ignored
                question.addOption(new Option(text, i + 1, i == correctIndex));
            }
        }
        return question;   // QuizService performs the full validation
    }

    private void addQuestion() {
        if (currentQuiz == null) {
            JOptionPane.showMessageDialog(this, "Select a quiz first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Question question = buildQuestionFromForm(0);
            quizService.addQuestion(question);
            JOptionPane.showMessageDialog(this, "Question added.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadSelectedQuiz();
        } catch (InvalidQuizException | DatabaseException e) {
            // Demonstrates custom exception handling
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not add question",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateQuestion() {
        if (selectedQuestionId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a question in the table first.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Question question = buildQuestionFromForm(selectedQuestionId);
            quizService.updateQuestion(question);
            JOptionPane.showMessageDialog(this, "Question updated.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadSelectedQuiz();
        } catch (InvalidQuizException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not update question",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteQuestion() {
        if (selectedQuestionId <= 0 || currentQuiz == null) {
            JOptionPane.showMessageDialog(this, "Select a question in the table first.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!UIUtil.confirm(this, "Delete the selected question?")) {
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
