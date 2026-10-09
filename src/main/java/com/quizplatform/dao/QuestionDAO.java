package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Question;

import java.util.List;

/**
 * DAO pattern: interface for all question table operations.
 * Implemented by com.quizplatform.dao.impl.QuestionDAOImpl (JDBC).
 */
public interface QuestionDAO {

    int addQuestion(Question question) throws DatabaseException;

    Question getQuestionById(int questionId) throws DatabaseException;

    List<Question> getQuestionsByQuiz(int quizId) throws DatabaseException;

    void updateQuestion(Question question) throws DatabaseException;

    void deleteQuestion(int questionId) throws DatabaseException;

    void deleteQuestionsByQuiz(int quizId) throws DatabaseException;

    int countQuestionsByQuiz(int quizId) throws DatabaseException;
}
