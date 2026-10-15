# RegLog Backend — Registration & Authentication Service

A robust, pure Java authentication backend service built with native Java HTTP server, MySQL JDBC, BCrypt password hashing, and RFC 7519 JWT session security.

---

## 1. Prerequisites

- **Java JDK**: Version 17 or higher (tested with Java 17, 21, and 26).
- **MySQL Server**: Version 8.0+ running on `localhost:3306`.
- **Database**: `reglog_db`.

---

## 2. Database Setup

1. Make sure MySQL is running on port 3306.
2. The default credentials configured are:
   - **Host**: `localhost`
   - **Port**: `3306`
   - **Database**: `reglog_db`
   - **Username**: `root`
   - **Password**: `root`
3. If not already created, run the following SQL command:
   ```sql
   CREATE DATABASE IF NOT EXISTS reglog_db;
   ```
4. **Auto-Migration**: The backend automatically initializes and verifies all tables (`users`, `user_sessions`, `otp_verifications`) on startup.

---

## 3. Configuration & Environment Variables

You can customize database connections and security tokens via `application.properties` or environment variables:

| Property | Default Value | Environment Variable | Description |
| :--- | :--- | :--- | :--- |
| `db.url` | `jdbc:mysql://localhost:3306/reglog_db` | `DB_URL` | MySQL JDBC connection string |
| `db.username` | `root` | `DB_USERNAME` | MySQL user |
| `db.password` | `root` | `DB_PASSWORD` | MySQL password |
| `server.port` | `8080` | `PORT` | HTTP Server port |
| `jwt.secret` | `RegLogSuperSecretKey...` | `JWT_SECRET` | Secret key for HMAC-SHA256 JWTs |
| `jwt.expiration` | `86400000` (24h in ms) | `JWT_EXPIRATION` | JWT token lifespan |

---

## 4. How to Build & Run

### Method 1: Using Provided Windows Scripts
- **To compile**:
  ```cmd
  build.bat
  ```
- **To run**:
  ```cmd
  run.bat
  ```

### Method 2: Using Command Line (PowerShell / Bash)
- **Compile**:
  ```powershell
  if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" }
  javac -cp "lib/*" -d bin (Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
  ```
- **Run**:
  ```powershell
  java -cp "bin;lib/*" com.reglog.ReglogServer
  ```

---

## 5. REST API Documentation

Base URL: `http://localhost:8080/api/auth`

### 1. Register User
- **Method**: `POST`
- **Endpoint**: `/api/auth/register`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "password": "Password@123",
    "confirmPassword": "Password@123"
  }
  ```
- **Validation**:
  - All fields are required.
  - Email format must be valid.
  - Mobile number must be exactly 10 digits.
  - Passwords must match.
  - Password must be at least 8 characters and include uppercase, lowercase, number, and special character.
  - Email and Mobile Number must each be unique.
- **Success Response (201)**:
  ```json
  {
    "message": "User registered successfully! Please log in.",
    "userId": 1,
    "status": 201
  }
  ```

### 2. Login
- **Method**: `POST`
- **Endpoint**: `/api/auth/login`
- **Request Body**:
  ```json
  {
    "identifier": "john.doe@example.com",
    "password": "Password@123"
  }
  ```
  *(Note: `identifier` can be either an Email address OR a 10-digit Mobile Number)*
- **Success Response (200)**:
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
- **Invalid Credentials (401)**:
  ```json
  {
    "error": "Invalid credentials.",
    "status": 401
  }
  ```
  *(Generic error message that does not leak whether email/mobile or password was wrong)*

### 3. Logout (Server-Side Session Revocation)
- **Method**: `POST`
- **Endpoint**: `/api/auth/logout`
- **Headers**: `Authorization: Bearer <token>`
- **Success Response (200)**:
  ```json
  {
    "message": "Logged out successfully",
    "status": 200
  }
  ```

### 4. Get Current Authenticated Profile
- **Method**: `GET`
- **Endpoint**: `/api/auth/me`
- **Headers**: `Authorization: Bearer <token>`
- **Success Response (200)**:
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

### 5. Forgot Password (Request OTP)
- **Method**: `POST`
- **Endpoint**: `/api/auth/forgot-password`
- **Request Body**:
  ```json
  {
    "identifier": "john.doe@example.com"
  }
  ```
- **Success Response (200)**:
  ```json
  {
    "message": "OTP generated successfully. (In local development, check terminal console)",
    "otp": "988613",
    "status": 200
  }
  ```

### 6. Verify OTP
- **Method**: `POST`
- **Endpoint**: `/api/auth/verify-otp`
- **Request Body**:
  ```json
  {
    "identifier": "john.doe@example.com",
    "otp": "988613"
  }
  ```
- **Success Response (200)**:
  ```json
  {
    "verified": true,
    "message": "OTP verified successfully.",
    "status": 200
  }
  ```

### 7. Reset Password
- **Method**: `POST`
- **Endpoint**: `/api/auth/reset-password`
- **Request Body**:
  ```json
  {
    "identifier": "john.doe@example.com",
    "otp": "988613",
    "newPassword": "NewPassword@2026",
    "confirmPassword": "NewPassword@2026"
  }
  ```
- **Success Response (200)**:
  ```json
  {
    "message": "Password reset successfully. You can now login with your new password.",
    "status": 200
  }
  ```

### 8. Change Password (Authenticated)
- **Method**: `PUT`
- **Endpoint**: `/api/auth/change-password`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "currentPassword": "Password@123",
    "newPassword": "NewPassword@2026",
    "confirmPassword": "NewPassword@2026"
  }
  ```
- **Success Response (200)**:
  ```json
  {
    "message": "Password changed successfully.",
    "status": 200
  }
  ```

---

## 6. Troubleshooting

- **Access denied for user 'root'@'localhost'**:
  - Verify your MySQL password in `application.properties`.
- **Port 8080 already in use**:
  - Change `server.port` in `application.properties` to `8081` or another open port.
- **MySQL Driver Not Found**:
  - Ensure `lib/mysql-connector-j-8.3.0.jar` is included in the `-cp` classpath flag.