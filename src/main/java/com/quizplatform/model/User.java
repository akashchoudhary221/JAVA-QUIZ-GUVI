package com.quizplatform.model;

import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.interfaces.Authenticatable;
import com.quizplatform.util.PasswordUtil;

import java.time.LocalDateTime;

/**
 * Abstract base class of the user hierarchy.
 *
 * Demonstrates INHERITANCE:
 *      User (abstract)
 *       ├── Admin
 *       ├── QuizCreator
 *       └── Participant
 *
 * Demonstrates INTERFACES: implements Authenticatable (login / logout).
 *
 * Demonstrates METHOD OVERRIDING: every subclass overrides login()
 * (role check) and displayDashboard() (opens its own dashboard window).
 */
public abstract class User implements Authenticatable {

    private int id;
    private String name;
    private String email;
    /** SHA-256 hash of the password - the plain password is never stored. */
    private String password;
    private final Role role;
    private LocalDateTime createdAt;
    private boolean loggedIn;

    protected User(int id, String name, String email, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------------
    // Demonstrates INTERFACE IMPLEMENTATION (base behaviour)
    // ------------------------------------------------------------------
    @Override
    public boolean login(String email, String password) throws InvalidLoginException {
        if (email == null || email.isBlank()) {
            throw new InvalidLoginException("Email must not be empty.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidLoginException("Password must not be empty.");
        }
        if (!this.email.equalsIgnoreCase(email.trim())) {
            throw new InvalidLoginException("No account found for email: " + email);
        }
        // compare hashes - never store / compare plain text passwords
        if (!PasswordUtil.matches(password, this.password)) {
            throw new InvalidLoginException("Incorrect password for email: " + email);
        }
        this.loggedIn = true;
        return true;
    }

    @Override
    public void logout() {
        this.loggedIn = false;
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Opens the dashboard that belongs to this role.
     *
     * Demonstrates RUNTIME POLYMORPHISM: the concrete window is chosen
     * at runtime from a User reference, e.g.
     *
     *   User user = userDAO.getUserByEmail("admin@quiz.com");
     *   user.displayDashboard();   // opens AdminDashboard
     */
    public abstract void displayDashboard();

    /** Short description used in menus / reports. */
    public abstract String getDashboardName();

    // ------------------------------------------------------------------
    // Getters / setters
    // ------------------------------------------------------------------
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', role=" + role + "}";
    }
}
