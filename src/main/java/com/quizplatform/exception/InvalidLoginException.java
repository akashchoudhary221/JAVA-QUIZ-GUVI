package com.quizplatform.exception;

/**
 * Demonstrates custom exception handling (checked exception).
 * Thrown when login fails because of unknown email, wrong password
 * or an attempt to access a dashboard that does not belong to the role.
 */
public class InvalidLoginException extends Exception {

    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
