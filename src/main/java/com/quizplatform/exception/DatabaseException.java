package com.quizplatform.exception;

/**
 * Demonstrates custom exception handling (checked exception).
 * Wraps every SQLException so that GUI / servlet layers never deal
 * with raw JDBC exceptions and can show a meaningful message instead.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
