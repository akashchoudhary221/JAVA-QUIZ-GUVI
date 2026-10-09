package com.quizplatform.database;

import com.quizplatform.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralized JDBC connection management (no framework - plain JDBC).
 *
 * - Reads driver / url / username / password from config.properties
 *   (classpath first, then the project directory) so that credentials
 *   are NOT hardcoded throughout the project.
 * - Creates a new Connection on demand (callers use try-with-resources).
 * - Provides close() helpers so that no resource leaks occur.
 */
public final class DatabaseConnection {

    private static final String CONFIG_RESOURCE = "config.properties";
    private static final String CONFIG_FILE = "config.properties";

    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String url = "jdbc:mysql://localhost:3306/java_quiz_platform";
    private static String user = "root";
    private static String password = "root";

    private static boolean loaded = false;

    private DatabaseConnection() {
    }

    /**
     * Programmatic override of the configuration.
     * Used by unit tests (in-memory H2 database) and by advanced
     * users who want to configure the database without a properties file.
     */
    public static synchronized void configure(String jdbcUrl, String dbUser, String dbPassword) {
        url = jdbcUrl;
        user = dbUser;
        password = dbPassword;
        loaded = true;
    }

    private static synchronized void loadConfig() {
        if (loaded) {
            return;
        }
        Properties props = new Properties();

        // 1) try classpath (src/main/resources/config.properties)
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConnection] Could not read classpath config: " + e.getMessage());
        }

        // 2) fallback: config.properties in the working directory
        if (props.isEmpty() && Files.exists(Path.of(CONFIG_FILE))) {
            try (InputStream in = Files.newInputStream(Path.of(CONFIG_FILE))) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("[DatabaseConnection] Could not read file config: " + e.getMessage());
            }
        }

        driver = props.getProperty("db.driver", driver);
        url = props.getProperty("db.url", url);
        user = props.getProperty("db.user", user);
        password = props.getProperty("db.password", password);
        loaded = true;
    }

    /**
     * Demonstrates JDBC: creates and returns a new database connection.
     *
     * @throws DatabaseException when the driver is missing or the connection fails
     */
    public static Connection getConnection() throws DatabaseException {
        loadConfig();
        try {
            // Explicit driver loading (classic JDBC step, even though modern
            // drivers also auto-register through the ServiceLoader)
            Class.forName(driver);
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC driver not found: " + driver, e);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot connect to database. Check config.properties "
                    + "(url/user/password) and make sure MySQL is running. Cause: " + e.getMessage(), e);
        }
    }

    /** Closes resources quietly but never silently: errors are reported on stderr. */
    public static void close(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("[DatabaseConnection] Error closing resource: " + e.getMessage());
                }
            }
        }
    }

    public static String getUrl() {
        loadConfig();
        return url;
    }
}
