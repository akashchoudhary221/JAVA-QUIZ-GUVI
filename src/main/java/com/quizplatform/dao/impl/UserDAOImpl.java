package com.quizplatform.dao.impl;

import com.quizplatform.dao.UserDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Admin;
import com.quizplatform.model.Participant;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.model.Role;
import com.quizplatform.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of the UserDAO interface.
 *
 * Demonstrates:
 *  - PreparedStatement (SQL injection safe - user input is never concatenated)
 *  - ResultSet mapping (row -> object)
 *  - try-with-resources (auto closing of Connection / Statement / ResultSet)
 *  - Polymorphic object creation: one SQL row becomes Admin, QuizCreator
 *    or Participant depending on the role column.
 */
public class UserDAOImpl implements UserDAO {

    private static final String BASE_SELECT =
            "SELECT user_id, name, email, password, role, created_at FROM users";

    // ------------------------------------------------------------------
    // Creates a polymorphic User object from the current ResultSet row.
    // Demonstrates runtime polymorphism through a User reference.
    // ------------------------------------------------------------------
    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password");
        Role role = Role.fromString(rs.getString("role"));
        Timestamp created = rs.getTimestamp("created_at");

        User user = switch (role) {
            case ADMIN -> new Admin(id, name, email, passwordHash);
            case QUIZ_CREATOR -> new QuizCreator(id, name, email, passwordHash);
            case PARTICIPANT -> new Participant(id, name, email, passwordHash);
        };
        if (created != null) {
            user.setCreatedAt(created.toLocalDateTime());
        }
        return user;
    }

    /** Translates SQLException into a meaningful DatabaseException (no empty catch blocks). */
    private DatabaseException translate(String email, String action, SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("duplicate")) {
            return new DatabaseException("A user with email '" + email + "' already exists.", e);
        }
        return new DatabaseException("Failed to " + action + " user: " + e.getMessage(), e);
    }

    // Demonstrates JDBC insert with generated keys
    @Override
    public int createUser(User user) throws DatabaseException {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());   // SHA-256 hash
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    user.setId(id);
                    return id;
                }
            }
            throw new DatabaseException("User was inserted but no generated id was returned.");
        } catch (SQLException e) {
            throw translate(user.getEmail(), "create", e);
        }
    }

    @Override
    public User getUserById(int userId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load user #" + userId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public User getUserByEmail(String email) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> getAllUsers() throws DatabaseException {
        String sql = BASE_SELECT + " ORDER BY user_id";
        List<User> users = new ArrayList<>();          // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load users: " + e.getMessage(), e);
        }
        return users;
    }

    @Override
    public void updateUser(User user) throws DatabaseException {
        String sql = "UPDATE users SET name = ?, email = ?, password = ?, role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole().name());
            ps.setInt(5, user.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Update failed: user #" + user.getId() + " does not exist.");
            }
        } catch (SQLException e) {
            throw translate(user.getEmail(), "update", e);
        }
    }

    @Override
    public void deleteUser(int userId) throws DatabaseException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Delete failed: user #" + userId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user #" + userId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean emailExists(String email) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check email: " + e.getMessage(), e);
        }
    }

    @Override
    public int countUsers() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count users: " + e.getMessage(), e);
        }
    }
}
