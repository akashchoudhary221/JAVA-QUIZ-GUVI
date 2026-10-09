package com.quizplatform.model;

import com.quizplatform.exception.InvalidLoginException;

/**
 * Participant account - extends the abstract class User.
 * Demonstrates INHERITANCE and METHOD OVERRIDING.
 */
public class Participant extends User {

    public Participant(int id, String name, String email, String password) {
        super(id, name, email, password, Role.PARTICIPANT);
    }

    /** Demonstrates METHOD OVERRIDING with a role specific rule. */
    @Override
    public boolean login(String email, String password) throws InvalidLoginException {
        boolean success = super.login(email, password);
        if (getRole() != Role.PARTICIPANT) {
            throw new InvalidLoginException("Access denied: participant role required.");
        }
        return success;
    }

    /** Demonstrates runtime polymorphism: opens the Participant dashboard. */
    @Override
    public void displayDashboard() {
        new com.quizplatform.gui.ParticipantDashboard((Participant) this).setVisible(true);
    }

    @Override
    public String getDashboardName() {
        return "Participant Dashboard";
    }
}
