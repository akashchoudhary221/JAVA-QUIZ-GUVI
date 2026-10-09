package com.quizplatform;

import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Role;
import com.quizplatform.util.PasswordUtil;
import com.quizplatform.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for password hashing, input validation and custom exceptions
 * (no database required).
 */
class ValidationAndExceptionTest {

    @Test
    @DisplayName("SHA-256 hashing is deterministic and never stores plain text")
    void passwordHashingWorks() {
        String hash1 = PasswordUtil.hash("admin123");
        String hash2 = PasswordUtil.hash("admin123");
        String other = PasswordUtil.hash("otherPassword");

        assertEquals(64, hash1.length(), "SHA-256 hex string has 64 characters");
        assertEquals(hash1, hash2, "same input -> same hash");
        assertFalse(hash1.equals(other));
        assertFalse(hash1.contains("admin123"), "hash must not contain the plain password");
        assertTrue(PasswordUtil.matches("admin123", hash1));
        assertFalse(PasswordUtil.matches("wrong", hash1));
    }

    @Test
    @DisplayName("ValidationUtil checks email, duration, password strength and marks")
    void validationHelpersWork() {
        assertTrue(ValidationUtil.isValidEmail("student@quiz.com"));
        assertFalse(ValidationUtil.isValidEmail("not-an-email"));
        assertFalse(ValidationUtil.isValidEmail(null));

        assertTrue(ValidationUtil.isValidDuration(10));
        assertFalse(ValidationUtil.isValidDuration(0));
        assertFalse(ValidationUtil.isValidDuration(5000));

        assertTrue(ValidationUtil.isStrongPassword("123456"));
        assertFalse(ValidationUtil.isStrongPassword("123"));

        assertTrue(ValidationUtil.isBlank("   "));
        assertFalse(ValidationUtil.isBlank("value"));
    }

    @Test
    @DisplayName("Role enum conversion throws IllegalArgumentException for unknown roles")
    void roleConversionRejectsUnknownValues() {
        assertEquals(Role.PARTICIPANT, Role.fromString("participant"));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("SUPERUSER"));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString(null));
    }

    @Test
    @DisplayName("User.login() raises InvalidLoginException for wrong password and unknown email")
    void loginRaisesCustomException() throws InvalidLoginException {
        // Demonstrates: polymorphic object + overridden login() + custom exception
        Participant participant = new Participant(1, "Student",
                "student@test.com", PasswordUtil.hash("correctPassword"));

        InvalidLoginException wrongPassword = assertThrows(InvalidLoginException.class,
                () -> participant.login("student@test.com", "wrongPassword"));
        assertTrue(wrongPassword.getMessage().contains("Incorrect password"));

        InvalidLoginException unknownEmail = assertThrows(InvalidLoginException.class,
                () -> participant.login("nobody@test.com", "correctPassword"));
        assertTrue(unknownEmail.getMessage().contains("No account found"));

        assertThrows(InvalidLoginException.class,
                () -> participant.login("", "correctPassword"));

        // correct credentials succeed and mark the user as logged in
        assertTrue(participant.login("student@test.com", "correctPassword"));
        assertTrue(participant.isLoggedIn());
        participant.logout();
        assertFalse(participant.isLoggedIn());
    }
}
