package com.quizplatform.exception;

/**
 * Demonstrates custom exception handling (checked exception).
 * Thrown for invalid user input: empty name, invalid email format,
 * duplicate email, weak password or an unknown role.
 */
public class InvalidUserException extends Exception {

    public InvalidUserException(String message) {
        super(message);
    }

    public InvalidUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
