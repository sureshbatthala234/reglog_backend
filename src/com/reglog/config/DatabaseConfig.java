package com.reglog.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConfig {
    private static String url = "jdbc:mysql://localhost:3306/reglog_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String username = "root";
    private static String password = "root";
    private static int serverPort = 8080;
    private static String jwtSecret = "RegLogSuperSecretKeyForJWTAuthentication2026";
    private static long jwtExpirationMs = 86400000L; // 24 hours

    static {
        loadProperties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initTables();
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("application.properties")) {
            props.load(input);
            if (props.getProperty("db.url") != null) url = props.getProperty("db.url");
            if (props.getProperty("db.username") != null) username = props.getProperty("db.username");
            if (props.getProperty("db.password") != null) password = props.getProperty("db.password");
            if (props.getProperty("server.port") != null) serverPort = Integer.parseInt(props.getProperty("server.port"));
            if (props.getProperty("jwt.secret") != null) jwtSecret = props.getProperty("jwt.secret");
            if (props.getProperty("jwt.expiration") != null) jwtExpirationMs = Long.parseLong(props.getProperty("jwt.expiration"));
        } catch (Exception e) {
            // Fallback to defaults or environment variables
            String envUrl = System.getenv("DB_URL");
            String envUser = System.getenv("DB_USERNAME");
            String envPass = System.getenv("DB_PASSWORD");
            if (envUrl != null) url = envUrl;
            if (envUser != null) username = envUser;
            if (envPass != null) password = envPass;
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static String getJwtSecret() {
        return jwtSecret;
    }

    public static long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    private static void initTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "full_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(100) NOT NULL UNIQUE," +
                    "mobile_number VARCHAR(15) NOT NULL UNIQUE," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "INDEX idx_user_email (email)," +
                    "INDEX idx_user_mobile (mobile_number)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_sessions (" +
                    "session_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "token_identifier VARCHAR(255) NOT NULL UNIQUE," +
                    "login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expiry_time TIMESTAMP NOT NULL," +
                    "revocation_status BOOLEAN DEFAULT FALSE," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "INDEX idx_session_token (token_identifier)," +
                    "INDEX idx_session_user (user_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS otp_verifications (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "identifier VARCHAR(100) NOT NULL," +
                    "otp_code VARCHAR(10) NOT NULL," +
                    "expiry_time TIMESTAMP NOT NULL," +
                    "verified BOOLEAN DEFAULT FALSE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_otp_identifier (identifier)" +
                    ")");
            System.out.println("Database tables checked/initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Database initialization notice: " + e.getMessage());
        }
    }
}
