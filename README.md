# 🛡️ MedShield
### Medical Management and Drug Interaction Checker

## 1. Project Overview
MedShield is a web-based medical management working prototype designed to help users manage medical records, prescriptions, medicine reminders and demonstrate basic drug interaction checking.

## 2. Technologies Used
- Frontend: HTML, CSS, JavaScript
- Backend: Java
- Database: SQLite
- Database Connectivity: JDBC
- Build Tool: Apache Maven

## 3. Main Features

### User Authentication
- User Registration
- User Login
- Password Hashing
- Session Management
- Logout

### Medical Records
- Add patient information
- Store age and blood group
- Maintain allergies and medical history
- View saved records

### Prescription Management
- Add medicine prescriptions
- Store dosage and frequency
- Maintain prescription dates
- View saved prescriptions

### Drug Interaction Checker
- Enter two medicine names
- Check a limited demonstration interaction database
- Display demo severity and warning messages

### Medicine Reminders
- Schedule medicine reminders
- Save reminder date and time
- Display scheduled and due reminders
- Show in-app reminder popups while the reminder page is open

## 4. Database Tables
1. users
2. medical_records
3. prescriptions
4. reminders

## 5. How to Run

1. Install Java and Maven.
2. Open the MEDSHIELD project folder in VS Code.
3. Open the integrated terminal.
4. Compile the project:

   mvn clean compile

5. Start the application:

   mvn exec:java

6. Open the browser:

   http://localhost:8080

## 6. Project Limitations
- Drug interaction checking uses fictional demonstration data and is not suitable for clinical decisions.
- Reminder notifications work while the reminder page is open.
- Sessions are stored in server memory and are cleared when the server restarts.
- This is an educational working prototype, not a production healthcare application.

## 7. Project Objective
To demonstrate how web technologies, Java backend programming and database connectivity can be combined to develop a basic medical management system.