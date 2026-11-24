package com.medshield;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PrescriptionHandler {

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

            String patient = data.get("patientName");
            String doctor = data.get("doctorName");
            String medicine = data.get("medicineName");
            String dosage = data.get("dosage");
            String frequency = data.get("frequency");
            String startDate = data.get("startDate");
            String endDate = data.get("endDate");

            if (patient == null || patient.isBlank()
                    || doctor == null || doctor.isBlank()
                    || medicine == null || medicine.isBlank()
                    || dosage == null || dosage.isBlank()
                    || frequency == null || frequency.isBlank()
                    || startDate == null || endDate == null) {

                sendResponse(exchange, 400, "Please fill all fields");
                return;
            }

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (end.isBefore(start)) {
                sendResponse(exchange, 400,
                        "End date cannot be before start date");
                return;
            }

            String sql = """
                    INSERT INTO prescriptions
                    (user_id, patient_name, doctor_name, medicine_name,
                     dosage, frequency, start_date, end_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = Database.connect();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, patient);
                stmt.setString(3, doctor);
                stmt.setString(4, medicine);
                stmt.setString(5, dosage);
                stmt.setString(6, frequency);
                stmt.setString(7, startDate);
                stmt.setString(8, endDate);

                stmt.executeUpdate();
            }

            sendResponse(exchange, 201,
                    "Prescription saved successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400,
                    "Could not save prescription. Check entered details.");
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
                SELECT patient_name, doctor_name, medicine_name,
                       dosage, frequency, start_date, end_date
                FROM prescriptions
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
                            .append("\"patientName\":\"")
                            .append(escapeJson(rs.getString("patient_name"))).append("\",")
                            .append("\"doctorName\":\"")
                            .append(escapeJson(rs.getString("doctor_name"))).append("\",")
                            .append("\"medicineName\":\"")
                            .append(escapeJson(rs.getString("medicine_name"))).append("\",")
                            .append("\"dosage\":\"")
                            .append(escapeJson(rs.getString("dosage"))).append("\",")
                            .append("\"frequency\":\"")
                            .append(escapeJson(rs.getString("frequency"))).append("\",")
                            .append("\"startDate\":\"")
                            .append(escapeJson(rs.getString("start_date"))).append("\",")
                            .append("\"endDate\":\"")
                            .append(escapeJson(rs.getString("end_date"))).append("\"")
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
            sendResponse(exchange, 500, "Could not load prescriptions");
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
