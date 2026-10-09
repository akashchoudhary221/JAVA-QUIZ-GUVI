package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.TimeExpiredException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.Option;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.Result;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultService;
import com.quizplatform.timer.QuizTimer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The screen where a participant actually writes a quiz.
 *
 * Demonstrates MULTITHREADING + SWING THREADING:
 *  - a dedicated QuizTimer thread counts down once per second
 *  - all Swing updates happen through SwingUtilities.invokeLater()
 *  - when the countdown reaches 00:00 the quiz is submitted automatically
 *  - after expiry the participant can no longer answer questions
 */
public class QuizAttemptPanel extends JPanel {

    private final ParticipantDashboard dashboard;
    private final Participant participant;
    private final QuizService quizService = new QuizService();
    private final ResultService resultService = new ResultService();

    // ---- attempt state ----
    private Quiz currentQuiz;
    private List<Question> questions = new ArrayList<>();
    private final Map<Integer, Long> selectedAnswers = new HashMap<>(); // questionId -> optionId (-1 = none)
    private int currentIndex;
    private boolean submitted;
    private long startedAtMillis;
    private QuizTimer timer;

    // ---- Swing components ----
    private final JLabel quizTitleLabel = new JLabel("No quiz loaded");
    private final JLabel timerLabel = new JLabel("Time Remaining: --:--");
    private final JLabel progressLabel = new JLabel(" ");
    private final JPanel questionArea = new JPanel(new BorderLayout(0, 10));
    private final JButton prevButton = UIUtil.neutralButton("<< Previous");
    private final JButton nextButton = UIUtil.primaryButton("Next >>");
    private final JButton submitButton = UIUtil.successButton("Submit Quiz");

    private JPanel optionsPanel;
    private javax.swing.ButtonGroup currentOptionGroup = new javax.swing.ButtonGroup();

    // Timer callbacks arrive on the QuizTimer thread -> marshal to the EDT
    private final QuizTimer.QuizTimerListener timerListener = new QuizTimer.QuizTimerListener() {
        @Override
        public void onTick(int secondsRemaining) {
            // Demonstrates correct Swing threading
            javax.swing.SwingUtilities.invokeLater(() -> {
                timerLabel.setText("Time Remaining: " + QuizTimer.format(secondsRemaining));
                if (secondsRemaining <= 30) {
                    timerLabel.setForeground(UIUtil.DANGER);
                }
            });
        }

        @Override
        public void onTimeExpired() {
            javax.swing.SwingUtilities.invokeLater(() -> {
                timerLabel.setText("Time Remaining: 00:00 - AUTO SUBMIT");
                UIUtil.info(QuizAttemptPanel.this,
                        "Time is over!\nThe quiz was submitted automatically.");
                submitQuiz(true);
            });
        }
    };

    public QuizAttemptPanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        this.participant = participant;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        questionArea.setBackground(UIUtil.BACKGROUND);
        add(new JScrollPane(questionArea), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        prevButton.addActionListener(e -> navigate(-1));
        nextButton.addActionListener(e -> navigate(1));
        submitButton.addActionListener(e -> submitQuiz(false));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBackground(UIUtil.BACKGROUND);
        quizTitleLabel.setFont(UIUtil.headingFont());
        quizTitleLabel.setForeground(UIUtil.TEXT);
        header.add(quizTitleLabel, BorderLayout.WEST);

        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        timerLabel.setForeground(UIUtil.ACCENT);
        header.add(timerLabel, BorderLayout.EAST);

        progressLabel.setFont(UIUtil.normalFont());
        header.add(progressLabel, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        footer.setBackground(UIUtil.BACKGROUND);
        footer.add(prevButton);
        footer.add(nextButton);
        footer.add(submitButton);
        return footer;
    }

    /**
     * Loads a quiz, starts the countdown THREAD and displays question 1.
     * Called by ParticipantDashboard.startQuiz().
     */
    public void prepareQuiz(Quiz quiz) {
        try {
            if (timer != null) {
                timer.stop();
            }
            currentQuiz = quiz;
            questions = quizService.getQuestionsForQuiz(quiz.getId());
            if (questions.isEmpty()) {
                UIUtil.error(this, "This quiz has no questions - it cannot be started.");
                return;
            }
            selectedAnswers.clear();
            currentIndex = 0;
            submitted = false;
            startedAtMillis = System.currentTimeMillis();

            quizTitleLabel.setText(quiz.getTitle() + "  (" + questions.size() + " questions, "
                    + quiz.getTotalMarks() + " marks)");
            timerLabel.setForeground(UIUtil.ACCENT);
            timerLabel.setText("Time Remaining: " + QuizTimer.format(quiz.getDurationMinutes() * 60));
            submitButton.setEnabled(true);

            // ---- Demonstrates MULTITHREADING: dedicated countdown thread ----
            timer = new QuizTimer(quiz.getDurationMinutes() * 60, timerListener);
            renderQuestion();
            timer.start();
        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load the quiz:\n" + e.getMessage());
        }
    }

    /** Rebuilds the question card for currentIndex (text + option radio buttons). */
    private void renderQuestion() {
        Question question = questions.get(currentIndex);

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JLabel text = new JLabel("<html><body style='width:640px'>"
                + "<b>Question " + (currentIndex + 1) + " of " + questions.size() + "</b><br><br>"
                + question.getText() + "</body></html>");
        text.setFont(UIUtil.normalFont());
        card.add(text, BorderLayout.NORTH);

        // One radio button per option - JRadioButton + ButtonGroup (rubric requirement)
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(Color.WHITE);
        currentOptionGroup = new javax.swing.ButtonGroup();

        long selected = selectedAnswers.getOrDefault(question.getId(), -1L);
        for (Option option : question.getOptions()) {
            JRadioButton radio = new JRadioButton(option.getLetter() + ")  " + option.getOptionText());
            radio.setFont(UIUtil.normalFont());
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            radio.putClientProperty("optionId", option.getId());   // remember which option this is
            if (option.getId() == selected) {
                radio.setSelected(true);
            }
            currentOptionGroup.add(radio);
            optionsPanel.add(radio);
            optionsPanel.add(Box.createVerticalStrut(6));
        }
        card.add(optionsPanel, BorderLayout.CENTER);

        questionArea.removeAll();
        questionArea.add(card, BorderLayout.CENTER);
        questionArea.revalidate();
        questionArea.repaint();

        progressLabel.setText("Question " + (currentIndex + 1) + " of " + questions.size()
                + "   |   Selected answers: " + selectedAnswers.size());
        prevButton.setEnabled(currentIndex > 0);
        nextButton.setEnabled(currentIndex < questions.size() - 1);
    }

    /** Saves the radio selection, then shows the previous/next question. */
    private void navigate(int direction) {
        if (submitted) {
            return;
        }
        rememberSelection();
        int target = currentIndex + direction;
        if (target >= 0 && target < questions.size()) {
            currentIndex = target;
            renderQuestion();
        }
    }

    /** Stores the selected option of the current question in the answers map. */
    private void rememberSelection() {
        if (optionsPanel == null || currentQuiz == null) {
            return;
        }
        Question question = questions.get(currentIndex);
        long chosen = -1L;
        for (Component component : optionsPanel.getComponents()) {
            if (component instanceof JRadioButton radio && radio.isSelected()) {
                chosen = (Long) radio.getClientProperty("optionId");
            }
        }
        if (chosen > 0) {
            selectedAnswers.put(question.getId(), chosen);
        } else {
            selectedAnswers.remove(question.getId());
        }
        progressLabel.setText("Question " + (currentIndex + 1) + " of " + questions.size()
                + "   |   Selected answers: " + selectedAnswers.size());
    }

    /**
     * Stores the attempt (attempt + answers + result in ONE transaction).
     *
     * @param timedOut true when called by the timer thread at 00:00
     */
    private void submitQuiz(boolean timedOut) {
        if (submitted || currentQuiz == null) {
            return;
        }
        if (!timedOut) {
            rememberSelection();
            int unanswered = questions.size() - selectedAnswers.size();
            if (unanswered > 0 && !UIUtil.confirm(this,
                    unanswered + " question(s) are unanswered. Submit anyway?")) {
                return;   // participant wants to continue
            }
        }

        submitted = true;
        submitButton.setEnabled(false);
        if (timer != null) {
            timer.stop();     // Demonstrates interrupt() handling inside QuizTimer
        }

        // build the final List<Answer> (unanswered -> -1)
        List<Answer> answers = new ArrayList<>();
        for (Question question : questions) {
            answers.add(new Answer(question.getId(),
                    selectedAnswers.getOrDefault(question.getId(), -1L)));
        }

        long timeTaken = (System.currentTimeMillis() - startedAtMillis) / 1000;
        if (timedOut) {
            timeTaken = currentQuiz.getDurationMinutes() * 60L;
        }

        try {
            Result result = resultService.submitAttempt(currentQuiz, questions,
                    participant.getId(), participant.getName(), answers, timeTaken, timedOut);
            dashboard.showResult(result, currentQuiz, questions, selectedAnswers);
            dashboard.refreshStatistics();
        } catch (TimeExpiredException e) {
            // Demonstrates custom exception handling: timer expiry
            UIUtil.info(this, e.getMessage());
        } catch (InvalidQuizException | DatabaseException e) {
            UIUtil.error(this, "Submission failed:\n" + e.getMessage());
            submitted = false;          // allow the participant to try again
            submitButton.setEnabled(true);
        }
    }
}