package com.medshield;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ReminderHandler {

    public static void save(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        Integer userId = AuthHandler.getLoggedInUserId(exchange);

        if (userId == null) {
            sendResponse(exchange, 401,
                    "{\"message\":\"Please login first\"}");
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

            String medicine = data.get("medicine");
            String dosage = data.get("dosage");
            String date = data.get("date");
            String time = data.get("time");

            if (medicine == null || medicine.isBlank()
                    || dosage == null || dosage.isBlank()
                    || date == null || time == null) {

                sendResponse(exchange, 400,
                        "{\"message\":\"Please fill all fields\"}");
                return;
            }

            LocalDate.parse(date);
            LocalTime.parse(time);

            String sql = """
                    INSERT INTO reminders
                    (user_id, medicine_name, dosage,
                     reminder_date, reminder_time)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (Connection conn = Database.connect();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, medicine);
                stmt.setString(3, dosage);
                stmt.setString(4, date);
                stmt.setString(5, time);

                stmt.executeUpdate();
            }

            sendResponse(exchange, 201,
                    "{\"message\":\"Reminder saved successfully!\"}");

        } catch (Exception e) {
            e.printStackTrace();

            sendResponse(exchange, 400,
                    "{\"message\":\"Could not save reminder. Check entered details.\"}");
        }
    }

    public static void list(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {
            sendResponse(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        Integer userId = AuthHandler.getLoggedInUserId(exchange);

        if (userId == null) {
            sendResponse(exchange, 401,
                    "{\"message\":\"Please login first\"}");
            return;
        }

        String sql = """
                SELECT id, medicine_name, dosage,
                       reminder_date, reminder_time
                FROM reminders
                WHERE user_id = ?
                ORDER BY reminder_date, reminder_time
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

                    String date = rs.getString("reminder_date");
                    String time = rs.getString("reminder_time");

                    LocalDateTime reminderDateTime = LocalDateTime.of(
                            LocalDate.parse(date),
                            LocalTime.parse(time));

                    String status = reminderDateTime.isAfter(
                            LocalDateTime.now()) ? "Scheduled" : "Due";

                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"medicine\":\"")
                            .append(escapeJson(rs.getString("medicine_name"))).append("\",")
                            .append("\"dosage\":\"")
                            .append(escapeJson(rs.getString("dosage"))).append("\",")
                            .append("\"date\":\"").append(escapeJson(date)).append("\",")
                            .append("\"time\":\"").append(escapeJson(time)).append("\",")
                            .append("\"status\":\"").append(status).append("\"")
                            .append("}");
                }

                json.append("]");

                sendResponse(exchange, 200, json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();

            sendResponse(exchange, 500,
                    "{\"message\":\"Could not load reminders\"}");
        }
    }

    private static String escapeJson(String value) {
        if (value == null)
            return "";

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void sendResponse(
            HttpExchange exchange, int status, String json)
            throws IOException {

        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
