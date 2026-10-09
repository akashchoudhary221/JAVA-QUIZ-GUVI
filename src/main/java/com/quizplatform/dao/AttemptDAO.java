package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.Result;

import java.util.List;

/**
 * DAO pattern: interface for quiz attempts and their answers.
 * Implemented by com.quizplatform.dao.impl.AttemptDAOImpl (JDBC).
 *
 * submitAttempt() performs a real JDBC TRANSACTION:
 * attempt + answers + result are committed together or rolled back together.
 */
public interface AttemptDAO {

    /** Stores attempt + all answers + the calculated result in ONE transaction. */
    Result submitAttempt(QuizAttempt attempt, List<Answer> answers, Result result) throws DatabaseException;

    QuizAttempt getAttemptById(int attemptId) throws DatabaseException;

    List<QuizAttempt> getAttemptsByParticipant(int participantId) throws DatabaseException;

    List<QuizAttempt> getAttemptsByQuiz(int quizId) throws DatabaseException;

    List<Answer> getAnswersByAttempt(int attemptId) throws DatabaseException;

    int countAllAttempts() throws DatabaseException;
}
