package com.quizplatform;

import com.quizplatform.gui.UIUtil;

import javax.swing.SwingUtilities;

/**
 * Entry point of the Swing desktop application.
 *
 * Java-Based Online Quiz Platform
 * Core Java + OOP + Collections + Generics + Exception Handling + Multithreading
 * + Synchronization + JDBC + DAO + Swing GUI + Java Servlets.
 */
public class QuizApplication {

    public static void main(String[] args) {
        // Initialize modern FlatLaf theme and design system tokens
        UIUtil.setupTheme();

        // Swing components must be created on the Event Dispatch Thread (EDT)
        // Demonstrates correct Swing threading
        SwingUtilities.invokeLater(() -> {
            new com.quizplatform.gui.LoginFrame().setVisible(true);
        });
    }
}
