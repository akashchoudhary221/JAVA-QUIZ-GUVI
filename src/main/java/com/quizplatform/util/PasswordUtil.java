package com.quizplatform.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Password security utility.
 *
 * Demonstrates the use of Java's built-in cryptographic API
 * (java.security.MessageDigest) to store SHA-256 hashes instead of
 * plain text passwords.
 */
public final class PasswordUtil {

    private PasswordUtil() {
        // utility class - no instances allowed
    }

    /** Returns the SHA-256 hash of the given plain password as a hex string. */
    public static String hash(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by every Java implementation
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /** Compares a plain password with a stored hash (constant usage of hash()). */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        return hash(plainPassword).equals(storedHash.trim());
    }
}
