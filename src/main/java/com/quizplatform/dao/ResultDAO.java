package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Result;

import java.sql.Connection;
import java.util.List;

/**
 * DAO pattern: interface for the results table.
 * Implemented by com.quizplatform.dao.impl.ResultDAOImpl (JDBC).
 *
 * Two saveResult() variants:
 *  - saveResult(Result)                 -> opens its own connection
 *  - saveResult(Connection, Result)     -> joins the caller's TRANSACTION
 */
public interface ResultDAO {

    int saveResult(Result result) throws DatabaseException;

    /** Uses the caller's connection so the insert participates in a transaction. */
    void saveResult(Connection connection, Result result) throws DatabaseException;

    Result getResultById(int resultId) throws DatabaseException;

    Result getResultByAttempt(int attemptId) throws DatabaseException;

    List<Result> getResultsByParticipant(int participantId) throws DatabaseException;

    List<Result> getResultsByQuiz(int quizId) throws DatabaseException;

    /** Results of all quizzes created by one quiz creator (creator dashboard). */
    List<Result> getResultsByCreator(int creatorId) throws DatabaseException;

    List<Result> getAllResults() throws DatabaseException;
}
