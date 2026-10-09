package com.quizplatform.exception;

/**
 * Demonstrates custom exception handling (checked exception).
 * Thrown for invalid quiz data: empty title, missing questions,
 * missing options, invalid duration, missing correct answer, etc.
 */
public class InvalidQuizException extends Exception {

    public InvalidQuizException(String message) {
        super(message);
    }

    public InvalidQuizException(String message, Throwable cause) {
        super(message, cause);
    }
}
