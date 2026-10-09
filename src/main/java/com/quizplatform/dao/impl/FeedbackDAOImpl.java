package com.quizplatform.dao.impl;

import com.quizplatform.dao.FeedbackDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of FeedbackDAO.
 */
public class FeedbackDAOImpl implements FeedbackDAO {

    private static final String BASE_SELECT =
            "SELECT f.feedback_id, f.result_id, f.quiz_id, f.participant_id, f.commented_by, "
                    + "f.comment_text, f.created_at, u.name AS commented_by_name, "
                    + "p.name AS participant_name, q.title AS quiz_title "
                    + "FROM feedback f "
                    + "JOIN users u ON f.commented_by = u.user_id "
                    + "JOIN users p ON f.participant_id = p.user_id "
                    + "JOIN quizzes q ON f.quiz_id = q.quiz_id";

    private Feedback mapRow(ResultSet rs) throws SQLException {
        Feedback feedback = new Feedback();
        feedback.setId(rs.getInt("feedback_id"));
        feedback.setResultId(rs.getInt("result_id"));
        feedback.setQuizId(rs.getInt("quiz_id"));
        feedback.setParticipantId(rs.getInt("participant_id"));
        feedback.setCommentedBy(rs.getInt("commented_by"));
        feedback.setComment(rs.getString("comment_text"));
        feedback.setCommentedByName(rs.getString("commented_by_name"));
        feedback.setParticipantName(rs.getString("participant_name"));
        feedback.setQuizTitle(rs.getString("quiz_title"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            feedback.setCreatedAt(created.toLocalDateTime());
        }
        return feedback;
    }

    private List<Feedback> queryList(String sql, Object... params) throws DatabaseException {
        List<Feedback> list = new ArrayList<>();      // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load feedback: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int addFeedback(Feedback feedback) throws DatabaseException {
        String sql = "INSERT INTO feedback (result_id, quiz_id, participant_id, commented_by, comment_text) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, feedback.getResultId());
            ps.setInt(2, feedback.getQuizId());
            ps.setInt(3, feedback.getParticipantId());
            ps.setInt(4, feedback.getCommentedBy());
            ps.setString(5, feedback.getComment());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    feedback.setId(id);
                    return id;
                }
            }
            throw new DatabaseException("Feedback inserted but no generated id returned.");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Feedback> getFeedbackByResult(int resultId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE f.result_id = ? ORDER BY f.created_at DESC", resultId);
    }

    @Override
    public List<Feedback> getFeedbackByParticipant(int participantId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE f.participant_id = ? ORDER BY f.created_at DESC", participantId);
    }

    @Override
    public List<Feedback> getFeedbackByQuiz(int quizId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE f.quiz_id = ? ORDER BY f.created_at DESC", quizId);
    }

    @Override
    public List<Feedback> getFeedbackByAuthor(int authorId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE f.commented_by = ? ORDER BY f.created_at DESC", authorId);
    }
}
