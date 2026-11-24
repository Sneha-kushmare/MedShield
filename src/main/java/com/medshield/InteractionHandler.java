package com.medshield;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class InteractionHandler {

    private static final Map<String, String[]> DEMO_DATABASE = new HashMap<>();

    static {
        DEMO_DATABASE.put(
                "demomed-a|demomed-b",
                new String[] { "HIGH", "Illustrative high-severity demo entry." });

        DEMO_DATABASE.put(
                "demomed-a|demomed-c",
                new String[] { "MODERATE", "Illustrative moderate-severity demo entry." });

        DEMO_DATABASE.put(
                "demomed-b|demomed-c",
                new String[] { "LOW", "Illustrative low-severity demo entry." });
    }

    public static void check(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
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

            String medicine1 = data.get("medicine1");
            String medicine2 = data.get("medicine2");

            if (medicine1 == null || medicine2 == null
                    || medicine1.isBlank() || medicine2.isBlank()) {

                sendResponse(exchange, 400,
                        "{\"message\":\"Please enter both medicines\"}");
                return;
            }

            if (medicine1.equalsIgnoreCase(medicine2)) {
                sendResponse(exchange, 400,
                        "{\"message\":\"Enter two different medicines\"}");
                return;
            }

            String first = medicine1.trim().toLowerCase();
            String second = medicine2.trim().toLowerCase();

            String key = first.compareTo(second) < 0
                    ? first + "|" + second
                    : second + "|" + first;

            String[] result = DEMO_DATABASE.get(key);

            String json;

            if (result != null) {

                json = "{"
                        + "\"found\":true,"
                        + "\"severity\":\"" + result[0] + "\","
                        + "\"message\":\"" + result[1] + "\","
                        + "\"note\":\"Fictional educational demo only. "
                        + "Not suitable for real medication decisions.\""
                        + "}";

            } else {

                json = "{"
                        + "\"found\":false,"
                        + "\"severity\":\"UNKNOWN\","
                        + "\"message\":\"No matching entry in this limited demo database.\","
                        + "\"note\":\"No match does not mean the combination is safe. "
                        + "Consult a qualified healthcare professional.\""
                        + "}";
            }

            sendResponse(exchange, 200, json);

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(exchange, 500,
                    "{\"message\":\"Interaction check failed\"}");
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
