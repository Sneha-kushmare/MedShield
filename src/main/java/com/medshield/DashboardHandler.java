package com.medshield;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

public class DashboardHandler {

    public static void stats(HttpExchange exchange)
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

        try (Connection conn = Database.connect()) {

            // Count unique medicines
            String medicineSql = """
                    SELECT COUNT(DISTINCT LOWER(TRIM(medicine_name)))
                    FROM prescriptions
                    WHERE user_id = ?
                    """;

            int totalMedicines = 0;

            try (PreparedStatement stmt = conn.prepareStatement(medicineSql)) {

                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalMedicines = rs.getInt(1);
                    }
                }
            }

            // Count prescriptions
            String prescriptionSql = """
                    SELECT COUNT(*)
                    FROM prescriptions
                    WHERE user_id = ?
                    """;

            int totalPrescriptions = 0;

            try (PreparedStatement stmt = conn.prepareStatement(prescriptionSql)) {

                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalPrescriptions = rs.getInt(1);
                    }
                }
            }

            // Count upcoming reminders
            String reminderSql = """
                    SELECT reminder_date, reminder_time
                    FROM reminders
                    WHERE user_id = ?
                    """;

            int activeReminders = 0;
            LocalDateTime now = LocalDateTime.now();

            try (PreparedStatement stmt = conn.prepareStatement(reminderSql)) {

                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {

                        LocalDateTime reminder = LocalDateTime.parse(
                                rs.getString("reminder_date")
                                        + "T"
                                        + rs.getString("reminder_time"));

                        if (reminder.isAfter(now)) {
                            activeReminders++;
                        }
                    }
                }
            }

            String json = "{"
                    + "\"totalMedicines\":" + totalMedicines + ","
                    + "\"totalPrescriptions\":" + totalPrescriptions + ","
                    + "\"activeReminders\":" + activeReminders
                    + "}";

            sendResponse(exchange, 200, json);

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(exchange, 500,
                    "{\"message\":\"Could not load dashboard statistics\"}");
        }
    }

    public static void profile(HttpExchange exchange)
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

        String sql = "SELECT full_name FROM users WHERE id = ?";

        try (Connection conn = Database.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    String name = rs.getString("full_name");

                    String json = "{\"fullName\":\""
                            + name.replace("\\", "\\\\")
                                    .replace("\"", "\\\"")
                            + "\"}";

                    sendResponse(exchange, 200, json);

                } else {
                    sendResponse(exchange, 404,
                            "{\"message\":\"User not found\"}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500,
                    "{\"message\":\"Could not load profile\"}");
        }
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