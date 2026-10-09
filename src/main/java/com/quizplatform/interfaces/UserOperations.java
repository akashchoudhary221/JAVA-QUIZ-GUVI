package com.quizplatform.interfaces;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.model.User;

import java.util.List;

/**
 * Demonstrates interface-based programming.
 * Implemented by UserService - defines all user (CRUD) operations
 * available to the Admin role.
 */
public interface UserOperations {

    User createUser(User user) throws InvalidUserException, DatabaseException;

    User updateUser(User user) throws InvalidUserException, DatabaseException;

    void deleteUser(int userId) throws DatabaseException;

    User getUserById(int userId) throws DatabaseException;

    User getUserByEmail(String email) throws DatabaseException;

    List<User> getAllUsers() throws DatabaseException;
}
