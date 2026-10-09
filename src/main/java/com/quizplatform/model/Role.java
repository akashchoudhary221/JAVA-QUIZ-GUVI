package com.quizplatform.model;

/**
 * Role of a user in the platform.
 * Stored in the database as a VARCHAR and mapped back to this enum
 * (see UserDAOImpl), which keeps Java code type-safe.
 */
public enum Role {

    ADMIN("Administrator"),
    QUIZ_CREATOR("Quiz Creator"),
    PARTICIPANT("Participant");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Safe conversion from database string to enum (throws IllegalArgumentException if unknown). */
    public static Role fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}
