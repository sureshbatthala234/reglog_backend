package com.reglog;

import com.reglog.config.DatabaseConfig;
import com.reglog.controller.AuthController;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ReglogServer {
    public static void main(String[] args) {
        int port = DatabaseConfig.getServerPort();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Authentication API endpoints
            server.createContext("/api/auth", new AuthController());

            // Static Frontend UI handler (serves Sunshine Classmorphism UI)
            server.createContext("/", new com.reglog.controller.StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("============================================================");
            System.out.println("  RegLog Authentication & Registration Service (Java Backend)");
            System.out.println("============================================================");
            System.out.println("  Server is listening on port: " + port);
            System.out.println("  Database connected: MySQL (localhost:3306 / reglog_db)");
            System.out.println("  Base API Endpoint: http://localhost:" + port + "/api/auth");
            System.out.println("    - POST /api/auth/register");
            System.out.println("    - POST /api/auth/login");
            System.out.println("    - POST /api/auth/logout");
            System.out.println("    - POST /api/auth/forgot-password");
            System.out.println("    - POST /api/auth/verify-otp");
            System.out.println("    - POST /api/auth/reset-password");
            System.out.println("    - PUT  /api/auth/change-password");
            System.out.println("    - GET  /api/auth/me");
            System.out.println("============================================================");
            System.out.println("Press Ctrl+C to stop the server.");
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
