package com.quizplatform.dao.impl;

import com.quizplatform.dao.AttemptDAO;
import com.quizplatform.dao.ResultDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.AttemptStatus;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of AttemptDAO.
 *
 * MOST IMPORTANT METHOD: submitAttempt() demonstrates JDBC TRANSACTION
 * MANAGEMENT - attempt + answers + result are inserted on ONE connection
 * and either all are committed or all are rolled back.
 */
public class AttemptDAOImpl implements AttemptDAO {

    private final ResultDAO resultDAO;

    public AttemptDAOImpl() {
        this(new ResultDAOImpl());
    }

    /** Constructor injection makes the DAO easy to replace in tests. */
    public AttemptDAOImpl(ResultDAO resultDAO) {
        this.resultDAO = resultDAO;
    }

    // ------------------------------------------------------------------
    // Demonstrates TRANSACTION MANAGEMENT (commit / rollback)
    // ------------------------------------------------------------------
    @Override
    public Result submitAttempt(QuizAttempt attempt, List<Answer> answers, Result result) throws DatabaseException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);              // 1) begin transaction

            int attemptId = insertAttempt(conn, attempt);
            attempt.setId(attemptId);

            insertAnswers(conn, attemptId, answers); // 2) participant answers

            result.setAttemptId(attemptId);
            resultDAO.saveResult(conn, result);      // 3) result on the SAME connection

            conn.commit();                           // 4) commit everything together
            return result;

        } catch (SQLException | DatabaseException e) {
            // 5) anything went wrong -> roll the whole submission back
            SQLException rollbackError = null;
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    rollbackError = re;
                }
            }
            DatabaseException failure = (e instanceof DatabaseException de)
                    ? de
                    : new DatabaseException("Quiz submission failed and was rolled back: " + e.getMessage(), e);
            if (rollbackError != null) {
                failure.addSuppressed(rollbackError);   // never swallow silently
            }
            throw failure;
        } finally {
            if (conn != null) {
                DatabaseConnection.close(conn);        // try-with-resources style cleanup
            }
        }
    }

    private int insertAttempt(Connection conn, QuizAttempt attempt) throws SQLException {
        String sql = "INSERT INTO quiz_attempts (quiz_id, participant_id, started_at, submitted_at, "
                + "time_taken_seconds, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attempt.getQuizId());
            ps.setInt(2, attempt.getParticipantId());
            ps.setTimestamp(3, Timestamp.valueOf(attempt.getStartedAt()));
            ps.setTimestamp(4, attempt.getSubmittedAt() == null ? null : Timestamp.valueOf(attempt.getSubmittedAt()));
            ps.setLong(5, attempt.getTimeTakenSeconds());
            ps.setString(6, attempt.getStatus() == null ? AttemptStatus.COMPLETED.name() : attempt.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Attempt inserted but no generated key returned.");
        }
    }

    /** Demonstrates JDBC BATCH UPDATE - all answers of one attempt in one round-trip. */
    private void insertAnswers(Connection conn, int attemptId, List<Answer> answers) throws SQLException {
        String sql = "INSERT INTO answers (attempt_id, question_id, selected_option_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Answer answer : answers) {
                ps.setInt(1, attemptId);
                ps.setInt(2, answer.getQuestionId());
                if (answer.getSelectedOptionId() > 0) {
                    ps.setLong(3, answer.getSelectedOptionId());
                } else {
                    ps.setNull(3, Types.BIGINT);      // question was not answered
                }
                answer.setAttemptId(attemptId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private QuizAttempt mapAttempt(ResultSet rs) throws SQLException {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(rs.getInt("attempt_id"));
        attempt.setQuizId(rs.getInt("quiz_id"));
        attempt.setParticipantId(rs.getInt("participant_id"));
        Timestamp started = rs.getTimestamp("started_at");
        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (started != null) {
            attempt.setStartedAt(started.toLocalDateTime());
        }
        if (submitted != null) {
            attempt.setSubmittedAt(submitted.toLocalDateTime());
        }
        attempt.setTimeTakenSeconds(rs.getLong("time_taken_seconds"));
        attempt.setStatus(AttemptStatus.fromString(rs.getString("status")));
        return attempt;
    }

    @Override
    public QuizAttempt getAttemptById(int attemptId) throws DatabaseException {
        String sql = "SELECT attempt_id, quiz_id, participant_id, started_at, submitted_at, "
                + "time_taken_seconds, status FROM quiz_attempts WHERE attempt_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAttempt(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load attempt #" + attemptId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<QuizAttempt> getAttemptsByParticipant(int participantId) throws DatabaseException {
        String sql = "SELECT attempt_id, quiz_id, participant_id, started_at, submitted_at, "
                + "time_taken_seconds, status FROM quiz_attempts WHERE participant_id = ? ORDER BY attempt_id DESC";
        List<QuizAttempt> attempts = new ArrayList<>();     // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapAttempt(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load attempts: " + e.getMessage(), e);
        }
        return attempts;
    }

    @Override
    public List<QuizAttempt> getAttemptsByQuiz(int quizId) throws DatabaseException {
        String sql = "SELECT attempt_id, quiz_id, participant_id, started_at, submitted_at, "
                + "time_taken_seconds, status FROM quiz_attempts WHERE quiz_id = ? ORDER BY attempt_id DESC";
        List<QuizAttempt> attempts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapAttempt(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load attempts: " + e.getMessage(), e);
        }
        return attempts;
    }

    @Override
    public List<Answer> getAnswersByAttempt(int attemptId) throws DatabaseException {
        String sql = "SELECT answer_id, attempt_id, question_id, selected_option_id FROM answers "
                + "WHERE attempt_id = ? ORDER BY answer_id";
        List<Answer> answers = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Answer answer = new Answer();
                    answer.setId(rs.getInt("answer_id"));
                    answer.setAttemptId(rs.getInt("attempt_id"));
                    answer.setQuestionId(rs.getInt("question_id"));
                    long selected = rs.getLong("selected_option_id");
                    answer.setSelectedOptionId(rs.wasNull() ? -1 : selected);
                    answers.add(answer);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load answers: " + e.getMessage(), e);
        }
        return answers;
    }

    @Override
    public int countAllAttempts() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM quiz_attempts";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count attempts: " + e.getMessage(), e);
        }
    }
}
