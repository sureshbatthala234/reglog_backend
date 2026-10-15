package com.reglog.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StaticFileHandler implements HttpHandler {
    private final File frontendDir;

    public StaticFileHandler() {
        File dir = new File("../reglog_frontend");
        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File("reglog_frontend");
        }
        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File("C:/java practice/patterns/reglog_frontend");
        }
        this.frontendDir = dir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/api/")) {
            return;
        }

        File targetFile;
        if (path.equals("/") || path.equals("/login") || path.equals("/register") || path.equals("/home") || path.equals("/forgot-password")) {
            targetFile = new File(frontendDir, "index.html");
        } else {
            targetFile = new File(frontendDir, path.startsWith("/") ? path.substring(1) : path);
            if (!targetFile.exists() || targetFile.isDirectory()) {
                targetFile = new File(frontendDir, "index.html");
            }
        }

        if (!targetFile.exists() || targetFile.isDirectory()) {
            String notFound = "404 Not Found";
            exchange.sendResponseHeaders(404, notFound.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound.getBytes());
            }
            return;
        }

        String mime = "text/plain";
        String name = targetFile.getName().toLowerCase();
        if (name.endsWith(".html")) mime = "text/html; charset=UTF-8";
        else if (name.endsWith(".css")) mime = "text/css; charset=UTF-8";
        else if (name.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
        else if (name.endsWith(".json")) mime = "application/json";
        else if (name.endsWith(".svg")) mime = "image/svg+xml";

        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, targetFile.length());

        try (FileInputStream fis = new FileInputStream(targetFile); OutputStream os = exchange.getResponseBody()) {
            fis.transferTo(os);
        }
    }
}
