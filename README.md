# 🚗 Auto Consultancy Management System

A full-stack, production-ready web application for managing bike-related services and finance consultancy. Built with **React.js** + **Spring Boot** + **MySQL**.

---

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Docker Setup](#docker-setup)
- [Default Login Credentials](#default-login-credentials)
- [API Documentation](#api-documentation)
- [Features](#features)
- [Database Schema](#database-schema)
- [Environment Variables](#environment-variables)
- [Production Deployment](#production-deployment)

---

## Project Overview

The Auto Consultancy Management System enables:
- **Customers** to submit bike details and finance information online via a guided 5-step form
- **Workers** to review, verify, and process applications
- **Admins** to manage the entire system — users, brands, bike models, and applications

> **Data:** The system ships with **62 pre-seeded bike brands** and **555 bike models** covering all major Indian and international manufacturers.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite 5 + Tailwind CSS |
| Backend | Spring Boot 3.2 (Java 17) |
| Database | MySQL 8.x |
| Authentication | JWT (JSON Web Tokens) |
| Charts | Recharts |
| File Storage | Local Filesystem |
| Containerization | Docker + Docker Compose |

---

## Project Structure

```
Auto_Consultancy/
├── backend/                    # Spring Boot REST API
│   ├── src/main/java/com/autoconsultancy/
│   │   ├── controller/         # REST controllers
│   │   ├── service/            # Business logic
│   │   ├── entity/             # JPA entities
│   │   ├── repository/         # Spring Data repositories
│   │   ├── dto/                # Request/Response DTOs
│   │   ├── security/           # JWT auth
│   │   ├── config/             # CORS, security config
│   │   └── exception/          # Global exception handling
│   └── src/main/resources/
│       └── application.properties
├── frontend/                   # React.js UI
│   └── src/
│       ├── api/                # Axios API clients
│       ├── components/         # Reusable UI components
│       ├── pages/              # Admin, Worker, Customer, Auth, Public pages
│       ├── hooks/              # Custom React hooks
│       ├── context/            # Auth context
│       └── utils/              # Constants, helpers
├── uploads/                    # Uploaded documents (local storage)
├── docker-compose.yml          # Docker orchestration
└── README.md
```

---

## Prerequisites

### For Local Development (without Docker)

| Tool | Version | Download |
|---|---|---|
| Java JDK | 17+ | https://adoptium.net/ |
| Apache Maven | 3.8+ | https://maven.apache.org/ |
| MySQL Server | 8.0+ | https://dev.mysql.com/downloads/ |
| Node.js | 18+ | https://nodejs.org/ |
| npm | 9+ | Included with Node.js |

### For Docker Setup
| Tool | Version |
|---|---|
| Docker | 24+ |
| Docker Compose | 2.x |

---

## Local Development Setup

### Step 1: Clone/Download the Project

```bash
cd Auto_Consultancy
```

### Step 2: Set Up MySQL Database

1. Open MySQL client (MySQL Workbench or terminal):
```sql
CREATE DATABASE auto_consultancy;
```

> **Note:** The backend connects with `root/root` by default (configurable in `application.properties`). You can create a dedicated user:
```sql
CREATE USER 'acuser'@'localhost' IDENTIFIED BY 'acpassword';
GRANT ALL PRIVILEGES ON auto_consultancy.* TO 'acuser'@'localhost';
FLUSH PRIVILEGES;
```

### Step 3: Configure Backend

Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auto_consultancy?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

### Step 4: Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start on **http://localhost:8080**

> **Note:** On first startup, the application automatically:
> - Creates all database tables (JPA DDL auto)
> - Seeds default admin and worker accounts
> - Seeds **62 bike brands** with models and manufacturing years

### Step 5: Set Up the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on **http://localhost:5173**

### Step 6: Open the Application

Open your browser and go to: **http://localhost:5173**

---

## Docker Setup

### Option 1: Run Everything with Docker Compose

```bash
# From the project root directory
docker-compose up --build
```

This starts:
- MySQL on port `3306`
- Spring Boot backend on port `8080`
- React frontend on port `3000`

Open: **http://localhost:3000**

### Option 2: Stop All Services

```bash
docker-compose down
```

### Option 3: Stop and Remove Data

```bash
docker-compose down -v
```

---

## Default Login Credentials

> ⚠️ **Credentials are set via environment variables. See `.env.example` for the full list.**
>
> **Do NOT hardcode real passwords anywhere** — not in source code, README, or commit messages.

Set these environment variables before starting the backend for the first time:

```bash
SEED_ADMIN_EMAIL=admin@yourconsultancy.com
SEED_ADMIN_PASSWORD=<strong-password>      # Min 8 chars, upper+lower+digit+special
SEED_WORKER_PASSWORD=<strong-password>
```

The DataSeeder creates these accounts on first startup **only if the admin email doesn't already exist**.

| Role | Email (default) | Password |
|---|---|---|
| **Admin** | `SEED_ADMIN_EMAIL` env var | `SEED_ADMIN_PASSWORD` env var |
| **Workers** | worker1/2/3@autoconsultancy.com | `SEED_WORKER_PASSWORD` env var |
| **Customer** | Register via `/register` | — |

**Customer:** Register a new account via the public registration page at `/register`.

---

## API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication

**Login:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@autoconsultancy.com",
  "password": "Admin@123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGci...",
    "role": "ADMIN",
    "firstName": "System",
    "lastName": "Admin",
    "email": "admin@autoconsultancy.com"
  }
}
```

**Register (Customer):**
```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "password": "Password@123"
}
```

### Protected Routes

All protected endpoints require:
```http
Authorization: Bearer <your-jwt-token>
```

### Key API Endpoints

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | /api/brands/public/all | Public | List all active brands |
| GET | /api/brands | Admin | All brands (paginated) |
| POST | /api/brands | Admin | Create brand |
| PUT | /api/brands/{id} | Admin | Update brand |
| DELETE | /api/brands/{id} | Admin | Deactivate brand |
| GET | /api/bike-models/public/by-manufacturer/{id} | Public | Models by brand |
| GET | /api/bike-models/public/{id}/variants | Public | Variants by model |
| GET | /api/bike-models/public/{id}/years | Public | Manufacturing years |
| POST | /api/applications | Customer | Create draft application |
| PUT | /api/applications/{id}/bike-details | Customer/Worker | Save bike details |
| PUT | /api/applications/{id}/finance-details | Customer/Worker | Save finance details |
| PUT | /api/applications/{id}/submit | Customer | Submit application |
| GET | /api/applications/my | Customer | My applications |
| GET | /api/applications | Admin | All applications (paginated) |
| PUT | /api/applications/{id}/status | Admin/Worker | Update status |
| PUT | /api/applications/{id}/assign-worker | Admin | Assign worker |
| POST | /api/documents/upload/{appId} | Customer/Worker | Upload document |
| GET | /api/admin/dashboard | Admin | Dashboard stats |
| GET | /api/reports/applications-by-month | Admin | Monthly chart data |
| GET | /api/reports/applications-by-status | Admin | Status distribution |
| GET | /api/reports/manufacturer-stats | Admin | Brand application stats |
| GET | /api/reports/finance-stats | Admin | Finance breakdown |
| GET | /api/reports/applications-by-worker | Admin | Worker performance |
| GET | /api/health | Public | Health check |

> **Legacy Note:** The `/api/manufacturers/*` endpoints remain available for backward compatibility but are deprecated. Use `/api/brands/*` instead.

---

## Features

### Admin Features
- ✅ Complete dashboard with live analytics charts (area, pie, bar)
- ✅ Manage customers and workers (CRUD)
- ✅ Manage bike brands (62 pre-seeded), models (555), variants, and years
- ✅ Review and approve/reject/complete applications
- ✅ Assign applications to workers
- ✅ Document verification workflow
- ✅ Finance records management
- ✅ Audit logs
- ✅ Search and filter applications by status, customer name, app number
- ✅ Reports & analytics with CSV export
- ✅ Collapsible responsive sidebar

### Worker Features
- ✅ View assigned applications
- ✅ Update application status with remarks
- ✅ Document verification
- ✅ Finance information review

### Customer Features
- ✅ Register and login
- ✅ 5-step guided application form
- ✅ Cascading dropdowns: Brand → Model → Variant → Year
- ✅ Finance details submission (with/without loan)
- ✅ Document upload (Aadhaar, DL, RC, Insurance, PAN, Address Proof)
- ✅ Real-time application status tracking
- ✅ Application history

---

## Database Schema

### Main Tables

| Table | Description |
|---|---|
| users | User accounts (all roles) |
| customers | Customer profiles |
| workers | Worker profiles |
| manufacturers | Bike brands (62 seeded) |
| bike_models | Bike models per brand (555 seeded) |
| bike_variants | Model variants (Standard variant per model) |
| manufacturing_years | Valid years per model |
| applications | Application records |
| bike_details | Bike info per application |
| finance_details | Finance/loan info |
| documents | Uploaded documents |
| application_status_history | Status change timeline |
| worker_assignments | Worker-application assignments |
| notifications | In-app notifications |
| audit_logs | Admin/worker action logs |

> **Note:** The `manufacturers` table name is kept internally for database compatibility. The user-facing term throughout the UI and API is **"Brands"**.

---

## Environment Variables

### Backend (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | localhost:3306/auto_consultancy | MySQL connection URL |
| `spring.datasource.username` | root | DB username |
| `spring.datasource.password` | root | DB password |
| `jwt.secret` | (long string) | JWT signing secret — **change in production** |
| `jwt.expiration` | 86400000 | Token expiry in ms (24h) |
| `file.upload-dir` | ./uploads | File upload directory |
| `server.port` | 8080 | Backend server port |
| `app.cors.allowed-origins` | http://localhost:5173,http://localhost:3000 | CORS allowed origins |

---

## Production Deployment

### Required Changes for Production:
1. **JWT Secret**: Change `jwt.secret` to a strong random string (64+ chars)
2. **DB Credentials**: Use environment variables — never hardcode credentials
3. **DDL Mode**: Set `spring.jpa.hibernate.ddl-auto=validate` (not `update`)
4. **HTTPS**: Enable TLS on the server
5. **CORS**: Restrict `app.cors.allowed-origins` to your actual domain
6. **File Storage**: Use cloud storage (S3/GCS) instead of local filesystem for documents
7. **Logging**: Configure structured logging and monitoring (e.g., Logback + ELK)
8. **Secrets Management**: Use environment variables or a vault (AWS Secrets Manager, etc.)

---

## Testing

### Backend Health Check
```
GET http://localhost:8080/api/health
```

### Quick API Smoke Test (PowerShell)
```powershell
# Set your credentials (do NOT hardcode passwords in scripts)
$email    = $env:SEED_ADMIN_EMAIL     # or type: 'admin@autoconsultancy.com'
$password = $env:SEED_ADMIN_PASSWORD  # or type your admin password

# Login
$r = Invoke-RestMethod "http://localhost:8080/api/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body "{`"email`":`"$email`",`"password`":`"$password`"}"
$token = $r.data.token

# Test brands
Invoke-RestMethod "http://localhost:8080/api/brands/public/all" | Select-Object -ExpandProperty data | Measure-Object | Select Count

# Test dashboard
Invoke-RestMethod "http://localhost:8080/api/admin/dashboard" `
  -Headers @{Authorization="Bearer $token"} | Select-Object -ExpandProperty data
```

---

## Support

For issues or questions, contact the development team.

---

*Auto Consultancy Management System — Professional Bike Finance Consultancy Platform*
#   A u t o - C o n s u l t a n c y - M a n a g e m e n t - S y s t e m  
 