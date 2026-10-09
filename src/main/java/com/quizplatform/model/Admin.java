package com.quizplatform.model;

import com.quizplatform.exception.InvalidLoginException;

/**
 * Administrator account - extends the abstract class User.
 * Demonstrates INHERITANCE and METHOD OVERRIDING.
 */
public class Admin extends User {

    public Admin(int id, String name, String email, String password) {
        super(id, name, email, password, Role.ADMIN);
    }

    /**
     * Demonstrates METHOD OVERRIDING: login() adds an extra role check
     * on top of the validation implemented in the parent class User.
     */
    @Override
    public boolean login(String email, String password) throws InvalidLoginException {
        boolean success = super.login(email, password);
        if (getRole() != Role.ADMIN) {
            throw new InvalidLoginException("Access denied: administrator role required.");
        }
        return success;
    }

    /** Demonstrates runtime polymorphism: opens the Admin dashboard. */
    @Override
    public void displayDashboard() {
        new com.quizplatform.gui.AdminDashboard((Admin) this).setVisible(true);
    }

    @Override
    public String getDashboardName() {
        return "Admin Dashboard";
    }
}
