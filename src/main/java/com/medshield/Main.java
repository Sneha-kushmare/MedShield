package com.medshield;

import com.sun.net.httpserver.HttpServer;
import com.medshield.MedicalRecordHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        Database.initialize();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        server.createContext("/api/register", AuthHandler::register);
        server.createContext("/api/login", AuthHandler::login);
        server.createContext("/api/logout", AuthHandler::logout);
        server.createContext("/api/medical-records/save", MedicalRecordHandler::save);
        server.createContext("/api/medical-records/list", MedicalRecordHandler::list);
        server.createContext("/api/prescriptions/save", PrescriptionHandler::save);
        server.createContext("/api/prescriptions/list", PrescriptionHandler::list);
        server.createContext("/api/interaction/check", InteractionHandler::check);
        server.createContext("/api/reminders/save", ReminderHandler::save);
        server.createContext("/api/reminders/list", ReminderHandler::list);
        server.createContext("/api/dashboard/stats", DashboardHandler::stats);
        server.createContext("/api/dashboard/profile", DashboardHandler::profile);
        Path root = Path.of("").toAbsolutePath().normalize();

        server.createContext("/", exchange -> {

            String requestPath = exchange.getRequestURI().getPath();

            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }
            boolean requiresLogin = requestPath.equals("/dashboard.html")
                    || requestPath.equals("/medical-records.html")
                    || requestPath.equals("/prescription.html")
                    || requestPath.equals("/interaction.html")
                    || requestPath.equals("/reminders.html");

            if (requiresLogin && AuthHandler.getLoggedInUserId(exchange) == null) {

                exchange.getResponseHeaders().set(
                        "Location", "/index.html");

                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            Path file = root.resolve(requestPath.substring(1)).normalize();

            if (!file.startsWith(root) || !Files.exists(file)
                    || Files.isDirectory(file)) {

                byte[] error = "404 - File Not Found"
                        .getBytes();

                exchange.sendResponseHeaders(404, error.length);
                exchange.getResponseBody().write(error);
                exchange.close();
                return;
            }

            String contentType = "text/html";

            if (requestPath.endsWith(".css")) {
                contentType = "text/css";
            } else if (requestPath.endsWith(".js")) {
                contentType = "application/javascript";
            }

            exchange.getResponseHeaders().set(
                    "Content-Type", contentType + "; charset=UTF-8");

            byte[] response = Files.readAllBytes(file);

            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        server.start();

        System.out.println(
                "MedShield running at http://localhost:8080");
    }
}
