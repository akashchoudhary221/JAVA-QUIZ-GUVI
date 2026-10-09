package com.quizplatform.model;

import com.quizplatform.exception.InvalidLoginException;

/**
 * Quiz Creator account - extends the abstract class User.
 * Demonstrates INHERITANCE and METHOD OVERRIDING.
 */
public class QuizCreator extends User {

    public QuizCreator(int id, String name, String email, String password) {
        super(id, name, email, password, Role.QUIZ_CREATOR);
    }

    /** Demonstrates METHOD OVERRIDING with a role specific rule. */
    @Override
    public boolean login(String email, String password) throws InvalidLoginException {
        boolean success = super.login(email, password);
        if (getRole() != Role.QUIZ_CREATOR) {
            throw new InvalidLoginException("Access denied: quiz creator role required.");
        }
        return success;
    }

    /** Demonstrates runtime polymorphism: opens the Quiz Creator dashboard. */
    @Override
    public void displayDashboard() {
        new com.quizplatform.gui.QuizCreatorDashboard((QuizCreator) this).setVisible(true);
    }

    @Override
    public String getDashboardName() {
        return "Quiz Creator Dashboard";
    }
}
