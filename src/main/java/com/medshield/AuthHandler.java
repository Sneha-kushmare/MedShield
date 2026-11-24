package com.medshield;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.spec.PBEKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

public class AuthHandler {
    private static final Map<String, Integer> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    public static void register(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            InputStream input = exchange.getRequestBody();

            String body = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8);

            Map<String, String> data = new HashMap<>();

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);

                if (parts.length == 2) {
                    data.put(
                            URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }

            String fullName = data.get("fullName");
            String username = data.get("username");
            String email = data.get("email");
            String password = data.get("password");

            if (fullName == null || username == null
                    || email == null || password == null
                    || fullName.isBlank() || username.isBlank()
                    || email.isBlank() || password.length() < 6) {

                sendResponse(exchange, 400, "Please enter valid details");
                return;
            }

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(), salt, 210000, 256);

            byte[] hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            spec.clearPassword();

            String passwordHash = Base64.getEncoder()
                    .encodeToString(hash);

            String passwordSalt = Base64.getEncoder()
                    .encodeToString(salt);

            String sql = """
                    INSERT INTO users
                    (full_name, username, email, password_hash, password_salt)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (Connection conn = Database.connect();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, fullName);
                stmt.setString(2, username);
                stmt.setString(3, email);
                stmt.setString(4, passwordHash);
                stmt.setString(5, passwordSalt);

                stmt.executeUpdate();
            }

            sendResponse(exchange, 201, "Registration successful!");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400,
                    "Registration failed. Username or email may already exist.");
        }
    }

    public static void login(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            Map<String, String> data = new HashMap<>();

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);

                if (parts.length == 2) {
                    data.put(
                            URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }

            String username = data.get("username");
            String password = data.get("password");

            if (username == null || password == null
                    || username.isBlank() || password.isBlank()) {

                sendResponse(exchange, 400, "Enter username and password");
                return;
            }

            String sql = """
                    SELECT id, password_hash, password_salt
                    FROM users
                    WHERE username = ?
                    """;

            try (Connection conn = Database.connect();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        sendResponse(exchange, 401,
                                "Invalid username or password");
                        return;
                    }

                    byte[] expectedHash = Base64.getDecoder()
                            .decode(rs.getString("password_hash"));

                    byte[] salt = Base64.getDecoder()
                            .decode(rs.getString("password_salt"));

                    PBEKeySpec spec = new PBEKeySpec(
                            password.toCharArray(), salt, 210000, 256);

                    byte[] actualHash = SecretKeyFactory
                            .getInstance("PBKDF2WithHmacSHA256")
                            .generateSecret(spec)
                            .getEncoded();

                    spec.clearPassword();

                    if (MessageDigest.isEqual(expectedHash, actualHash)) {

                        // Generate a secure session token
                        byte[] tokenBytes = new byte[32];
                        new SecureRandom().nextBytes(tokenBytes);

                        String token = Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(tokenBytes);

                        // Store token with user ID
                        int userId = rs.getInt("id");
                        ACTIVE_SESSIONS.put(token, userId);

                        // Send HttpOnly session cookie
                        exchange.getResponseHeaders().add(
                                "Set-Cookie",
                                "MEDSHIELD_SESSION=" + token
                                        + "; HttpOnly; SameSite=Strict; Path=/");

                        sendResponse(exchange, 200, "Login successful!");

                    } else {
                        sendResponse(exchange, 401,
                                "Invalid username or password");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Login could not be completed");
        }
    }

    public static Integer getLoggedInUserId(HttpExchange exchange) {

        String cookieHeader = exchange.getRequestHeaders()
                .getFirst("Cookie");

        if (cookieHeader == null) {
            return null;
        }

        for (String cookie : cookieHeader.split(";")) {

            String[] parts = cookie.trim().split("=", 2);

            if (parts.length == 2
                    && parts[0].equals("MEDSHIELD_SESSION")) {

                return ACTIVE_SESSIONS.get(parts[1]);
            }
        }

        return null;
    }

    private static void sendResponse(
            HttpExchange exchange, int status, String message)
            throws IOException {

        String json = "{\"message\":\"" + message + "\"}";

        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    public static void logout(HttpExchange exchange)
            throws IOException {

        String cookieHeader = exchange.getRequestHeaders()
                .getFirst("Cookie");

        if (cookieHeader != null) {

            for (String cookie : cookieHeader.split(";")) {

                String[] parts = cookie.trim().split("=", 2);

                if (parts.length == 2
                        && parts[0].equals("MEDSHIELD_SESSION")) {

                    ACTIVE_SESSIONS.remove(parts[1]);
                    break;
                }
            }
        }

        exchange.getResponseHeaders().add(
                "Set-Cookie",
                "MEDSHIELD_SESSION=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");

        exchange.getResponseHeaders().set(
                "Location", "/index.html");

        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

}
