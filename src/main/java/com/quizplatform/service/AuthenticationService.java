package com.quizplatform.service;

import com.quizplatform.dao.UserDAO;
import com.quizplatform.dao.impl.UserDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.User;
import com.quizplatform.util.ValidationUtil;

/**
 * Handles login / logout / registration for the whole application
 * (used by both the Swing GUI and the Servlets).
 */
public class AuthenticationService {

    private final UserDAO userDAO;

    public AuthenticationService() {
        this(new UserDAOImpl());
    }

    /** Constructor injection - makes the service testable. */
    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user.
     *
     * Demonstrates RUNTIME POLYMORPHISM: the object loaded from the database
     * is an Admin, QuizCreator or Participant. Calling user.login(...)
     * therefore executes the OVERRIDDEN login() of that concrete role.
     */
    public User login(String email, String password) throws InvalidLoginException, DatabaseException {
        if (ValidationUtil.isBlank(email)) {
            throw new InvalidLoginException("Email must not be empty.");
        }
        if (ValidationUtil.isBlank(password)) {
            throw new InvalidLoginException("Password must not be empty.");
        }
        User user = userDAO.getUserByEmail(email.trim());
        if (user == null) {
            // Demonstrates custom exception handling
            throw new InvalidLoginException("No account found with email: " + email);
        }
        user.login(email.trim(), password);   // polymorphic call
        return user;
    }

    /** Self-registration - every new account becomes a Participant. */
    public User registerParticipant(String name, String email, String password)
            throws InvalidUserException, DatabaseException {
        User participant = new Participant(0, name, email, password);
        return new UserService(userDAO).createUser(participant);
    }

    public void logout(User user) {
        if (user != null) {
            user.logout();
        }
    }
}
