package com.quizplatform.service;

import com.quizplatform.dao.UserDAO;
import com.quizplatform.dao.impl.UserDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.interfaces.UserOperations;
import com.quizplatform.model.User;
import com.quizplatform.util.PasswordUtil;
import com.quizplatform.util.ValidationUtil;

import java.util.List;

/**
 * Business logic for user management (Admin role).
 * Implements the UserOperations interface.
 *
 * CONVENTION: createUser()/updateUser() receive the PLAIN password typed
 * by the admin - the service validates it and stores only the SHA-256 hash.
 */
public class UserService implements UserOperations {

    private final UserDAO userDAO;

    public UserService() {
        this(new UserDAOImpl());
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Demonstrates INTERFACE IMPLEMENTATION
    @Override
    public User createUser(User user) throws InvalidUserException, DatabaseException {
        validate(user, true);
        // Demonstrates custom exception: duplicate email
        if (userDAO.emailExists(user.getEmail().trim())) {
            throw new InvalidUserException("Email already registered: " + user.getEmail());
        }
        user.setPassword(PasswordUtil.hash(user.getPassword()));   // never store plain text
        userDAO.createUser(user);
        return user;
    }

    @Override
    public User updateUser(User user) throws InvalidUserException, DatabaseException {
        if (user == null || user.getId() <= 0) {
            throw new InvalidUserException("Please select a valid user to update.");
        }
        User existing = userDAO.getUserById(user.getId());
        if (existing == null) {
            throw new InvalidUserException("User #" + user.getId() + " does not exist anymore.");
        }
        validate(user, false);   // password may stay empty on update

        if (ValidationUtil.isBlank(user.getPassword())
                || user.getPassword().equals(existing.getPassword())) {
            user.setPassword(existing.getPassword());          // keep old hash (unchanged password)
        } else {
            user.setPassword(PasswordUtil.hash(user.getPassword())); // new plain password
        }

        User other = userDAO.getUserByEmail(user.getEmail().trim());
        if (other != null && other.getId() != user.getId()) {
            throw new InvalidUserException("Email already used by another user: " + user.getEmail());
        }
        userDAO.updateUser(user);
        return user;
    }

    @Override
    public void deleteUser(int userId) throws DatabaseException {
        userDAO.deleteUser(userId);
    }

    @Override
    public User getUserById(int userId) throws DatabaseException {
        return userDAO.getUserById(userId);
    }

    @Override
    public User getUserByEmail(String email) throws DatabaseException {
        return userDAO.getUserByEmail(email);
    }

    @Override
    public List<User> getAllUsers() throws DatabaseException {
        return userDAO.getAllUsers();
    }

    /** Validates user input - throws a custom exception with a GUI-friendly message. */
    private void validate(User user, boolean requirePassword) throws InvalidUserException {
        if (user == null) {
            throw new InvalidUserException("User data must not be empty.");
        }
        if (ValidationUtil.isBlank(user.getName())) {
            throw new InvalidUserException("Name is required.");
        }
        if (user.getName().trim().length() < 2) {
            throw new InvalidUserException("Name must contain at least 2 characters.");
        }
        if (ValidationUtil.isBlank(user.getEmail())) {
            throw new InvalidUserException("Email is required.");
        }
        if (!ValidationUtil.isValidEmail(user.getEmail())) {
            throw new InvalidUserException("Invalid email format: " + user.getEmail());
        }
        if (user.getRole() == null) {
            throw new InvalidUserException("Role is required.");
        }
        if (requirePassword && !ValidationUtil.isStrongPassword(user.getPassword())) {
            throw new InvalidUserException("Password must be at least 6 characters.");
        }
        if (!requirePassword && !ValidationUtil.isBlank(user.getPassword())
                && !ValidationUtil.isStrongPassword(user.getPassword())) {
            throw new InvalidUserException("New password must be at least 6 characters.");
        }
    }
}
