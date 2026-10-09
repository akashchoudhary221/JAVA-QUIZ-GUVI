package com.quizplatform.dao.impl;

import com.quizplatform.dao.ResultDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of ResultDAO.
 *
 * Two saveResult() variants demonstrate how a DAO can either open its
 * own connection OR join an existing transaction (method overloading).
 */
public class ResultDAOImpl implements ResultDAO {

    private static final String BASE_SELECT =
            "SELECT r.result_id, r.attempt_id, r.quiz_id, r.participant_id, r.total_questions, "
                    + "r.attempted_questions, r.correct_answers, r.incorrect_answers, r.total_marks, "
                    + "r.obtained_marks, r.percentage, r.time_taken_seconds, r.feedback, r.submitted_at, "
                    + "q.title AS quiz_title, u.name AS participant_name "
                    + "FROM results r "
                    + "JOIN quizzes q ON r.quiz_id = q.quiz_id "
                    + "JOIN users u ON r.participant_id = u.user_id";

    private Result mapRow(ResultSet rs) throws SQLException {
        Result result = new Result();
        result.setId(rs.getInt("result_id"));
        result.setAttemptId(rs.getInt("attempt_id"));
        result.setQuizId(rs.getInt("quiz_id"));
        result.setParticipantId(rs.getInt("participant_id"));
        result.setTotalQuestions(rs.getInt("total_questions"));
        result.setAttemptedQuestions(rs.getInt("attempted_questions"));
        result.setCorrectAnswers(rs.getInt("correct_answers"));
        result.setIncorrectAnswers(rs.getInt("incorrect_answers"));
        result.setTotalMarks(rs.getInt("total_marks"));
        result.setObtainedMarks(rs.getInt("obtained_marks"));
        result.setPercentage(rs.getDouble("percentage"));
        result.setTimeTakenSeconds(rs.getLong("time_taken_seconds"));
        result.setFeedback(rs.getString("feedback"));
        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) {
            result.setSubmittedAt(submitted.toLocalDateTime());
        }
        result.setQuizTitle(rs.getString("quiz_title"));
        result.setParticipantName(rs.getString("participant_name"));
        return result;
    }

    /** Stand-alone save - opens (and closes) its own connection. */
    @Override
    public int saveResult(Result result) throws DatabaseException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            saveResult(conn, result);
            return result.getId();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save result: " + e.getMessage(), e);
        }
    }

    /**
     * Transaction-friendly save: the caller controls commit/rollback.
     * Demonstrates how one method can participate in a larger JDBC transaction.
     */
    @Override
    public void saveResult(Connection conn, Result result) throws DatabaseException {
        String sql = "INSERT INTO results (attempt_id, quiz_id, participant_id, total_questions, "
                + "attempted_questions, correct_answers, incorrect_answers, total_marks, obtained_marks, "
                + "percentage, time_taken_seconds, feedback, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, result.getAttemptId());
            ps.setInt(2, result.getQuizId());
            ps.setInt(3, result.getParticipantId());
            ps.setInt(4, result.getTotalQuestions());
            ps.setInt(5, result.getAttemptedQuestions());
            ps.setInt(6, result.getCorrectAnswers());
            ps.setInt(7, result.getIncorrectAnswers());
            ps.setInt(8, result.getTotalMarks());
            ps.setInt(9, result.getObtainedMarks());
            ps.setDouble(10, result.getPercentage());
            ps.setLong(11, result.getTimeTakenSeconds());
            ps.setString(12, result.getFeedback());
            ps.setTimestamp(13, result.getSubmittedAt() == null ? null : Timestamp.valueOf(result.getSubmittedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    result.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save result: " + e.getMessage(), e);
        }
    }

    /** Generic select helper: 0..n parameters, rows mapped to Result objects. */
    private List<Result> queryList(String sql, Object... params) throws DatabaseException {
        List<Result> results = new ArrayList<>();      // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load results: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public Result getResultById(int resultId) throws DatabaseException {
        List<Result> list = queryList(BASE_SELECT + " WHERE r.result_id = ?", resultId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Result getResultByAttempt(int attemptId) throws DatabaseException {
        List<Result> list = queryList(BASE_SELECT + " WHERE r.attempt_id = ?", attemptId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Result> getResultsByParticipant(int participantId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE r.participant_id = ? ORDER BY r.result_id DESC", participantId);
    }

    @Override
    public List<Result> getResultsByQuiz(int quizId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE r.quiz_id = ? ORDER BY r.result_id DESC", quizId);
    }

    @Override
    public List<Result> getResultsByCreator(int creatorId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE q.created_by = ? ORDER BY r.result_id DESC", creatorId);
    }

    @Override
    public List<Result> getAllResults() throws DatabaseException {
        return queryList(BASE_SELECT + " ORDER BY r.result_id DESC");
    }
}
