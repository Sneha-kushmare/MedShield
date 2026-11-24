// LOGIN FORM
// LOGIN FORM
const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function(event) {

        event.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        if (!username || !password) {
            alert("Please enter username and password.");
            return;
        }

        try {
            const response = await fetch("/api/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    username: username,
                    password: password
                })
            });

            const result = await response.json();

            if (response.ok) {
                alert(result.message);
                window.location.href = "dashboard.html";
            } else {
                alert(result.message);
            }

        } catch (error) {
            alert("Could not connect to server. Please try again.");
            console.error(error);
        }
    });
}


// REGISTRATION FORM
const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function(event) {
        event.preventDefault();

        const fullName = document.getElementById("fullName").value.trim();
        const username = document.getElementById("newUsername").value.trim();
        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("newPassword").value;
        const confirmPassword = document.getElementById("confirmPassword").value;

        if (!fullName || !username || !email || !password || !confirmPassword) {
            alert("Please fill all fields.");
            return;
        }

        if (password.length < 6) {
            alert("Password must contain at least 6 characters.");
            return;
        }

        if (password !== confirmPassword) {
            alert("Password and Confirm Password do not match!");
            return;
        }

        const response = await fetch("/api/register", {
    method: "POST",
    headers: {
        "Content-Type": "application/x-www-form-urlencoded"
    },
    body: new URLSearchParams({
        fullName: fullName,
        username: username,
        email: email,
        password: password
    })
});

const result = await response.json();

if (response.ok) {
    alert(result.message);
    window.location.href = "index.html";
} else {
    alert(result.message);
}
    });
}
// MEDICAL RECORD FORM
const medicalForm = document.getElementById("medicalForm");

if (medicalForm) {

    medicalForm.addEventListener("submit", async function(event) {

        event.preventDefault();

        const patientName = document.getElementById("patientName").value.trim();
        const age = document.getElementById("patientAge").value;
        const bloodGroup = document.getElementById("bloodGroup").value;
        const allergies = document.getElementById("allergies").value;
        const medicalHistory = document.getElementById("medicalHistory").value;

        if (!patientName || !age) {
            alert("Please enter patient name and age.");
            return;
        }

        try {

            const response = await fetch("/api/medical-records/save", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    patientName: patientName,
                    age: age,
                    bloodGroup: bloodGroup,
                    allergies: allergies,
                    medicalHistory: medicalHistory
                })
            });

            const result = await response.json();

            if (response.ok) {

    alert(result.message);

    medicalForm.reset();

    // Refresh saved medical records
    await loadMedicalRecords();

        } else {

                alert(result.message);
            }

        } catch (error) {

            console.error(error);
            alert("Could not connect to server.");

        }
    });
}
// LOAD SAVED MEDICAL RECORDS

async function loadMedicalRecords() {

    const recordTable = document.getElementById("recordTable");

    if (!recordTable) {
        return;
    }

    try {

        const response = await fetch("/api/medical-records/list");

        if (!response.ok) {
            throw new Error("Could not load medical records");
        }

        const records = await response.json();

        recordTable.replaceChildren();

        if (records.length === 0) {

            const row = document.createElement("tr");
            const cell = document.createElement("td");

            cell.colSpan = 4;
            cell.textContent = "No records available yet.";

            row.appendChild(cell);
            recordTable.appendChild(row);

            return;
        }

        records.forEach(record => {

            const row = document.createElement("tr");

            const values = [
                record.patientName,
                record.age,
                record.bloodGroup,
                record.allergies
            ];

            values.forEach(value => {

                const cell = document.createElement("td");

                cell.textContent =
                    value === null || value === "" ? "—" : String(value);

                row.appendChild(cell);
            });

            recordTable.appendChild(row);
        });

    } catch (error) {

        console.error(error);

        recordTable.replaceChildren();

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 4;
        cell.textContent = "Unable to load medical records.";

        row.appendChild(cell);
        recordTable.appendChild(row);
    }
}

// Load records when the page opens
loadMedicalRecords();
// PRESCRIPTION FORM
const prescriptionForm = document.getElementById("prescriptionForm");

if (prescriptionForm) {

    prescriptionForm.addEventListener("submit", async function(event) {

        event.preventDefault();

        const patientName = document.getElementById("presPatient").value.trim();
        const doctorName = document.getElementById("doctorName").value.trim();
        const medicineName = document.getElementById("medicineName").value.trim();
        const dosage = document.getElementById("dosage").value.trim();
        const frequency = document.getElementById("frequency").value;
        const startDate = document.getElementById("startDate").value;
        const endDate = document.getElementById("endDate").value;

        if (endDate < startDate) {
            alert("End date cannot be before start date.");
            return;
        }

        try {

            const response = await fetch("/api/prescriptions/save", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    patientName,
                    doctorName,
                    medicineName,
                    dosage,
                    frequency,
                    startDate,
                    endDate
                })
            });

            const result = await response.json();

            if (response.ok) {

                alert(result.message);
                prescriptionForm.reset();

                await loadPrescriptions();

            } else {

                alert(result.message);
            }

        } catch (error) {

            console.error(error);
            alert("Could not connect to server.");

        }
    });
}


// LOAD SAVED PRESCRIPTIONS
async function loadPrescriptions() {

    const table = document.getElementById("prescriptionTable");

    if (!table) return;

    try {

        const response = await fetch("/api/prescriptions/list");

        const records = await response.json();

        if (!response.ok) {
            throw new Error(records.message || "Could not load prescriptions");
        }

        table.replaceChildren();

        if (records.length === 0) {

            const row = document.createElement("tr");
            const cell = document.createElement("td");

            cell.colSpan = 5;
            cell.textContent = "No prescriptions available yet.";

            row.appendChild(cell);
            table.appendChild(row);

            return;
        }

        records.forEach(record => {

            const row = document.createElement("tr");

            const values = [
                record.patientName,
                record.medicineName,
                record.dosage,
                record.frequency,
                record.doctorName
            ];

            values.forEach(value => {

                const cell = document.createElement("td");

                cell.textContent = value || "—";

                row.appendChild(cell);
            });

            table.appendChild(row);
        });

    } catch (error) {

        console.error(error);

        table.replaceChildren();

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 5;
        cell.textContent = "Unable to load prescriptions.";

        row.appendChild(cell);
        table.appendChild(row);
    }
}


// Load prescriptions when page opens
loadPrescriptions();
// DRUG INTERACTION CHECKER

const interactionForm = document.getElementById("interactionForm");

if (interactionForm) {

    interactionForm.addEventListener("submit", async function(event) {

        event.preventDefault();

        const medicine1 = document.getElementById("medicine1").value.trim();
        const medicine2 = document.getElementById("medicine2").value.trim();

        const resultBox = document.getElementById("interactionResult");

        resultBox.textContent = "Checking interaction...";

        try {

            const response = await fetch("/api/interaction/check", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    medicine1: medicine1,
                    medicine2: medicine2
                })
            });

            const result = await response.json();

            resultBox.replaceChildren();

            const heading = document.createElement("h3");
            heading.textContent = result.found
                ? "⚠️ Interaction Entry Found"
                : "No Matching Demo Entry";

            const severity = document.createElement("p");
            severity.textContent = "Severity: " + result.severity;

            const message = document.createElement("p");
            message.textContent = result.message;

            const note = document.createElement("p");
            note.textContent = result.note;

            resultBox.append(heading, severity, message, note);

        } catch (error) {

            console.error(error);
            resultBox.textContent = "Could not connect to server.";

        }
    });
}
// MEDICINE REMINDER FORM

const reminderForm = document.getElementById("reminderForm");

if (reminderForm) {

    reminderForm.addEventListener("submit", async function(event) {

        event.preventDefault();

        const medicine = document.getElementById("reminderMedicine").value.trim();
        const dosage = document.getElementById("reminderDosage").value.trim();
        const date = document.getElementById("reminderDate").value;
        const time = document.getElementById("reminderTime").value;

        if (!medicine || !dosage || !date || !time) {
            alert("Please fill all fields.");
            return;
        }

        try {

            const response = await fetch("/api/reminders/save", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    medicine: medicine,
                    dosage: dosage,
                    date: date,
                    time: time
                })
            });

            const result = await response.json();

            if (response.ok) {

                alert(result.message);

                reminderForm.reset();

                await loadReminders();

            } else {

                alert(result.message);
            }

        } catch (error) {

            console.error(error);
            alert("Could not connect to server.");

        }
    });
}


// LOAD SAVED REMINDERS

async function loadReminders() {

    const table = document.getElementById("reminderTable");

    if (!table) return;

    try {

        const response = await fetch("/api/reminders/list");
        const reminders = await response.json();

        if (!response.ok) {
            throw new Error(reminders.message);
        }

        table.replaceChildren();

        if (reminders.length === 0) {

            const row = document.createElement("tr");
            const cell = document.createElement("td");

            cell.colSpan = 5;
            cell.textContent = "No reminders scheduled yet.";

            row.appendChild(cell);
            table.appendChild(row);

            return;
        }

        reminders.forEach(reminder => {

            const row = document.createElement("tr");

            const values = [
                reminder.medicine,
                reminder.dosage,
                reminder.date,
                reminder.time,
                reminder.status
            ];

            values.forEach(value => {

                const cell = document.createElement("td");

                cell.textContent = value || "—";

                row.appendChild(cell);
            });

            table.appendChild(row);
        });

    } catch (error) {

        console.error(error);

        table.replaceChildren();

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 5;
        cell.textContent = "Unable to load reminders.";

        row.appendChild(cell);
        table.appendChild(row);
    }
}


// LOAD REMINDERS WHEN PAGE OPENS

loadReminders();
// MEDICINE REMINDER POPUP NOTIFICATION

const notifiedReminders = new Set();

async function checkDueReminders() {

    try {

        const response = await fetch("/api/reminders/list");

        if (!response.ok) return;

        const reminders = await response.json();

        reminders.forEach(reminder => {

            if (reminder.status === "Due"
                && !notifiedReminders.has(reminder.id)) {

                notifiedReminders.add(reminder.id);

                alert(
                    "⏰ MEDSHIELD MEDICINE REMINDER!\n\n" +
                    "Medicine: " + reminder.medicine + "\n" +
                    "Dosage: " + reminder.dosage + "\n" +
                    "Scheduled Time: " + reminder.time + "\n\n" +
                    "Please check your prescribed schedule."
                );
            }

        });

    } catch (error) {

        console.error("Reminder notification error:", error);

    }
}


// Check every 30 seconds on reminders page

if (document.getElementById("reminderTable")) {

    checkDueReminders();

    setInterval(checkDueReminders, 30000);

}
// DASHBOARD STATISTICS

async function loadDashboardStats() {

    const medicineCount = document.getElementById("totalMedicines");

    if (!medicineCount) return;

    try {

        const response = await fetch("/api/dashboard/stats");

        const stats = await response.json();

        if (!response.ok) {
            throw new Error(stats.message || "Could not load statistics");
        }

        document.getElementById("totalMedicines").textContent =
            stats.totalMedicines;

        document.getElementById("totalPrescriptions").textContent =
            stats.totalPrescriptions;

        document.getElementById("activeReminders").textContent =
            stats.activeReminders;

    } catch (error) {

        console.error("Dashboard Error:", error);

        medicineCount.textContent = "—";
        document.getElementById("totalPrescriptions").textContent = "—";
        document.getElementById("activeReminders").textContent = "—";
    }
}

// Load statistics when dashboard opens
loadDashboardStats();
// LOAD LOGGED-IN USER PROFILE

async function loadDashboardProfile() {

    const profile = document.getElementById("userProfile");

    if (!profile) return;

    try {

        const response = await fetch("/api/dashboard/profile");

        const result = await response.json();

        if (!response.ok) {
            throw new Error("Could not load profile");
        }

        profile.textContent = "👤 " + result.fullName;

    } catch (error) {

        console.error(error);
        profile.textContent = "👤 User";

    }
}

loadDashboardProfile();