package com.quizplatform.util;

import java.util.regex.Pattern;

/**
 * Small validation helpers used by the service layer before data
 * is written to the database. Services convert a "false" result of
 * these helpers into a custom exception (InvalidUserException / InvalidQuizException).
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidDuration(int minutes) {
        return minutes >= 1 && minutes <= 480; // 1 minute .. 8 hours
    }

    public static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidMarks(int marks) {
        return marks >= 1 && marks <= 100;
    }
}
