# RegLog Backend — Registration & Authentication Service

A robust, pure Java authentication backend service built with native Java `HttpServer`, MySQL JDBC, BCrypt password hashing, and RFC 7519 JWT session security. No frameworks — zero Spring, zero external HTTP libraries.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17+ |
| HTTP Server | `com.sun.net.httpserver.HttpServer` (built-in Java) |
| Database | MySQL 8.0+ via JDBC |
| Password Hashing | jBCrypt 0.4 |
| JWT | Custom HMAC-SHA256 (RFC 7519) |
| JSON | Gson 2.10.1 |

---

## Project Structure

```
reglog_backend/
├── src/
│   └── com/reglog/
│       ├── ReglogServer.java          # Entry point — starts HttpServer on port 8080
│       ├── config/
│       │   └── DatabaseConfig.java    # Reads application.properties, manages DB connection
│       ├── controller/
│       │   ├── AuthController.java    # Routes all /api/auth/* requests
│       │   └── StaticFileHandler.java # Serves reglog_frontend static files at /
│       ├── model/
│       │   ├── User.java              # User entity
│       │   └── UserSession.java       # Session/token entity
│       ├── security/
│       │   ├── JwtUtil.java           # JWT generation & validation (HMAC-SHA256)
│       │   └── PasswordUtil.java      # BCrypt hash & verify
│       └── service/
│           └── AuthService.java       # Business logic: register, login, OTP, etc.
├── lib/
│   ├── gson-2.10.1.jar
│   ├── jbcrypt-0.4.jar
│   └── mysql-connector-j-8.3.0.jar
├── bin/                               # Compiled .class files (auto-generated)
├── application.properties             # DB credentials, port, JWT secret
├── build.bat                          # Windows compile script
└── run.bat                            # Windows run script
```

---

## Prerequisites

- **Java JDK** 17 or higher (tested with Java 17, 21, 26)
- **MySQL Server** 8.0+ running on `localhost:3306`
- No Maven or Gradle required — all JARs are bundled in `lib/`

---

## Database Setup

1. Make sure MySQL is running on port `3306`.
2. Create the database:
   ```sql
   CREATE DATABASE IF NOT EXISTS reglog_db;
   ```
3. **Auto-Migration**: On every startup, the backend automatically creates and verifies the following tables:
   - `users` — stores user accounts
   - `user_sessions` — stores active JWT sessions
   - `otp_verifications` — stores OTP codes for password reset

---

## Configuration

Edit `application.properties` to change defaults:

```properties
# Database
db.url=jdbc:mysql://localhost:3306/reglog_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=root

# Server
server.port=8080

# JWT
jwt.secret=RegLogSuperSecretKeyForJWTAuthentication2026
jwt.expiration=86400000
```

Supports environment variable overrides:

| Property | Environment Variable |
|----------|---------------------|
| `db.url` | `DB_URL` |
| `db.username` | `DB_USERNAME` |
| `db.password` | `DB_PASSWORD` |
| `server.port` | `PORT` |
| `jwt.secret` | `JWT_SECRET` |
| `jwt.expiration` | `JWT_EXPIRATION` |

---

## How to Build & Run

### Method 1 — Windows Scripts (Easiest)

```cmd
# Compile
build.bat

# Run
run.bat
```

### Method 2 — PowerShell

```powershell
# Compile
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" }
javac -cp "lib/*" -d bin (Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })

# Run
java -cp "bin;lib/*" com.reglog.ReglogServer
```

### Method 3 — Linux / macOS / Git Bash

```bash
mkdir -p bin
find src -name "*.java" > sources.txt
javac -cp "lib/*" -d bin @sources.txt
java -cp "bin:lib/*" com.reglog.ReglogServer
```

On successful startup you will see:

```
============================================================
  RegLog Authentication & Registration Service (Java Backend)
============================================================
  Server is listening on port: 8080
  Database connected: MySQL (localhost:3306 / reglog_db)
  Base API Endpoint: http://localhost:8080/api/auth
============================================================
```

---

## REST API Reference

**Base URL:** `http://localhost:8080/api/auth`
**Content-Type:** `application/json`
**Authentication:** `Authorization: Bearer <token>` (where required)

---

### POST `/api/auth/register`

Register a new user account.

**Request Body:**
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "password": "Password@123",
  "confirmPassword": "Password@123"
}
```

**Validation Rules:**
- All fields are required
- Valid email format
- Mobile number must be exactly 10 digits
- Passwords must match
- Password: min 8 characters, must include uppercase, lowercase, digit, and special character
- Email and mobile number must be unique

**Success `201`:**
```json
{
  "message": "User registered successfully! Please log in.",
  "userId": 1,
  "status": 201
}
```

---

### POST `/api/auth/login`

Login with email or mobile number.

**Request Body:**
```json
{
  "identifier": "john.doe@example.com",
  "password": "Password@123"
}
```

> `identifier` can be an email address **or** a 10-digit mobile number.

**Success `200`:**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210"
  },
  "status": 200
}
```

**Invalid Credentials `401`:**
```json
{
  "error": "Invalid credentials.",
  "status": 401
}
```

> Generic error — does not reveal whether email/mobile or password was wrong (prevents user enumeration).

---

### POST `/api/auth/logout`

Revokes the JWT session on the server side.

**Headers:** `Authorization: Bearer <token>`

**Success `200`:**
```json
{
  "message": "Logged out successfully",
  "status": 200
}
```

---

### GET `/api/auth/me`

Returns the authenticated user's profile.

**Headers:** `Authorization: Bearer <token>`

**Success `200`:**
```json
{
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "createdDate": "2026-10-15 18:46:58.0"
  },
  "status": 200
}
```

---

### POST `/api/auth/forgot-password`

Generates a 6-digit OTP for password recovery.

**Request Body:**
```json
{
  "identifier": "john.doe@example.com"
}
```

**Success `200`:**
```json
{
  "message": "OTP generated successfully. (In local development, check terminal console)",
  "otp": "988613",
  "status": 200
}
```

---

### POST `/api/auth/verify-otp`

Verifies the 6-digit OTP.

**Request Body:**
```json
{
  "identifier": "john.doe@example.com",
  "otp": "988613"
}
```

**Success `200`:**
```json
{
  "verified": true,
  "message": "OTP verified successfully.",
  "status": 200
}
```

---

### POST `/api/auth/reset-password`

Resets password after OTP verification.

**Request Body:**
```json
{
  "identifier": "john.doe@example.com",
  "otp": "988613",
  "newPassword": "NewPassword@2026",
  "confirmPassword": "NewPassword@2026"
}
```

**Success `200`:**
```json
{
  "message": "Password reset successfully. You can now login with your new password.",
  "status": 200
}
```

---

### PUT `/api/auth/change-password`

Changes password for an authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "currentPassword": "Password@123",
  "newPassword": "NewPassword@2026",
  "confirmPassword": "NewPassword@2026"
}
```

**Success `200`:**
```json
{
  "message": "Password changed successfully.",
  "status": 200
}
```

---

## CORS

All endpoints include CORS headers:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

---

## Frontend Integration

The backend also serves the React frontend static files via `StaticFileHandler`:

| URL | Description |
|-----|-------------|
| `http://localhost:8080/` | Serves `reglog_frontend/index.html` |
| `http://localhost:8080/login` | Serves `reglog_frontend/index.html` |
| `http://localhost:8080/register` | Serves `reglog_frontend/index.html` |
| `http://localhost:8080/auth` | Serves `reglog_frontend/index.html` |

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `Access denied for user 'root'@'localhost'` | Check `db.password` in `application.properties` |
| `Port 8080 already in use` | Change `server.port` to `8081` or kill the process using port 8080 |
| `MySQL Driver Not Found` | Ensure `lib/mysql-connector-j-8.3.0.jar` is present |
| `reglog_db` does not exist | Run `CREATE DATABASE IF NOT EXISTS reglog_db;` in MySQL |
| `bin` folder missing | Run `build.bat` or create the `bin/` folder manually before compiling |
