package com.quizplatform.interfaces;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.model.Quiz;

/**
 * Demonstrates interface-based programming.
 * Implemented by QuizService - defines the quiz lifecycle operations
 * available to the Quiz Creator role (and Admin for approval).
 */
public interface QuizOperations {

    Quiz createQuiz(Quiz quiz) throws InvalidQuizException, DatabaseException;

    Quiz updateQuiz(Quiz quiz) throws InvalidQuizException, DatabaseException;

    void deleteQuiz(int quizId) throws DatabaseException;
}
