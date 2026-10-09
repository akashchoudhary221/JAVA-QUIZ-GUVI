package com.quizplatform.exception;

/**
 * Demonstrates custom exception handling (checked exception).
 * Thrown when the participant tries to continue or submit a quiz
 * after the timer thread has reached 00:00.
 */
public class TimeExpiredException extends Exception {

    private final int secondsRemaining;

    public TimeExpiredException(String message) {
        this(message, 0);
    }

    public TimeExpiredException(String message, int secondsRemaining) {
        super(message);
        this.secondsRemaining = secondsRemaining;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }
}
