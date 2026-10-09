package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.User;

import java.util.List;

/**
 * DAO pattern: interface for all user table operations.
 * Implemented by com.quizplatform.dao.impl.UserDAOImpl (JDBC).
 * GUI classes never write SQL themselves - they call this interface.
 */
public interface UserDAO {

    int createUser(User user) throws DatabaseException;

    User getUserById(int userId) throws DatabaseException;

    /** Returns null when no user exists with this email. */
    User getUserByEmail(String email) throws DatabaseException;

    List<User> getAllUsers() throws DatabaseException;

    void updateUser(User user) throws DatabaseException;

    void deleteUser(int userId) throws DatabaseException;

    boolean emailExists(String email) throws DatabaseException;

    int countUsers() throws DatabaseException;
}
