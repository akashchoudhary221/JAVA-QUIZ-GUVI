package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Quiz;

import java.util.List;

/**
 * DAO pattern: interface for all quiz table operations.
 * Implemented by com.quizplatform.dao.impl.QuizDAOImpl (JDBC).
 */
public interface QuizDAO {

    int createQuiz(Quiz quiz) throws DatabaseException;

    Quiz getQuizById(int quizId) throws DatabaseException;

    List<Quiz> getAllQuizzes() throws DatabaseException;

    /** Only APPROVED/PUBLISHED quizzes are visible to participants. */
    List<Quiz> getApprovedQuizzes() throws DatabaseException;

    List<Quiz> getQuizzesByCreator(int creatorId) throws DatabaseException;

    List<Quiz> getQuizzesByStatus(String status) throws DatabaseException;

    void updateQuiz(Quiz quiz) throws DatabaseException;

    void deleteQuiz(int quizId) throws DatabaseException;

    /** Status transitions used by the admin approval workflow. */
    void updateQuizStatus(int quizId, String status) throws DatabaseException;

    int countQuizzesByStatus(String status) throws DatabaseException;

    int countAllQuizzes() throws DatabaseException;
}
