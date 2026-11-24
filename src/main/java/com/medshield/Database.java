package com.medshield;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:medshield.db";

    public static Connection connect() throws Exception {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    username TEXT NOT NULL UNIQUE,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL
                )
                """;

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            String recordsTable = """
                    CREATE TABLE IF NOT EXISTS medical_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        patient_name TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        blood_group TEXT,
                        allergies TEXT,
                        medical_history TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """;

            stmt.execute(recordsTable);
            String prescriptionTable = """
                    CREATE TABLE IF NOT EXISTS prescriptions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        patient_name TEXT NOT NULL,
                        doctor_name TEXT NOT NULL,
                        medicine_name TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        end_date TEXT NOT NULL,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """;

            stmt.execute(prescriptionTable);
            String reminderTable = """
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        medicine_name TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        reminder_date TEXT NOT NULL,
                        reminder_time TEXT NOT NULL,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """;

            stmt.execute(reminderTable);

            System.out.println("Medicine Reminders Table Created Successfully!");

            System.out.println("Prescriptions Table Created Successfully!");

            System.out.println("Medical Records Table Created Successfully!");
            System.out.println("SQLite Database Connected!");
            System.out.println("Users Table Created Successfully!");

        } catch (Exception e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }
}
