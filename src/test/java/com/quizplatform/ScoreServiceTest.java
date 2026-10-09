package com.quizplatform;

import com.quizplatform.interfaces.ResultOperations;
import com.quizplatform.model.Answer;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.Result;
import com.quizplatform.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for the score calculation (no database required).
 * Demonstrates: JUnit 5, Collections, interfaces and the ScoreService logic.
 */
class ScoreServiceTest {

    private ScoreService scoreService;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService();
        quiz = new Quiz("Java OOP Test", "Test quiz", 10);
        quiz.setId(1);
    }

    /** Builds one MCQ with a known correct option. */
    private Question question(int id, int marks, long correctOptionId) {
        Question question = new Question("Question " + id, marks, "explanation " + id);
        question.setId(id);
        question.setQuizId(1);
        Option a = new Option("Option A", 1, correctOptionId == id * 10 + 1);
        a.setId(id * 10 + 1);
        Option b = new Option("Option B", 2, correctOptionId == id * 10 + 2);
        b.setId(id * 10 + 2);
        question.addOption(a);
        question.addOption(b);
        return question;
    }

    @Test
    @DisplayName("calculateScore() counts correct / incorrect / unanswered answers")
    void calculateScoreCountsAnswers() {
        Question q1 = question(1, 2, 11);   // correct = 11
        Question q2 = question(2, 1, 22);   // correct = 22
        Question q3 = question(3, 1, 32);   // correct = 32

        QuizAttempt attempt = new QuizAttempt(1, 5);
        attempt.setParticipantName("Test Student");
        attempt.setTimeTakenSeconds(120);
        attempt.addAnswer(new Answer(1, 11));   // correct (2 marks)
        attempt.addAnswer(new Answer(2, 21));   // wrong   (0 marks)
        attempt.addAnswer(new Answer(3, -1));   // not answered

        // Demonstrates interface usage: ResultOperations reference
        ResultOperations service = scoreService;
        Result result = service.calculateScore(quiz, List.of(q1, q2, q3), attempt);

        assertEquals(3, result.getTotalQuestions());
        assertEquals(2, result.getAttemptedQuestions());
        assertEquals(1, result.getCorrectAnswers());
        assertEquals(1, result.getIncorrectAnswers());
        assertEquals(4, result.getTotalMarks());
        assertEquals(2, result.getObtainedMarks());
        assertEquals(50.0, result.getPercentage(), 0.001);
        assertTrue(result.getFeedback().contains("Average"), "feedback should reflect 50%");
    }

    @Test
    @DisplayName("All answers correct -> 100% and 'Excellent' feedback")
    void allCorrectGivesFullScore() {
        Question q1 = question(1, 1, 11);
        Question q2 = question(2, 1, 22);

        QuizAttempt attempt = new QuizAttempt(1, 5);
        attempt.addAnswer(new Answer(1, 11));
        attempt.addAnswer(new Answer(2, 22));

        Result result = scoreService.calculateScore(quiz, List.of(q1, q2), attempt);

        assertEquals(100.0, result.getPercentage(), 0.001);
        assertEquals(2, result.getCorrectAnswers());
        assertEquals(0, result.getIncorrectAnswers());
        assertTrue(result.getFeedback().contains("Excellent"));
    }

    @Test
    @DisplayName("generateReport() contains participant, quiz, score and feedback")
    void generateReportContainsAllSections() {
        Result result = new Result();
        result.setParticipantName("Test Student");
        result.setQuizTitle("Java OOP Test");
        result.setTotalQuestions(10);
        result.setAttemptedQuestions(8);
        result.setCorrectAnswers(6);
        result.setIncorrectAnswers(2);
        result.setTotalMarks(10);
        result.setObtainedMarks(6);
        result.setPercentage(60.0);
        result.setTimeTakenSeconds(300);
        result.setFeedback("Good work!");

        String report = scoreService.generateReport(result);

        assertTrue(report.contains("PERFORMANCE REPORT"));
        assertTrue(report.contains("Test Student"));
        assertTrue(report.contains("Java OOP Test"));
        assertTrue(report.contains("6/10"));
        assertTrue(report.contains("60.0%"));
        assertTrue(report.contains("05:00"));
        assertTrue(report.contains("Good work!"));
    }
}
