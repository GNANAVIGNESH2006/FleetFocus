# FleetFocus — Fleet Management System

FleetFocus is a full-stack role-based Fleet Management System supporting **Admin**, **Dispatcher**, and **Driver** workflows across Vehicles, Drivers, Trips, Maintenance Logs, and System Users.

---

## Architecture & Technology Stack

### Backend (`backend/`)
- **Java 21** & **Spring Boot 3.5.3**
- **Spring Security** with stateless JWT authentication (`jjwt 0.11.5`) & UID token verification
- **Spring Data JPA / Hibernate** with pessimistic row-level locking on concurrent state transitions
- **MySQL 8.x** (Production/Development) & **H2** (Automated Integration Tests)
- **Maven**

### Frontend (`frontend/`)
- **Static HTML5, CSS3, Vanilla JavaScript**
- **Bootstrap 5.3.3**, **Bootstrap Icons 1.11.3**, and **Axios 1.7.2** (pinned & vendored locally in `frontend/vendor/`)

---

## Required Environment Variables

Before starting the backend, configure the following environment variables (see `.env.example`):

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/fleetfocus_dbs?...` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password (**required**) | *(none)* |
| `JWT_SECRET` | Base64-encoded secret key, minimum 32 decoded bytes / 256 bits (**required**) | *(none)* |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | `86400000` (24h) |
| `CORS_ORIGINS` | Comma-separated allowed frontend origins | `http://127.0.0.1:5500,http://localhost:5500,http://localhost:63342` |
| `TRIP_OVERDUE_HOURS` | Hours after which an `ACTIVE` trip is flagged as overdue | `8` |
| `SPRING_PROFILES_ACTIVE` | Set to `dev` to enable optional development seed accounts | *(none)* |
| `SEED_ADMIN_PASSWORD` | Password for seeded `admin` user when `dev` profile is active | *(none)* |
| `SEED_DISPATCHER_PASSWORD` | Password for seeded `dispatcher` user when `dev` profile is active | *(none)* |
| `SEED_DRIVER_PASSWORD` | Password for seeded `driver` user when `dev` profile is active | *(none)* |

---

## Running the Project

### 1. Run Automated Backend Tests
```bash
cd backend
mvn clean verify
```

### 2. Start the Backend
```powershell
cd backend
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNGNDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTlBQg=="
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SEED_ADMIN_PASSWORD="adminPassword123"
$env:SEED_DISPATCHER_PASSWORD="dispatcherPassword123"
$env:SEED_DRIVER_PASSWORD="driverPassword123"
mvn spring-boot:run
```

### 3. Start the Frontend
```bash
python -m http.server 5500 --directory frontend
```
Open `http://localhost:5500/login.html`.

---

## Database Diagnostics & Maintenance Migration Scripts
- `docs/db_diagnostics.sql` — Read-only SQL diagnostic queries to inspect data consistency before applying manual fixes.
- `docs/migration_maintenance.sql` — SQL migration script for adding `status` (`IN_PROGRESS`/`COMPLETED`) and `completed_date` to existing `maintenance_logs` tables.
