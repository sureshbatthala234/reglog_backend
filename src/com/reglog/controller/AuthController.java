package com.reglog.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.reglog.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AuthController implements HttpHandler {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle CORS Preflight OPTIONS
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("/api/auth/register".equals(path) && "POST".equals(method)) {
                handleRegister(exchange);
            } else if ("/api/auth/login".equals(path) && "POST".equals(method)) {
                handleLogin(exchange);
            } else if ("/api/auth/logout".equals(path) && "POST".equals(method)) {
                handleLogout(exchange);
            } else if ("/api/auth/forgot-password".equals(path) && "POST".equals(method)) {
                handleForgotPassword(exchange);
            } else if ("/api/auth/verify-otp".equals(path) && "POST".equals(method)) {
                handleVerifyOtp(exchange);
            } else if ("/api/auth/reset-password".equals(path) && "POST".equals(method)) {
                handleResetPassword(exchange);
            } else if ("/api/auth/change-password".equals(path) && ("PUT".equals(method) || "POST".equals(method))) {
                handleChangePassword(exchange);
            } else if ("/api/auth/me".equals(path) && "GET".equals(method)) {
                handleGetMe(exchange);
            } else {
                sendJsonResponse(exchange, 404, Map.of("error", "Endpoint not found: " + path));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        JsonObject body = parseJsonBody(exchange);
        String fullName = getString(body, "fullName");
        String email = getString(body, "email");
        String mobile = getString(body, "mobileNumber");
        String password = getString(body, "password");
        String confirmPassword = getString(body, "confirmPassword");

        Map<String, Object> result = authService.register(fullName, email, mobile, password, confirmPassword);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonObject body = parseJsonBody(exchange);
        String identifier = getString(body, "identifier");
        if (identifier == null) {
            identifier = getString(body, "email");
        }
        if (identifier == null) {
            identifier = getString(body, "mobileNumber");
        }
        String password = getString(body, "password");

        Map<String, Object> result = authService.login(identifier, password);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = extractBearerToken(exchange);
        Map<String, Object> result = authService.logout(token);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleGetMe(HttpExchange exchange) throws IOException {
        String token = extractBearerToken(exchange);
        Map<String, Object> result = authService.getMe(token);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleForgotPassword(HttpExchange exchange) throws IOException {
        JsonObject body = parseJsonBody(exchange);
        String identifier = getString(body, "identifier");
        if (identifier == null) identifier = getString(body, "email");
        if (identifier == null) identifier = getString(body, "mobileNumber");

        Map<String, Object> result = authService.forgotPassword(identifier);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleVerifyOtp(HttpExchange exchange) throws IOException {
        JsonObject body = parseJsonBody(exchange);
        String identifier = getString(body, "identifier");
        String otp = getString(body, "otp");

        Map<String, Object> result = authService.verifyOtp(identifier, otp);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleResetPassword(HttpExchange exchange) throws IOException {
        JsonObject body = parseJsonBody(exchange);
        String identifier = getString(body, "identifier");
        String otp = getString(body, "otp");
        String newPassword = getString(body, "newPassword");
        String confirmPassword = getString(body, "confirmPassword");

        Map<String, Object> result = authService.resetPassword(identifier, otp, newPassword, confirmPassword);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private void handleChangePassword(HttpExchange exchange) throws IOException {
        String token = extractBearerToken(exchange);
        JsonObject body = parseJsonBody(exchange);
        String currentPassword = getString(body, "currentPassword");
        String newPassword = getString(body, "newPassword");
        String confirmPassword = getString(body, "confirmPassword");

        Map<String, Object> result = authService.changePassword(token, currentPassword, newPassword, confirmPassword);
        int status = (int) result.getOrDefault("status", 200);
        sendJsonResponse(exchange, status, result);
    }

    private String extractBearerToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private JsonObject parseJsonBody(HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (json.trim().isEmpty()) return new JsonObject();
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        String responseJson = gson.toJson(data);
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
