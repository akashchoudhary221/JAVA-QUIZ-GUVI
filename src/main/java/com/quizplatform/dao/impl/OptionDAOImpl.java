package com.quizplatform.dao.impl;

import com.quizplatform.dao.OptionDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Option;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of OptionDAO (the A/B/C/D choices).
 */
public class OptionDAOImpl implements OptionDAO {

    @Override
    public int addOption(Option option) throws DatabaseException {
        String sql = "INSERT INTO options (question_id, option_text, option_order, is_correct) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, option.getQuestionId());
            ps.setString(2, option.getOptionText());
            ps.setInt(3, option.getOptionOrder());
            ps.setBoolean(4, option.isCorrect());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    option.setId(id);
                    return id;
                }
            }
            throw new DatabaseException("Option inserted but no generated id returned.");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add option: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Option> getOptionsByQuestion(int questionId) throws DatabaseException {
        String sql = "SELECT option_id, question_id, option_text, option_order, is_correct "
                + "FROM options WHERE question_id = ? ORDER BY option_order";
        List<Option> options = new ArrayList<>();       // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Option option = new Option();
                    option.setId(rs.getInt("option_id"));
                    option.setQuestionId(rs.getInt("question_id"));
                    option.setOptionText(rs.getString("option_text"));
                    option.setOptionOrder(rs.getInt("option_order"));
                    option.setCorrect(rs.getBoolean("is_correct"));
                    options.add(option);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load options: " + e.getMessage(), e);
        }
        return options;
    }

    @Override
    public void deleteOptionsByQuestion(int questionId) throws DatabaseException {
        String sql = "DELETE FROM options WHERE question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete options: " + e.getMessage(), e);
        }
    }
}
