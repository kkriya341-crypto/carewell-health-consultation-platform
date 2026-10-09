# Carewell Health

A role-based online health consultation MVP with a React interface, Java Spring Boot API, and persistent file-based H2 database. It supports patient signup, appointment booking, professional availability, consultation advice and summaries, health records, admin user and appointment management, and clinic settings.

The backend uses Spring Data JPA for domain CRUD and `JdbcTemplate` with parameterized SQL for admin analytics. Spring MVC runs on embedded Tomcat; a small `PlatformStatusServlet` demonstrates the Servlet API directly at `/servlet/status`.

## Requirements

- Java 21 or newer
- Maven 3.9 or newer
- Node.js 20 or newer and npm

The API listens on `http://localhost:8080` and the web app runs on `http://localhost:5173`.

## Start the backend

In a terminal, open the `backend` directory and run:

```powershell
mvn spring-boot:run
```

The database file is created under `backend/data/` the first time the API starts. H2 Console (development only) is available at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:file:./data/health-consultation;DB_CLOSE_ON_EXIT=FALSE`, user `sa`, and a blank password. The lightweight servlet status endpoint is available at `http://localhost:8080/servlet/status`.

## Start the frontend

In another terminal, open the `frontend` directory and run:

```powershell
npm install
npm run dev
```

Open `http://localhost:5173`. The frontend sends session cookies to the API. The API is configured for this local development origin.

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Patient | `patient@carewell.test` | `Patient123!` |
| Healthcare professional | `doctor@carewell.test` | `Doctor123!` |
| Admin | `admin@carewell.test` | `Admin123!` |

These accounts and sample professional availability are inserted automatically into an empty database. Patients can also create accounts from the sign-in screen. Admins can create patient, professional, and admin accounts; if the password field is left blank for a newly created user, the temporary password is `Welcome123!`.

## MVP behavior

- Patients can book an open future time slot, see appointment status, add personal health records, and read professional advice and summaries.
- Professionals can add or remove open availability, review their appointments, complete a consultation with written advice and a summary, and add care notes to records of patients who have an appointment in their care.
- Admins can review appointments, create and edit accounts, manage appointment statuses, adjust clinic settings, and view basic platform totals.
- All API access except professional browsing and open-slot browsing requires a signed-in session. Passwords are BCrypt encoded. Patient records are restricted to the patient and professionals with an appointment in their care.

This is a local MVP starter, not a production medical-record system. Before real patient use, add a deployment security review, HTTPS, audit logging, backup and retention policy, and the applicable healthcare privacy controls.
