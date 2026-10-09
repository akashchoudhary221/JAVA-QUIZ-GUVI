package com.quizplatform.service;

import com.quizplatform.interfaces.ResultOperations;
import com.quizplatform.model.Answer;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.Result;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates scores and generates performance reports.
 * Implements the ResultOperations interface.
 *
 * Example output:
 *   Total Questions: 10 | Correct: 8 | Incorrect: 2
 *   Score: 8/10  ->  80.00%
 */
public class ScoreService implements ResultOperations {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Demonstrates INTERFACE IMPLEMENTATION
    @Override
    public Result calculateScore(Quiz quiz, List<Question> questions, QuizAttempt attempt) {
        Result result = new Result();
        result.setQuizId(quiz.getId());
        result.setQuizTitle(quiz.getTitle());
        result.setParticipantId(attempt.getParticipantId());
        result.setParticipantName(attempt.getParticipantName());
        result.setTotalQuestions(questions.size());
        result.setTimeTakenSeconds(attempt.getTimeTakenSeconds());
        result.setSubmittedAt(attempt.getSubmittedAt());

        // Demonstrates Java Collections: Map<Integer, Question> for O(1) lookup
        Map<Integer, Question> questionMap = new HashMap<>();
        int totalMarks = 0;
        for (Question question : questions) {
            questionMap.put(question.getId(), question);
            totalMarks += question.getMarks();
        }

        int correct = 0;
        int incorrect = 0;
        int attempted = 0;
        int obtainedMarks = 0;

        for (Answer answer : attempt.getAnswers()) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null || !answer.isAttempted()) {
                continue;   // unanswered questions are counted separately
            }
            attempted++;
            if (answer.getSelectedOptionId() == question.getCorrectOptionId()) {
                correct++;
                obtainedMarks += question.getMarks();
            } else {
                incorrect++;
            }
        }

        double percentage = totalMarks == 0
                ? 0.0
                : Math.round((obtainedMarks * 100.0 / totalMarks) * 100.0) / 100.0;

        result.setAttemptedQuestions(attempted);
        result.setCorrectAnswers(correct);
        result.setIncorrectAnswers(incorrect);
        result.setTotalMarks(totalMarks);
        result.setObtainedMarks(obtainedMarks);
        result.setPercentage(percentage);
        result.setFeedback(buildFeedback(percentage, correct, questions.size()));
        return result;
    }

    /** Automatic feedback based on the achieved percentage. */
    private String buildFeedback(double percentage, int correct, int total) {
        if (percentage >= 80) {
            return "Excellent! You have a strong command of this topic (" + correct + "/" + total + " correct).";
        } else if (percentage >= 60) {
            return "Good work! Revise the questions you missed to reach the top band (" + correct + "/" + total + " correct).";
        } else if (percentage >= 40) {
            return "Average performance. Focus on the concepts behind the incorrect answers (" + correct + "/" + total + " correct).";
        }
        return "Needs improvement. Please review the full topic before the next attempt (" + correct + "/" + total + " correct).";
    }

    // Demonstrates INTERFACE IMPLEMENTATION
    @Override
    public String generateReport(Result result) {
        if (result == null) {
            return "No result available.";
        }
        String submitted = result.getSubmittedAt() == null ? "-" : DATE_TIME.format(result.getSubmittedAt());
        return String.join("\n",
                "================ PERFORMANCE REPORT ================",
                "Participant : " + result.getParticipantName(),
                "Quiz        : " + result.getQuizTitle(),
                "Date        : " + submitted,
                "-----------------------------------------------------",
                "Total Questions     : " + result.getTotalQuestions(),
                "Attempted Questions : " + result.getAttemptedQuestions(),
                "Correct Answers     : " + result.getCorrectAnswers(),
                "Incorrect Answers   : " + result.getIncorrectAnswers(),
                "Score               : " + result.getObtainedMarks() + "/" + result.getTotalMarks(),
                "Percentage          : " + result.getPercentage() + "%",
                "Time Taken          : " + result.getFormattedTimeTaken(),
                "-----------------------------------------------------",
                "Feedback    : " + result.getFeedback(),
                "=====================================================");
    }

    /** Debug helper used by the result panel: correct option of a question. */
    public String correctAnswerText(Question question) {
        Option correct = question.getCorrectOption();
        return correct == null ? "-" : correct.getLetter() + ") " + correct.getOptionText();
    }
}
