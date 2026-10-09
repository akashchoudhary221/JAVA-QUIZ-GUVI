package com.quizplatform.model;

/** How a quiz attempt ended. */
public enum AttemptStatus {

    /** Participant pressed the Submit button before the timer ended. */
    COMPLETED("Submitted"),

    /** The timer thread reached 00:00 and the quiz was auto-submitted. */
    AUTO_SUBMITTED("Time Expired");

    private final String label;

    AttemptStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AttemptStatus fromString(String value) {
        if (value == null) {
            return COMPLETED;
        }
        return AttemptStatus.valueOf(value.trim().toUpperCase());
    }
}
