package com.quizplatform.interfaces;

import com.quizplatform.exception.InvalidLoginException;

/**
 * Demonstrates interface-based programming.
 * Any entity that can be authenticated (Admin, QuizCreator, Participant)
 * must provide login / logout behaviour.
 */
public interface Authenticatable {

    /** Validates email + password for this user. */
    boolean login(String email, String password) throws InvalidLoginException;

    /** Ends the current session of this user. */
    void logout();

    /** True while the user is logged in. */
    boolean isLoggedIn();
}
