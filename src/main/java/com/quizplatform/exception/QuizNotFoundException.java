package com.quizplatform.exception;

/**
 * Demonstrates custom exception handling (checked exception).
 * Thrown when a quiz that is expected to exist cannot be found
 * in the database (e.g. deleted by an admin while a user is viewing it).
 */
public class QuizNotFoundException extends Exception {

    public QuizNotFoundException(String message) {
        super(message);
    }

    public QuizNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
