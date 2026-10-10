package com.quizplatform.database;

import com.quizplatform.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
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

        ConnectionConfig config = resolveConfig(props, System.getenv());
        driver = config.driver;
        url = config.url;
        user = config.user;
        password = config.password;
        loaded = true;
    }

    /**
     * Immutable snapshot of resolved database connection parameters.
     */
    public static class ConnectionConfig {
        private final String driver;
        private final String url;
        private final String user;
        private final String password;

        public ConnectionConfig(String driver, String url, String user, String password) {
            this.driver = driver;
            this.url = url;
            this.user = user;
            this.password = password;
        }

        public String getDriver() {
            return driver;
        }

        public String getUrl() {
            return url;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }
    }

    /**
     * Resolves connection configuration against baseline properties and environment variables.
     *
     * URL Precedence:
     * 1. DB_URL (direct JDBC URL, e.g. jdbc:mysql://...) takes highest precedence.
     * 2. MYSQL_URL (cloud provider connection string, e.g. mysql://user:pass@host:port/database).
     *    Standard cloud providers format URLs with user info in the authority component.
     *    Because MySQL Connector/J expects "jdbc:mysql://host:port/database" without credentials
     *    in the authority, this method parses the URI, extracts user/password, and formats a valid JDBC URL.
     * 3. If neither is defined, the url from config.properties is preserved.
     *
     * Credential Precedence:
     * - DB_USER and DB_PASSWORD environment variables override any credentials extracted from URL or properties.
     */
    public static ConnectionConfig resolveConfig(Properties props, Map<String, String> env) {
        String baseDriver = props.getProperty("db.driver", driver);
        String baseUrl = props.getProperty("db.url", url);
        String baseUser = props.getProperty("db.user", user);
        String basePassword = props.getProperty("db.password", password);

        String resolvedDriver = baseDriver;
        String resolvedUrl = baseUrl;
        String resolvedUser = baseUser;
        String resolvedPassword = basePassword;

        String envDbUrl = env != null ? env.get("DB_URL") : null;
        String envMysqlUrl = env != null ? env.get("MYSQL_URL") : null;
        String envUser = env != null ? env.get("DB_USER") : null;
        String envPass = env != null ? env.get("DB_PASSWORD") : null;

        if (envDbUrl != null && !envDbUrl.isBlank()) {
            // 1. Direct JDBC URL takes highest precedence
            resolvedUrl = envDbUrl.trim();
        } else if (envMysqlUrl != null && !envMysqlUrl.isBlank()) {
            // 2. Cloud provider URL parsing
            String raw = envMysqlUrl.trim();
            if (raw.startsWith("jdbc:mysql://")) {
                resolvedUrl = raw;
            } else {
                try {
                    URI uri = URI.create(raw);
                    String scheme = uri.getScheme();
                    if (scheme == null) {
                        System.err.println("[DatabaseConnection] MYSQL_URL is missing scheme; retaining default configuration.");
                    } else if (!scheme.equalsIgnoreCase("mysql") && !scheme.equalsIgnoreCase("mysqls")) {
                        System.err.println("[DatabaseConnection] Unsupported MYSQL_URL scheme: " + scheme + "; retaining default configuration.");
                    } else {
                        String host = uri.getHost();
                        String path = uri.getPath();

                        // Validate host and usable database path (must start with '/' and specify a database name)
                        if (host == null || host.isBlank()) {
                            System.err.println("[DatabaseConnection] MYSQL_URL is missing host; retaining default configuration.");
                        } else if (path == null || path.length() <= 1 || path.equals("/")) {
                            System.err.println("[DatabaseConnection] MYSQL_URL is missing database path; retaining default configuration.");
                        } else {
                            int port = uri.getPort();
                            String query = uri.getQuery();

                            // Preserve secure-connection intent for mysqls://
                            if (scheme.equalsIgnoreCase("mysqls")) {
                                if (query == null || query.isBlank()) {
                                    query = "sslMode=REQUIRED";
                                } else if (!query.contains("sslMode") && !query.contains("useSSL")) {
                                    query = query + "&sslMode=REQUIRED";
                                }
                            }

                            StringBuilder jdbc = new StringBuilder("jdbc:mysql://").append(host);
                            if (port > 0) {
                                jdbc.append(":").append(port);
                            }
                            jdbc.append(path);
                            if (query != null && !query.isBlank()) {
                                jdbc.append("?").append(query);
                            }
                            resolvedUrl = jdbc.toString();

                            // URI.getUserInfo() already provides decoded user-info per RFC 2396.
                            // We avoid calling URLDecoder to prevent converting literal '+' into spaces.
                            String userInfo = uri.getUserInfo();
                            if (userInfo != null && !userInfo.isEmpty()) {
                                int colon = userInfo.indexOf(':');
                                if (colon >= 0) {
                                    resolvedUser = userInfo.substring(0, colon);
                                    resolvedPassword = userInfo.substring(colon + 1);
                                } else {
                                    resolvedUser = userInfo;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DatabaseConnection] Malformed MYSQL_URL; retaining default configuration.");
                }
            }
        }

        if (envUser != null && !envUser.isBlank()) {
            resolvedUser = envUser.trim();
        }
        if (envPass != null) {
            resolvedPassword = envPass;
        }

        return new ConnectionConfig(resolvedDriver, resolvedUrl, resolvedUser, resolvedPassword);
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
