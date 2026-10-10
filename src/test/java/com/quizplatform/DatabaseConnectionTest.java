package com.quizplatform;

import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.database.DatabaseConnection.ConnectionConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConnectionTest {

    private Properties createDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/java_quiz_platform");
        props.setProperty("db.user", "local_user");
        props.setProperty("db.password", "local_pass");
        return props;
    }

    @Test
    @DisplayName("Falls back to properties when environment map has no database variables")
    void testFallbackToPropertiesWhenNoEnv() {
        Properties props = createDefaultProperties();
        Map<String, String> env = new HashMap<>();

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://localhost:3306/java_quiz_platform", config.getUrl());
        assertEquals("local_user", config.getUser());
        assertEquals("local_pass", config.getPassword());
    }

    @Test
    @DisplayName("DB_URL takes precedence over MYSQL_URL and properties")
    void testDbUrlPrecedenceOverMysqlUrl() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("DB_URL", "jdbc:mysql://custom-host:3306/custom_db");
        env.put("MYSQL_URL", "mysql://other-user:other-pass@other-host:3306/other_db");
        env.put("DB_USER", "custom_user");
        env.put("DB_PASSWORD", "custom_secret");

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://custom-host:3306/custom_db", config.getUrl());
        assertEquals("custom_user", config.getUser());
        assertEquals("custom_secret", config.getPassword());
    }

    @Test
    @DisplayName("Parses cloud provider MYSQL_URL format and extracts user, password, and port")
    void testParseMysqlUrlWithCredentialsAndPort() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "mysql://demo_user:mockPass123@mysql.railway.internal:3306/quiz_db?useSSL=false");

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://mysql.railway.internal:3306/quiz_db?useSSL=false", config.getUrl());
        assertEquals("demo_user", config.getUser());
        assertEquals("mockPass123", config.getPassword());
    }

    @Test
    @DisplayName("DB_USER and DB_PASSWORD explicitly override credentials inside MYSQL_URL")
    void testDbUserAndDbPasswordOverrideMysqlUrlCredentials() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "mysql://default_user:default_pass@mysql.railway.internal:3306/quiz_db");
        env.put("DB_USER", "override_user");
        env.put("DB_PASSWORD", "override_pass");

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://mysql.railway.internal:3306/quiz_db", config.getUrl());
        assertEquals("override_user", config.getUser());
        assertEquals("override_pass", config.getPassword());
    }

    @Test
    @DisplayName("Preserves already well-formed jdbc:mysql:// URLs without altering them")
    void testJdbcMysqlUrlPreserved() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "jdbc:mysql://proxy.example.com:12345/quiz_db?useSSL=false");

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://proxy.example.com:12345/quiz_db?useSSL=false", config.getUrl());
    }

    @Test
    @DisplayName("Preserves literal plus signs and handles encoded special characters in credentials")
    void testSpecialCharactersInCredentials() {
        Properties props = createDefaultProperties();

        // Passwords with literal plus (+) and percent-encoded characters like %2B, %40 (@)
        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "mysql://demo_user:pass%2Bword+literal%40host@db.example.com:3306/quiz_db");

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://db.example.com:3306/quiz_db", config.getUrl());
        assertEquals("demo_user", config.getUser());
        // URI.getUserInfo() decodes %2B to + and %40 to @ while preserving literal +
        assertEquals("pass+word+literal@host", config.getPassword());
    }

    @Test
    @DisplayName("mysqls:// scheme preserves TLS intent by injecting sslMode=REQUIRED")
    void testMysqlsSchemePreservesTlsConfiguration() {
        Properties props = createDefaultProperties();

        // 1. Without pre-existing query string
        Map<String, String> env1 = new HashMap<>();
        env1.put("MYSQL_URL", "mysqls://tls_user:tls_pass@db.example.com:3306/quiz_db");
        ConnectionConfig config1 = DatabaseConnection.resolveConfig(props, env1);

        assertEquals("jdbc:mysql://db.example.com:3306/quiz_db?sslMode=REQUIRED", config1.getUrl());
        assertEquals("tls_user", config1.getUser());
        assertEquals("tls_pass", config1.getPassword());

        // 2. With pre-existing query string
        Map<String, String> env2 = new HashMap<>();
        env2.put("MYSQL_URL", "mysqls://tls_user:tls_pass@db.example.com:3306/quiz_db?serverTimezone=UTC");
        ConnectionConfig config2 = DatabaseConnection.resolveConfig(props, env2);

        assertEquals("jdbc:mysql://db.example.com:3306/quiz_db?serverTimezone=UTC&sslMode=REQUIRED", config2.getUrl());
    }

    @Test
    @DisplayName("Malformed MYSQL_URL with missing host falls back safely to properties baseline")
    void testMalformedMissingHostFallsBackToDefault() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "mysql:///quiz_db"); // missing host

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://localhost:3306/java_quiz_platform", config.getUrl());
        assertEquals("local_user", config.getUser());
        assertEquals("local_pass", config.getPassword());
    }

    @Test
    @DisplayName("Malformed MYSQL_URL with missing database path falls back safely to properties baseline")
    void testMalformedMissingDatabasePathFallsBackToDefault() {
        Properties props = createDefaultProperties();

        // Empty database path
        Map<String, String> env1 = new HashMap<>();
        env1.put("MYSQL_URL", "mysql://db.example.com:3306");
        ConnectionConfig config1 = DatabaseConnection.resolveConfig(props, env1);
        assertEquals("jdbc:mysql://localhost:3306/java_quiz_platform", config1.getUrl());

        // Root slash only with no database name
        Map<String, String> env2 = new HashMap<>();
        env2.put("MYSQL_URL", "mysql://db.example.com:3306/");
        ConnectionConfig config2 = DatabaseConnection.resolveConfig(props, env2);
        assertEquals("jdbc:mysql://localhost:3306/java_quiz_platform", config2.getUrl());
    }

    @Test
    @DisplayName("Unsupported scheme falls back safely to properties baseline")
    void testUnsupportedSchemeFallsBackToDefault() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "postgres://demo_user:demo_pass@db.example.com:5432/quiz_db");

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://localhost:3306/java_quiz_platform", config.getUrl());
        assertEquals("local_user", config.getUser());
        assertEquals("local_pass", config.getPassword());
    }

    @Test
    @DisplayName("Malformed syntax in MYSQL_URL falls back safely to properties baseline without throwing")
    void testMalformedSyntaxFallsBackToDefault() {
        Properties props = createDefaultProperties();

        Map<String, String> env = new HashMap<>();
        env.put("MYSQL_URL", "mysql://invalid host name:3306/quiz_db"); // space in authority causes URI syntax error

        ConnectionConfig config = DatabaseConnection.resolveConfig(props, env);

        assertEquals("jdbc:mysql://localhost:3306/java_quiz_platform", config.getUrl());
        assertEquals("local_user", config.getUser());
        assertEquals("local_pass", config.getPassword());
    }
}
