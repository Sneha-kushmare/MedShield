package com.medshield;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.sql.ResultSet;

public class MedicalRecordHandler {

    public static void save(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        Integer userId = AuthHandler.getLoggedInUserId(exchange);

        if (userId == null) {
            sendResponse(exchange, 401, "Please login first");
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

            String patientName = data.get("patientName");
            String ageText = data.get("age");
            String bloodGroup = data.get("bloodGroup");
            String allergies = data.get("allergies");
            String medicalHistory = data.get("medicalHistory");

            if (patientName == null || patientName.isBlank()
                    || ageText == null) {

                sendResponse(exchange, 400, "Please enter patient name and age");
                return;
            }

            int age = Integer.parseInt(ageText);

            if (age < 1 || age > 120) {
                sendResponse(exchange, 400, "Please enter a valid age");
                return;
            }

            String sql = """
                    INSERT INTO medical_records
                    (user_id, patient_name, age, blood_group,
                     allergies, medical_history)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = Database.connect();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, patientName);
                stmt.setInt(3, age);
                stmt.setString(4, bloodGroup);
                stmt.setString(5, allergies);
                stmt.setString(6, medicalHistory);

                stmt.executeUpdate();
            }

            sendResponse(exchange, 201, "Medical record saved successfully!");

        } catch (NumberFormatException e) {

            sendResponse(exchange, 400, "Age must be a valid number");

        } catch (Exception e) {

            e.printStackTrace();
            sendResponse(exchange, 500, "Could not save medical record");
        }
    }

    public static void list(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        Integer userId = AuthHandler.getLoggedInUserId(exchange);

        if (userId == null) {
            sendResponse(exchange, 401, "Please login first");
            return;
        }

        String sql = """
                SELECT id, patient_name, age, blood_group,
                       allergies, medical_history, created_at
                FROM medical_records
                WHERE user_id = ?
                ORDER BY id DESC
                """;

        try (Connection conn = Database.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"patientName\":\"")
                            .append(escapeJson(rs.getString("patient_name"))).append("\",")
                            .append("\"age\":").append(rs.getInt("age")).append(",")
                            .append("\"bloodGroup\":\"")
                            .append(escapeJson(rs.getString("blood_group"))).append("\",")
                            .append("\"allergies\":\"")
                            .append(escapeJson(rs.getString("allergies"))).append("\",")
                            .append("\"medicalHistory\":\"")
                            .append(escapeJson(rs.getString("medical_history"))).append("\",")
                            .append("\"createdAt\":\"")
                            .append(escapeJson(rs.getString("created_at"))).append("\"")
                            .append("}");
                }

                json.append("]");

                byte[] response = json.toString()
                        .getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type", "application/json; charset=UTF-8");

                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Could not load medical records");
        }
    }

    private static String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
}
