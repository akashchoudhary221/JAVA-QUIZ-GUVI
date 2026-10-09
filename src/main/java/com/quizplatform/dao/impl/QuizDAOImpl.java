package com.quizplatform.dao.impl;

import com.quizplatform.dao.QuizDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of the QuizDAO interface.
 * Demonstrates PreparedStatement, JOINs, ResultSet mapping and
 * try-with-resources for every CRUD operation.
 */
public class QuizDAOImpl implements QuizDAO {

    private static final String BASE_SELECT =
            "SELECT q.quiz_id, q.title, q.description, q.duration_minutes, q.total_marks, "
                    + "q.status, q.created_by, q.created_at, u.name AS creator_name "
                    + "FROM quizzes q JOIN users u ON q.created_by = u.user_id";

    private Quiz mapRow(ResultSet rs) throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setId(rs.getInt("quiz_id"));
        quiz.setTitle(rs.getString("title"));
        quiz.setDescription(rs.getString("description"));
        quiz.setDurationMinutes(rs.getInt("duration_minutes"));
        quiz.setTotalMarks(rs.getInt("total_marks"));
        quiz.setStatus(QuizStatus.fromString(rs.getString("status")));
        quiz.setCreatedBy(rs.getInt("created_by"));
        quiz.setCreatorName(rs.getString("creator_name"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            quiz.setCreatedAt(created.toLocalDateTime());
        }
        return quiz;
    }

    /**
     * Generic select helper: runs a query with 0..n parameters and maps
     * every row to a Quiz object. Demonstrates varargs + JDBC.
     */
    private List<Quiz> queryList(String sql, Object... params) throws DatabaseException {
        List<Quiz> quizzes = new ArrayList<>();      // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quizzes.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load quizzes: " + e.getMessage(), e);
        }
        return quizzes;
    }

    @Override
    public int createQuiz(Quiz quiz) throws DatabaseException {
        String sql = "INSERT INTO quizzes (title, description, duration_minutes, total_marks, status, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, quiz.getTitle());
            ps.setString(2, quiz.getDescription());
            ps.setInt(3, quiz.getDurationMinutes());
            ps.setInt(4, quiz.getTotalMarks());
            ps.setString(5, quiz.getStatus().name());
            ps.setInt(6, quiz.getCreatedBy());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    quiz.setId(id);
                    return id;
                }
            }
            throw new DatabaseException("Quiz inserted but no generated id returned.");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create quiz: " + e.getMessage(), e);
        }
    }

    @Override
    public Quiz getQuizById(int quizId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE q.quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load quiz #" + quizId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Quiz> getAllQuizzes() throws DatabaseException {
        return queryList(BASE_SELECT + " ORDER BY q.quiz_id");
    }

    @Override
    public List<Quiz> getApprovedQuizzes() throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE q.status IN ('APPROVED', 'PUBLISHED') ORDER BY q.quiz_id");
    }

    @Override
    public List<Quiz> getQuizzesByCreator(int creatorId) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE q.created_by = ? ORDER BY q.quiz_id", creatorId);
    }

    @Override
    public List<Quiz> getQuizzesByStatus(String status) throws DatabaseException {
        return queryList(BASE_SELECT + " WHERE q.status = ? ORDER BY q.quiz_id", status);
    }

    @Override
    public void updateQuiz(Quiz quiz) throws DatabaseException {
        String sql = "UPDATE quizzes SET title = ?, description = ?, duration_minutes = ?, "
                + "total_marks = ?, status = ? WHERE quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quiz.getTitle());
            ps.setString(2, quiz.getDescription());
            ps.setInt(3, quiz.getDurationMinutes());
            ps.setInt(4, quiz.getTotalMarks());
            ps.setString(5, quiz.getStatus().name());
            ps.setInt(6, quiz.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Update failed: quiz #" + quiz.getId() + " does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update quiz: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteQuiz(int quizId) throws DatabaseException {
        String sql = "DELETE FROM quizzes WHERE quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Delete failed: quiz #" + quizId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete quiz #" + quizId + ": " + e.getMessage(), e);
        }
    }

    /** Used by submit-for-approval / approve / reject workflow. */
    @Override
    public void updateQuizStatus(int quizId, String status) throws DatabaseException {
        String sql = "UPDATE quizzes SET status = ? WHERE quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, quizId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Cannot change status: quiz #" + quizId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update quiz status: " + e.getMessage(), e);
        }
    }

    @Override
    public int countQuizzesByStatus(String status) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM quizzes WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count quizzes: " + e.getMessage(), e);
        }
    }

    @Override
    public int countAllQuizzes() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM quizzes";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count quizzes: " + e.getMessage(), e);
        }
    }
}