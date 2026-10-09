package com.quizplatform.dao.impl;

import com.quizplatform.dao.OptionDAO;
import com.quizplatform.dao.QuestionDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of QuestionDAO.
 * Answer options are loaded through OptionDAO (composition of DAOs).
 */
public class QuestionDAOImpl implements QuestionDAO {

    private static final String BASE_SELECT =
            "SELECT question_id, quiz_id, question_text, marks, explanation FROM questions";

    private Question mapRow(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setId(rs.getInt("question_id"));
        question.setQuizId(rs.getInt("quiz_id"));
        question.setText(rs.getString("question_text"));
        question.setMarks(rs.getInt("marks"));
        question.setExplanation(rs.getString("explanation"));
        return question;
    }

    @Override
    public int addQuestion(Question question) throws DatabaseException {
        String sql = "INSERT INTO questions (quiz_id, question_text, marks, explanation) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, question.getQuizId());
            ps.setString(2, question.getText());
            ps.setInt(3, question.getMarks());
            ps.setString(4, question.getExplanation());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    question.setId(id);
                    return id;
                }
            }
            throw new DatabaseException("Question inserted but no generated id returned.");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add question: " + e.getMessage(), e);
        }
    }

    @Override
    public Question getQuestionById(int questionId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load question #" + questionId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Question> getQuestionsByQuiz(int quizId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE quiz_id = ? ORDER BY question_id";
        List<Question> questions = new ArrayList<>();     // Demonstrates Java Collections
        OptionDAO optionDAO = new OptionDAOImpl();        // composition of DAOs
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question question = mapRow(rs);
                    question.setOptions(optionDAO.getOptionsByQuestion(question.getId()));
                    questions.add(question);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load questions for quiz #" + quizId + ": " + e.getMessage(), e);
        }
        return questions;
    }

    @Override
    public void updateQuestion(Question question) throws DatabaseException {
        String sql = "UPDATE questions SET question_text = ?, marks = ?, explanation = ? WHERE question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, question.getText());
            ps.setInt(2, question.getMarks());
            ps.setString(3, question.getExplanation());
            ps.setInt(4, question.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Update failed: question #" + question.getId() + " does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update question: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteQuestion(int questionId) throws DatabaseException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // options must be removed first because of the foreign key
            String deleteOptions = "DELETE FROM options WHERE question_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteOptions)) {
                ps.setInt(1, questionId);
                ps.executeUpdate();
            }
            String deleteQuestion = "DELETE FROM questions WHERE question_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteQuestion)) {
                ps.setInt(1, questionId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    throw new DatabaseException("Delete failed: question #" + questionId + " does not exist.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete question #" + questionId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteQuestionsByQuiz(int quizId) throws DatabaseException {
        String sql = "DELETE FROM questions WHERE quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete questions of quiz #" + quizId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int countQuestionsByQuiz(int quizId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM questions WHERE quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count questions: " + e.getMessage(), e);
        }
    }
}