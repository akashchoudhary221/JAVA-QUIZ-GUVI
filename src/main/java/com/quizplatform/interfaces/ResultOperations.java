package com.quizplatform.interfaces;

import com.quizplatform.model.Quiz;
import com.quizplatform.model.Question;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.Result;

import java.util.List;

/**
 * Demonstrates interface-based programming.
 * Implemented by ScoreService - defines everything related to
 * score calculation and performance reporting.
 */
public interface ResultOperations {

    /** Calculates correct/incorrect answers, marks, percentage and feedback. */
    Result calculateScore(Quiz quiz, List<Question> questions, QuizAttempt attempt);

    /** Builds a human readable performance report for one result. */
    String generateReport(Result result);
}
