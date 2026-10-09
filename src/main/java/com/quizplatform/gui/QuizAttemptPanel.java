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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern participant quiz execution screen.
 *
 * Demonstrates:
 *  - MULTITHREADING: Dedicated QuizTimer thread counting down once per second
 *  - SWING THREADING: All GUI updates dispatched to Event Dispatch Thread via invokeLater()
 *  - QUESTION PALETTE: Interactive answered/unanswered question navigation matrix
 *  - VISUALLY DISTINCT ANSWER STATES: Card-style radio option selection
 *  - RACE CONDITION PREVENTION: Thread-safe auto-submission vs manual submission
 */
public class QuizAttemptPanel extends JPanel {

    private final ParticipantDashboard dashboard;
    private final Participant participant;
    private final QuizService quizService = new QuizService();
    private final ResultService resultService = new ResultService();

    // ---- Attempt state ----
    private Quiz currentQuiz;
    private List<Question> questions = new ArrayList<>();
    private final Map<Integer, Long> selectedAnswers = new HashMap<>(); // questionId -> optionId
    private int currentIndex;
    private boolean submitted;
    private long startedAtMillis;
    private QuizTimer timer;

    // ---- GUI components ----
    private final JLabel quizTitleLabel = new JLabel("No quiz loaded");
    private final JLabel quizMetaLabel = new JLabel(" ");
    private final JLabel timerLabel = new JLabel("⏱ 00:00", SwingConstants.CENTER);
    private final JPanel timerBadge = new JPanel(new BorderLayout());

    private final JPanel questionContent = new JPanel(new BorderLayout(0, 14));
    private final JPanel palettePanel = new JPanel();
    private final List<JButton> paletteButtons = new ArrayList<>();

    private final JButton prevButton = UIUtil.secondaryButton("← Previous");
    private final JButton nextButton = UIUtil.primaryButton("Next →");
    private final JButton submitButton = UIUtil.successButton("✔ Submit Quiz");

    private JPanel optionsPanel;
    private ButtonGroup currentOptionGroup = new ButtonGroup();

    // Timer listener marshaled cleanly to the EDT
    private final QuizTimer.QuizTimerListener timerListener = new QuizTimer.QuizTimerListener() {
        @Override
        public void onTick(int secondsRemaining) {
            SwingUtilities.invokeLater(() -> {
                timerLabel.setText("⏱ " + QuizTimer.format(secondsRemaining));
                if (secondsRemaining <= 30) {
                    timerBadge.setBackground(UIUtil.DANGER_LIGHT);
                    timerBadge.setBorder(BorderFactory.createLineBorder(UIUtil.DANGER, 2));
                    timerLabel.setForeground(UIUtil.DANGER);
                } else if (secondsRemaining <= 60) {
                    timerBadge.setBackground(UIUtil.WARNING_LIGHT);
                    timerBadge.setBorder(BorderFactory.createLineBorder(UIUtil.WARNING, 1));
                    timerLabel.setForeground(UIUtil.WARNING);
                } else {
                    timerBadge.setBackground(Color.WHITE);
                    timerBadge.setBorder(BorderFactory.createLineBorder(UIUtil.BORDER, 1));
                    timerLabel.setForeground(UIUtil.ACCENT);
                }
            });
        }

        @Override
        public void onTimeExpired() {
            SwingUtilities.invokeLater(() -> {
                timerLabel.setText("⏱ TIME OVER");
                timerBadge.setBackground(UIUtil.DANGER_LIGHT);
                timerLabel.setForeground(UIUtil.DANGER);
                JOptionPane.showMessageDialog(QuizAttemptPanel.this,
                        "Time limit reached!\nYour quiz has been automatically submitted.",
                        "Time Expired", JOptionPane.INFORMATION_MESSAGE);
                submitQuiz(true);
            });
        }
    };

    public QuizAttemptPanel(ParticipantDashboard dashboard, Participant participant) {
        this.dashboard = dashboard;
        this.participant = participant;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        prevButton.addActionListener(e -> navigate(-1));
        nextButton.addActionListener(e -> navigate(1));
        submitButton.addActionListener(e -> submitQuiz(false));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(Color.WHITE);

        quizTitleLabel.setFont(UIUtil.subTitleFont());
        quizTitleLabel.setForeground(UIUtil.TEXT);

        quizMetaLabel.setFont(UIUtil.smallFont());
        quizMetaLabel.setForeground(UIUtil.TEXT_MUTED);

        left.add(quizTitleLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(quizMetaLabel);
        header.add(left, BorderLayout.WEST);

        // Timer badge on right
        timerBadge.setBackground(Color.WHITE);
        timerBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        timerLabel.setForeground(UIUtil.ACCENT);
        timerBadge.add(timerLabel, BorderLayout.CENTER);
        header.add(timerBadge, BorderLayout.EAST);

        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setBackground(UIUtil.BACKGROUND);

        // Left Side: Question Navigation Palette Card
        JPanel paletteCard = new JPanel(new BorderLayout(0, 10));
        paletteCard.setBackground(Color.WHITE);
        paletteCard.setPreferredSize(new Dimension(220, 300));
        paletteCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel palTitle = new JLabel("Question Navigator");
        palTitle.setFont(UIUtil.headingFont());
        palTitle.setForeground(UIUtil.TEXT);
        paletteCard.add(palTitle, BorderLayout.NORTH);

        palettePanel.setBackground(Color.WHITE);
        paletteCard.add(new JScrollPane(palettePanel), BorderLayout.CENTER);

        // Palette legend
        JPanel legend = new JPanel(new GridLayout(3, 1, 0, 4));
        legend.setBackground(Color.WHITE);
        legend.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        legend.add(createLegendItem("🟢 Answered", UIUtil.SUCCESS));
        legend.add(createLegendItem("🔵 Current", UIUtil.ACCENT));
        legend.add(createLegendItem("⚪ Unanswered", UIUtil.TEXT_MUTED));
        paletteCard.add(legend, BorderLayout.SOUTH);

        center.add(paletteCard, BorderLayout.WEST);

        // Right Side: Question Card
        questionContent.setBackground(UIUtil.BACKGROUND);
        center.add(questionContent, BorderLayout.CENTER);

        return center;
    }

    private JLabel createLegendItem(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtil.smallFont());
        label.setForeground(color);
        return label;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));

        JPanel navGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navGroup.setBackground(Color.WHITE);
        navGroup.add(prevButton);
        navGroup.add(nextButton);
        footer.add(navGroup, BorderLayout.WEST);

        JPanel submitGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        submitGroup.setBackground(Color.WHITE);
        submitGroup.add(submitButton);
        footer.add(submitGroup, BorderLayout.EAST);

        return footer;
    }

    /**
     * Loads a quiz, builds the question palette, and starts the countdown thread.
     */
    public void prepareQuiz(Quiz quiz) {
        try {
            if (timer != null) {
                timer.stop();
            }
            currentQuiz = quiz;
            questions = quizService.getQuestionsForQuiz(quiz.getId());
            if (questions.isEmpty()) {
                UIUtil.error(this, "This quiz currently has no questions and cannot be started.");
                return;
            }
            selectedAnswers.clear();
            currentIndex = 0;
            submitted = false;
            startedAtMillis = System.currentTimeMillis();

            quizTitleLabel.setText(quiz.getTitle());
            quizMetaLabel.setText("Total: " + questions.size() + " Questions  |  "
                    + quiz.getTotalMarks() + " Marks  |  Duration: " + quiz.getDurationMinutes() + " mins");
            timerLabel.setText("⏱ " + QuizTimer.format(quiz.getDurationMinutes() * 60));
            submitButton.setEnabled(true);

            buildPaletteButtons();
            renderQuestion();

            // Dedicated background countdown thread
            timer = new QuizTimer(quiz.getDurationMinutes() * 60, timerListener);
            timer.start();

        } catch (DatabaseException e) {
            UIUtil.error(this, "Could not load quiz questions:\n" + e.getMessage());
        }
    }

    /** Builds the interactive Question Palette buttons grid. */
    private void buildPaletteButtons() {
        palettePanel.removeAll();
        paletteButtons.clear();

        int cols = 4;
        int rows = (int) Math.ceil((double) questions.size() / cols);
        palettePanel.setLayout(new GridLayout(Math.max(rows, 1), cols, 6, 6));

        for (int i = 0; i < questions.size(); i++) {
            final int index = i;
            JButton btn = new JButton(String.valueOf(i + 1));
            btn.setFont(UIUtil.smallBoldFont());
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(38, 34));
            btn.addActionListener(e -> jumpToQuestion(index));
            paletteButtons.add(btn);
            palettePanel.add(btn);
        }
        palettePanel.revalidate();
        palettePanel.repaint();
    }

    /** Updates the palette button colors based on current & answered states. */
    private void refreshPaletteState() {
        for (int i = 0; i < questions.size(); i++) {
            JButton btn = paletteButtons.get(i);
            Question q = questions.get(i);
            boolean isAnswered = selectedAnswers.containsKey(q.getId());
            boolean isCurrent = (i == currentIndex);

            if (isCurrent) {
                btn.setBackground(UIUtil.ACCENT);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(UIUtil.PRIMARY, 2));
            } else if (isAnswered) {
                btn.setBackground(UIUtil.SUCCESS);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(UIUtil.SUCCESS, 1));
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(UIUtil.TEXT);
                btn.setBorder(BorderFactory.createLineBorder(UIUtil.BORDER, 1));
            }
        }
    }

    private void jumpToQuestion(int target) {
        if (submitted || target < 0 || target >= questions.size()) {
            return;
        }
        rememberSelection();
        currentIndex = target;
        renderQuestion();
    }

    /** Rebuilds the current question display card and option buttons. */
    private void renderQuestion() {
        Question question = questions.get(currentIndex);

        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)));

        // Question header: Number + Marks badge
        JPanel qTop = new JPanel(new BorderLayout());
        qTop.setBackground(Color.WHITE);

        JLabel qNumberLabel = new JLabel("Question " + (currentIndex + 1) + " of " + questions.size());
        qNumberLabel.setFont(UIUtil.subTitleFont());
        qNumberLabel.setForeground(UIUtil.TEXT);
        qTop.add(qNumberLabel, BorderLayout.WEST);

        JLabel marksBadge = UIUtil.badge(question.getMarks() + " Mark" + (question.getMarks() > 1 ? "s" : ""),
                new Color(0xDB, 0xEA, 0xFE), UIUtil.ACCENT);
        qTop.add(marksBadge, BorderLayout.EAST);
        card.add(qTop, BorderLayout.NORTH);

        // Center: Question text & Options
        JPanel qCenter = new JPanel();
        qCenter.setLayout(new BoxLayout(qCenter, BoxLayout.Y_AXIS));
        qCenter.setBackground(Color.WHITE);

        JLabel qText = new JLabel("<html><body style='width:680px; font-family: Segoe UI; font-size: 14pt; line-height: 1.4;'>"
                + question.getText() + "</body></html>");
        qText.setFont(UIUtil.normalFont());
        qText.setForeground(UIUtil.TEXT);
        qCenter.add(qText);
        qCenter.add(Box.createVerticalStrut(18));

        // Options: Styled card-like rows
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(Color.WHITE);
        currentOptionGroup = new ButtonGroup();

        long selected = selectedAnswers.getOrDefault(question.getId(), -1L);

        for (Option option : question.getOptions()) {
            JPanel optionRow = createOptionCard(option, option.getId() == selected);
            optionsPanel.add(optionRow);
            optionsPanel.add(Box.createVerticalStrut(8));
        }

        qCenter.add(optionsPanel);
        card.add(qCenter, BorderLayout.CENTER);

        questionContent.removeAll();
        questionContent.add(new JScrollPane(card), BorderLayout.CENTER);
        questionContent.revalidate();
        questionContent.repaint();

        prevButton.setEnabled(currentIndex > 0);
        nextButton.setEnabled(currentIndex < questions.size() - 1);
        refreshPaletteState();
    }

    /** Creates a card-style option row with visual selection feedback. */
    private JPanel createOptionCard(Option option, boolean isSelected) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JRadioButton radio = new JRadioButton();
        radio.setOpaque(false);
        radio.setFocusPainted(false);
        radio.putClientProperty("optionId", option.getId());

        JLabel textLabel = new JLabel("<html><b>(" + option.getLetter() + ")</b> &nbsp; "
                + option.getOptionText() + "</html>");
        textLabel.setFont(UIUtil.normalFont());

        row.add(radio, BorderLayout.WEST);
        row.add(textLabel, BorderLayout.CENTER);

        // Apply distinct styling based on selection
        applyOptionCardStyle(row, textLabel, isSelected);

        if (isSelected) {
            radio.setSelected(true);
        }
        currentOptionGroup.add(radio);

        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!submitted) {
                    radio.setSelected(true);
                    updateAllOptionStyles();
                    rememberSelection();
                    refreshPaletteState();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!submitted && !radio.isSelected()) {
                    row.setBackground(new Color(0xF8, 0xFA, 0xFC));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!submitted && !radio.isSelected()) {
                    row.setBackground(Color.WHITE);
                }
            }
        };

        row.addMouseListener(clickHandler);
        radio.addActionListener(e -> {
            updateAllOptionStyles();
            rememberSelection();
            refreshPaletteState();
        });

        return row;
    }

    private void applyOptionCardStyle(JPanel row, JLabel textLabel, boolean isSelected) {
        if (isSelected) {
            row.setBackground(new Color(0xEF, 0xF6, 0xFF));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIUtil.ACCENT, 2),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            textLabel.setForeground(UIUtil.PRIMARY);
        } else {
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            textLabel.setForeground(UIUtil.TEXT);
        }
    }

    private void updateAllOptionStyles() {
        if (optionsPanel == null) {
            return;
        }
        for (Component c : optionsPanel.getComponents()) {
            if (c instanceof JPanel card) {
                JRadioButton radio = null;
                JLabel label = null;
                for (Component sub : card.getComponents()) {
                    if (sub instanceof JRadioButton r) radio = r;
                    if (sub instanceof JLabel l) label = l;
                }
                if (radio != null && label != null) {
                    applyOptionCardStyle(card, label, radio.isSelected());
                }
            }
        }
    }

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

    private void rememberSelection() {
        if (optionsPanel == null || currentQuiz == null || currentIndex >= questions.size()) {
            return;
        }
        Question question = questions.get(currentIndex);
        long chosen = -1L;

        for (Component c : optionsPanel.getComponents()) {
            if (c instanceof JPanel card) {
                for (Component sub : card.getComponents()) {
                    if (sub instanceof JRadioButton radio && radio.isSelected()) {
                        chosen = (Long) radio.getClientProperty("optionId");
                    }
                }
            }
        }

        if (chosen > 0) {
            selectedAnswers.put(question.getId(), chosen);
        } else {
            selectedAnswers.remove(question.getId());
        }
    }

    /**
     * Submits the quiz attempt transactionally.
     * Prevents duplicate submissions and asks confirmation before manual submission.
     */
    private void submitQuiz(boolean timedOut) {
        if (submitted || currentQuiz == null) {
            return;
        }

        if (!timedOut) {
            rememberSelection();
            int total = questions.size();
            int answered = selectedAnswers.size();
            int unanswered = total - answered;

            String confirmMsg = "Quiz Submission Summary:\n"
                    + "• Total Questions: " + total + "\n"
                    + "• Answered: " + answered + "\n"
                    + "• Unanswered: " + unanswered + "\n\n"
                    + (unanswered > 0 ? "You have " + unanswered + " unanswered question(s).\n" : "")
                    + "Are you sure you want to finish and submit?";

            if (!UIUtil.confirm(this, confirmMsg)) {
                return;
            }
        }

        submitted = true;
        submitButton.setEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        if (timer != null) {
            timer.stop();
        }

        List<Answer> answers = new ArrayList<>();
        for (Question question : questions) {
            answers.add(new Answer(question.getId(), selectedAnswers.getOrDefault(question.getId(), -1L)));
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
            UIUtil.info(this, e.getMessage());
        } catch (InvalidQuizException | DatabaseException e) {
            UIUtil.error(this, "Submission failed:\n" + e.getMessage());
            submitted = false;
            submitButton.setEnabled(true);
            prevButton.setEnabled(currentIndex > 0);
            nextButton.setEnabled(currentIndex < questions.size() - 1);
        }
    }
}