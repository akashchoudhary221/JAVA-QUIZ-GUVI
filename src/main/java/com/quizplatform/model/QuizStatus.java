package com.quizplatform.model;

/**
 * Lifecycle of a quiz.
 *
 * DRAFT -> PENDING_APPROVAL -> APPROVED (visible to participants)
 *                            -> REJECTED (back to the creator)
 *
 * Only APPROVED quizzes are shown in the participant dashboard.
 */
public enum QuizStatus {

    DRAFT("Draft"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PUBLISHED("Published");

    private final String label;

    QuizStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static QuizStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Quiz status must not be null");
        }
        return QuizStatus.valueOf(value.trim().toUpperCase());
    }
}
