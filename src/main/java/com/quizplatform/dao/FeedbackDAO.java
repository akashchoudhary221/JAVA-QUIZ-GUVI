package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;

import java.util.List;

/**
 * DAO pattern: interface for feedback (comments given by creators/admins).
 * Implemented by com.quizplatform.dao.impl.FeedbackDAOImpl (JDBC).
 */
public interface FeedbackDAO {

    int addFeedback(Feedback feedback) throws DatabaseException;

    List<Feedback> getFeedbackByResult(int resultId) throws DatabaseException;

    List<Feedback> getFeedbackByParticipant(int participantId) throws DatabaseException;

    List<Feedback> getFeedbackByQuiz(int quizId) throws DatabaseException;

    /** Feedback written by a specific creator (creator dashboard). */
    List<Feedback> getFeedbackByAuthor(int authorId) throws DatabaseException;
}
