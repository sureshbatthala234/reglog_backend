package com.reglog.service;

import com.reglog.config.DatabaseConfig;
import com.reglog.model.User;
import com.reglog.security.JwtUtil;
import com.reglog.security.PasswordUtil;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\d{10}$");

    public Map<String, Object> register(String fullName, String email, String mobileNumber, String password, String confirmPassword) {
        Map<String, Object> response = new HashMap<>();

        // Validation: Required fields
        if (fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            mobileNumber == null || mobileNumber.trim().isEmpty() ||
            password == null || password.isEmpty() ||
            confirmPassword == null || confirmPassword.isEmpty()) {
            response.put("error", "All fields are required.");
            response.put("status", 400);
            return response;
        }

        fullName = fullName.trim();
        email = email.trim().toLowerCase();
        mobileNumber = mobileNumber.trim();

        // Validation: Email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            response.put("error", "Please provide a valid email address.");
            response.put("status", 400);
            return response;
        }

        // Validation: Exactly 10 mobile digits
        if (!MOBILE_PATTERN.matcher(mobileNumber).matches()) {
            response.put("error", "Mobile number must be exactly 10 digits.");
            response.put("status", 400);
            return response;
        }

        // Validation: Matching passwords
        if (!password.equals(confirmPassword)) {
            response.put("error", "Passwords do not match.");
            response.put("status", 400);
            return response;
        }

        // Validation: Password complexity (8+ chars, upper, lower, digit, special)
        if (!PasswordUtil.isValidPassword(password)) {
            response.put("error", "Password must be at least 8 characters and include uppercase, lowercase, numeric and special characters.");
            response.put("status", 400);
            return response;
        }

        // Uniqueness check: Email and Mobile
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement checkEmail = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                checkEmail.setString(1, email);
                try (ResultSet rs = checkEmail.executeQuery()) {
                    if (rs.next()) {
                        response.put("error", "Email address is already registered.");
                        response.put("status", 409);
                        return response;
                    }
                }
            }

            try (PreparedStatement checkMobile = conn.prepareStatement("SELECT id FROM users WHERE mobile_number = ?")) {
                checkMobile.setString(1, mobileNumber);
                try (ResultSet rs = checkMobile.executeQuery()) {
                    if (rs.next()) {
                        response.put("error", "Mobile number is already registered.");
                        response.put("status", 409);
                        return response;
                    }
                }
            }

            // Hash password with BCrypt
            String passwordHash = PasswordUtil.hashPassword(password);

            // Insert into users
            String insertSql = "INSERT INTO users (full_name, email, mobile_number, password_hash) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, fullName);
                insertStmt.setString(2, email);
                insertStmt.setString(3, mobileNumber);
                insertStmt.setString(4, passwordHash);
                insertStmt.executeUpdate();

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long newId = generatedKeys.getLong(1);
                        response.put("message", "User registered successfully! Please log in.");
                        response.put("userId", newId);
                        response.put("status", 201);
                        return response;
                    }
                }
            }
        } catch (SQLException e) {
            response.put("error", "Database error during registration: " + e.getMessage());
            response.put("status", 500);
            return response;
        }

        response.put("error", "Failed to register user.");
        response.put("status", 500);
        return response;
    }

    public Map<String, Object> login(String identifier, String password) {
        Map<String, Object> response = new HashMap<>();

        if (identifier == null || identifier.trim().isEmpty() || password == null || password.isEmpty()) {
            response.put("error", "Invalid credentials.");
            response.put("status", 401);
            return response;
        }

        identifier = identifier.trim();

        try (Connection conn = DatabaseConfig.getConnection()) {
            String selectSql = "SELECT id, full_name, email, mobile_number, password_hash FROM users WHERE email = ? OR mobile_number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, identifier.toLowerCase());
                stmt.setString(2, identifier);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        // Generic error message that does not reveal whether email/mobile or password was wrong
                        response.put("error", "Invalid credentials.");
                        response.put("status", 401);
                        return response;
                    }

                    long userId = rs.getLong("id");
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email");
                    String mobile = rs.getString("mobile_number");
                    String storedHash = rs.getString("password_hash");

                    // Check password
                    if (!PasswordUtil.verifyPassword(password, storedHash)) {
                        response.put("error", "Invalid credentials.");
                        response.put("status", 401);
                        return response;
                    }

                    // Generate JWT and session
                    String jti = UUID.randomUUID().toString();
                    String token = JwtUtil.generateToken(userId, email, mobile, jti);

                    long now = System.currentTimeMillis();
                    long exp = now + DatabaseConfig.getJwtExpirationMs();

                    String sessionSql = "INSERT INTO user_sessions (user_id, token_identifier, login_time, expiry_time, revocation_status) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement sessionStmt = conn.prepareStatement(sessionSql)) {
                        sessionStmt.setLong(1, userId);
                        sessionStmt.setString(2, jti);
                        sessionStmt.setTimestamp(3, new Timestamp(now));
                        sessionStmt.setTimestamp(4, new Timestamp(exp));
                        sessionStmt.setBoolean(5, false);
                        sessionStmt.executeUpdate();
                    }

                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", userId);
                    userInfo.put("fullName", fullName);
                    userInfo.put("email", email);
                    userInfo.put("mobileNumber", mobile);

                    response.put("message", "Login successful");
                    response.put("token", token);
                    response.put("user", userInfo);
                    response.put("status", 200);
                    return response;
                }
            }
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }
    }

    public Map<String, Object> logout(String token) {
        Map<String, Object> response = new HashMap<>();
        JwtUtil.JwtClaims claims = JwtUtil.validateAndParseToken(token);
        if (claims == null) {
            response.put("error", "Invalid or expired token.");
            response.put("status", 401);
            return response;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE user_sessions SET revocation_status = TRUE WHERE token_identifier = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, claims.jti);
                stmt.executeUpdate();
            }
            response.put("message", "Logged out successfully");
            response.put("status", 200);
            return response;
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }
    }

    public Map<String, Object> getMe(String token) {
        Map<String, Object> response = new HashMap<>();
        JwtUtil.JwtClaims claims = JwtUtil.validateAndParseToken(token);
        if (claims == null) {
            response.put("error", "Unauthorized: Invalid or expired token.");
            response.put("status", 401);
            return response;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Verify session revocation
            String sessionCheck = "SELECT revocation_status, expiry_time FROM user_sessions WHERE token_identifier = ?";
            try (PreparedStatement sStmt = conn.prepareStatement(sessionCheck)) {
                sStmt.setString(1, claims.jti);
                try (ResultSet sRs = sStmt.executeQuery()) {
                    if (!sRs.next() || sRs.getBoolean("revocation_status")) {
                        response.put("error", "Unauthorized: Token revoked or session ended.");
                        response.put("status", 401);
                        return response;
                    }
                }
            }

            // Fetch user profile
            String userSql = "SELECT id, full_name, email, mobile_number, created_date FROM users WHERE id = ?";
            try (PreparedStatement uStmt = conn.prepareStatement(userSql)) {
                uStmt.setLong(1, claims.userId);
                try (ResultSet uRs = uStmt.executeQuery()) {
                    if (uRs.next()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", uRs.getLong("id"));
                        user.put("fullName", uRs.getString("full_name"));
                        user.put("email", uRs.getString("email"));
                        user.put("mobileNumber", uRs.getString("mobile_number"));
                        user.put("createdDate", uRs.getTimestamp("created_date").toString());
                        response.put("user", user);
                        response.put("status", 200);
                        return response;
                    }
                }
            }
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }

        response.put("error", "User not found.");
        response.put("status", 404);
        return response;
    }

    public Map<String, Object> forgotPassword(String identifier) {
        Map<String, Object> response = new HashMap<>();
        if (identifier == null || identifier.trim().isEmpty()) {
            response.put("error", "Email or mobile number is required.");
            response.put("status", 400);
            return response;
        }

        identifier = identifier.trim();

        try (Connection conn = DatabaseConfig.getConnection()) {
            String checkSql = "SELECT id FROM users WHERE email = ? OR mobile_number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, identifier.toLowerCase());
                stmt.setString(2, identifier);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        response.put("error", "No user found with the provided email or mobile number.");
                        response.put("status", 404);
                        return response;
                    }
                }
            }

            // Generate 6-digit OTP
            String otp = String.format("%06d", new Random().nextInt(999999));
            long expiryTime = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes

            String insertOtp = "INSERT INTO otp_verifications (identifier, otp_code, expiry_time, verified) VALUES (?, ?, ?, false)";
            try (PreparedStatement otpStmt = conn.prepareStatement(insertOtp)) {
                otpStmt.setString(1, identifier.toLowerCase());
                otpStmt.setString(2, otp);
                otpStmt.setTimestamp(3, new Timestamp(expiryTime));
                otpStmt.executeUpdate();
            }

            // Console output for OTP (configurable SMS/Email provider requirement)
            System.out.println("==================================================");
            System.out.println("[OTP NOTIFICATION] Destination: " + identifier);
            System.out.println("[OTP CODE]: " + otp + " (Expires in 10 minutes)");
            System.out.println("==================================================");

            response.put("message", "OTP generated successfully. (In local development, check terminal console)");
            response.put("otp", otp); // included for seamless local development & testing
            response.put("status", 200);
            return response;
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }
    }

    public Map<String, Object> verifyOtp(String identifier, String otp) {
        Map<String, Object> response = new HashMap<>();
        if (identifier == null || otp == null || otp.trim().isEmpty()) {
            response.put("error", "Identifier and OTP are required.");
            response.put("status", 400);
            return response;
        }

        identifier = identifier.trim().toLowerCase();
        otp = otp.trim();

        try (Connection conn = DatabaseConfig.getConnection()) {
            String checkSql = "SELECT id, expiry_time FROM otp_verifications WHERE identifier = ? AND otp_code = ? AND verified = false ORDER BY id DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, identifier);
                stmt.setString(2, otp);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        response.put("error", "Invalid OTP.");
                        response.put("status", 400);
                        return response;
                    }

                    long id = rs.getLong("id");
                    Timestamp exp = rs.getTimestamp("expiry_time");
                    if (System.currentTimeMillis() > exp.getTime()) {
                        response.put("error", "OTP has expired. Please request a new one.");
                        response.put("status", 400);
                        return response;
                    }

                    // Mark as verified
                    try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE otp_verifications SET verified = true WHERE id = ?")) {
                        updateStmt.setLong(1, id);
                        updateStmt.executeUpdate();
                    }

                    response.put("message", "OTP verified successfully.");
                    response.put("verified", true);
                    response.put("status", 200);
                    return response;
                }
            }
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }
    }

    public Map<String, Object> resetPassword(String identifier, String otp, String newPassword, String confirmPassword) {
        Map<String, Object> response = new HashMap<>();
        if (identifier == null || otp == null || newPassword == null || confirmPassword == null) {
            response.put("error", "All fields are required.");
            response.put("status", 400);
            return response;
        }

        identifier = identifier.trim().toLowerCase();

        if (!newPassword.equals(confirmPassword)) {
            response.put("error", "Passwords do not match.");
            response.put("status", 400);
            return response;
        }

        if (!PasswordUtil.isValidPassword(newPassword)) {
            response.put("error", "Password must be at least 8 characters and include uppercase, lowercase, numeric and special characters.");
            response.put("status", 400);
            return response;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check verified OTP
            String otpSql = "SELECT id FROM otp_verifications WHERE identifier = ? AND otp_code = ? AND verified = true ORDER BY id DESC LIMIT 1";
            long otpId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(otpSql)) {
                stmt.setString(1, identifier);
                stmt.setString(2, otp.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        response.put("error", "OTP not verified or invalid.");
                        response.put("status", 400);
                        return response;
                    }
                    otpId = rs.getLong("id");
                }
            }

            // Update user password
            String hash = PasswordUtil.hashPassword(newPassword);
            String updatePass = "UPDATE users SET password_hash = ? WHERE email = ? OR mobile_number = ?";
            try (PreparedStatement uStmt = conn.prepareStatement(updatePass)) {
                uStmt.setString(1, hash);
                uStmt.setString(2, identifier);
                uStmt.setString(3, identifier);
                int updated = uStmt.executeUpdate();
                if (updated == 0) {
                    response.put("error", "Failed to update password for account.");
                    response.put("status", 404);
                    return response;
                }
            }

            // Invalidate all existing sessions for this user
            String revokeSessions = "UPDATE user_sessions SET revocation_status = true WHERE user_id = (SELECT id FROM users WHERE email = ? OR mobile_number = ?)";
            try (PreparedStatement rStmt = conn.prepareStatement(revokeSessions)) {
                rStmt.setString(1, identifier);
                rStmt.setString(2, identifier);
                rStmt.executeUpdate();
            }

            // Clean up OTP record
            try (PreparedStatement delStmt = conn.prepareStatement("DELETE FROM otp_verifications WHERE id = ?")) {
                delStmt.setLong(1, otpId);
                delStmt.executeUpdate();
            }

            response.put("message", "Password reset successfully. You can now login with your new password.");
            response.put("status", 200);
            return response;
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }
    }

    public Map<String, Object> changePassword(String token, String currentPassword, String newPassword, String confirmPassword) {
        Map<String, Object> response = new HashMap<>();
        JwtUtil.JwtClaims claims = JwtUtil.validateAndParseToken(token);
        if (claims == null) {
            response.put("error", "Unauthorized: Invalid or expired token.");
            response.put("status", 401);
            return response;
        }

        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            response.put("error", "All fields are required.");
            response.put("status", 400);
            return response;
        }

        if (!newPassword.equals(confirmPassword)) {
            response.put("error", "New passwords do not match.");
            response.put("status", 400);
            return response;
        }

        if (!PasswordUtil.isValidPassword(newPassword)) {
            response.put("error", "Password must be at least 8 characters and include uppercase, lowercase, numeric and special characters.");
            response.put("status", 400);
            return response;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check session
            String sessionSql = "SELECT revocation_status FROM user_sessions WHERE token_identifier = ?";
            try (PreparedStatement sStmt = conn.prepareStatement(sessionSql)) {
                sStmt.setString(1, claims.jti);
                try (ResultSet sRs = sStmt.executeQuery()) {
                    if (!sRs.next() || sRs.getBoolean("revocation_status")) {
                        response.put("error", "Unauthorized: Session revoked.");
                        response.put("status", 401);
                        return response;
                    }
                }
            }

            // Check current password
            String userSql = "SELECT password_hash FROM users WHERE id = ?";
            String storedHash = null;
            try (PreparedStatement uStmt = conn.prepareStatement(userSql)) {
                uStmt.setLong(1, claims.userId);
                try (ResultSet uRs = uStmt.executeQuery()) {
                    if (uRs.next()) {
                        storedHash = uRs.getString("password_hash");
                    }
                }
            }

            if (storedHash == null || !PasswordUtil.verifyPassword(currentPassword, storedHash)) {
                response.put("error", "Current password is incorrect.");
                response.put("status", 400);
                return response;
            }

            // Update with new password hash
            String newHash = PasswordUtil.hashPassword(newPassword);
            try (PreparedStatement upStmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
                upStmt.setString(1, newHash);
                upStmt.setLong(2, claims.userId);
                upStmt.executeUpdate();
            }

            // Invalidate other active sessions
            try (PreparedStatement rStmt = conn.prepareStatement("UPDATE user_sessions SET revocation_status = true WHERE user_id = ? AND token_identifier != ?")) {
                rStmt.setLong(1, claims.userId);
                rStmt.setString(2, claims.jti);
                rStmt.executeUpdate();
            }

            response.put("message", "Password changed successfully.");
            response.put("status", 200);
            return response;
        } catch (SQLException e) {
            response.put("error", "Database error: " + e.getMessage());
            response.put("status", 500);
            return response;
        }
    }
}
