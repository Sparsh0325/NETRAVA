# IntelliShare

IntelliShare is a React interface backed by a Java Spring Boot API and PostgreSQL. This repository is the migration project; the separate original reference project is not modified.

## Current status

Implemented: JWT registration/login, persistent case APIs, case evidence and timelines, criminal registry APIs, agency-scoped case counts and analytics, global search across saved cases and registry records, persistent audit history with CSV export, manual case entry, validated CSV case import, agency-scoped case sharing with read/edit permissions and revocation, and optional server-side Gemini intelligence analysis with Google Search grounding.

The audit API is restricted to Administrator and Analyst roles. Case and registry writes, authentication success, imports, sharing actions, and AI requests are auditable. Migration V7 rehashes existing records in chain order using an unambiguous encoding. The SHA-256 chain helps detect changes to records; a database administrator can still alter both records and hashes, so the chain is not tamper-proof against privileged database access.

Known limitations:

- Gemini analysis is available when `INTELLISHARE_AI_GEMINI_API_KEY` is configured on the backend. Only the prompt text typed into the scanner is sent to Google Gemini; case data, uploaded files, and photos are not included. Responses are AI-generated and require independent verification. Search grounding may incur provider usage charges.
- Case sharing currently covers records and their attached evidence/timeline; it does not transfer files or provide messaging. Access is not time-limited.
- Some older analytics drill-down panels and dashboard visualizations remain demonstrations. The global case analytics and dashboard totals use saved cases.
- CSV import supports case records only (UTF-8, up to 2 MB and 500 rows). It stores job metadata, validates the complete file before importing, and does not retain the uploaded file.
- No public GitHub repository or deployment URL has been created or verified.
- Face identification and suspect matching are not implemented.
- External government and agency data systems are not connected; the architecture illustration is not a live integration.
- A real Gemini provider request remains unverified until a backend API key is configured through the hosting provider.

## Technology

- Frontend: React 19, TypeScript, Vite, React Router, Tailwind CSS, Lucide, Leaflet, and Recharts
- Backend: Java 25, Spring Boot 3.5, Spring Security, Spring Data JPA, Hibernate, Maven Wrapper
- Data: PostgreSQL 17 and Flyway migrations
- Authentication: BCrypt password hashing and JWT bearer tokens
- Optional AI: server-side Google Gemini GenerateContent with Google Search grounding

## Run locally on Windows

### 1. Start PostgreSQL

The local setup uses a dedicated PostgreSQL 17 container named `intellishare-db`, bound to `127.0.0.1:5436`. Do not expose this port publicly. If the container is not running, start the existing IntelliShare container with Docker Desktop; do not stop unrelated containers.

### 2. Configure backend environment

Copy `.env.example` to `.env` and set a strong database password and a random Base64 JWT signing key (at least 32 random bytes before Base64 encoding). To use the intelligence scanner, also set `INTELLISHARE_AI_GEMINI_API_KEY` on the backend. This key is optional and must never be put in `frontend/.env`. `.env` is ignored by Git. Do not commit secrets.

### 3. Start the Spring API

From the project root in PowerShell:

```powershell
.\run-backend.ps1
```

The API listens on `http://localhost:8085`. Health check: `http://localhost:8085/api/health`. Set `FLYWAY_ENABLED=true` in `.env` to apply pending migrations at startup.

### 4. Start the React app

In another PowerShell window:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. Set `VITE_API_BASE_URL` only if the API is hosted somewhere other than `http://localhost:8085/api`.

## CSV import format

The import accepts one `.csv` file with this exact header order:

```csv
caseNumber,title,type,status,priority,incidentDate,city,address,description
DEMO-2026-001,Example case,Theft,Registered,Medium,2026-01-02,Delhi,Example Road,Synthetic demonstration record
```

Allowed statuses are `Registered`, `Under Investigation`, `Closed`, and `Cold Case`. Allowed priorities are `High`, `Medium`, and `Low`. Each imported case is assigned to the signed-in user's agency. Invalid rows reject the file without creating cases; the response lists the row errors.

## Verification

Run backend tests from the project root:

```powershell
.\mvnw.cmd clean test
```

Verified on 2026-10-21: 24 Java tests passed, including the Flyway migration test against the dedicated PostgreSQL 17 container and an audit rehash migration test. The Google provider call is exercised with a local mock HTTP server; a real Gemini key/provider request has not been verified. The frontend TypeScript check and Vite production build passed. The Vite build reports a large-bundle advisory. Build the frontend with:

```powershell
cd frontend
npm run build
```

## Main API routes

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET|POST /api/cases`, `GET|PUT|DELETE /api/cases/{id}`
- `POST /api/cases/{id}/evidence`, `POST /api/cases/{id}/timeline`
- `GET|POST /api/criminal-records`, `GET|PUT|DELETE /api/criminal-records/{recordNumber}`
- `GET /api/dashboard/stats`, `GET /api/dashboard/analytics`
- `GET /api/audit-logs`, `GET /api/audit-logs/export.csv`, `GET /api/audit-logs/verify`
- `POST /api/ingestion/cases.csv` (multipart field `file`)
- `POST /api/intelligence/analyze` (Administrator, Officer, or Analyst; prompt text only)
- `GET /api/shares`, `POST /api/shares/cases/{caseKey}`, `DELETE /api/shares/{shareId}`

Protected routes require `Authorization: Bearer <token>`. Public registration creates a Read-Only user; an administrator must grant any elevated role through a controlled administrative process.

## Migrations

SQL Flyway migrations are versioned under `src/main/resources/db/migration`; the order-preserving audit rehash is Java migration `src/main/java/db/migration/V7__rehash_audit_chain.java`. Never edit an applied migration; add a new version for schema changes.

## Deployment and resume use

Deployment remains pending. First configure a user-owned GitHub repository, production PostgreSQL, backend and frontend hosting, and their environment variables. Keep database/JWT/AI secrets on the backend. Use synthetic demo records only; do not publish real case or personal data.

Resume description (limited to verified functionality):

> Migrated IntelliShare’s React/TypeScript interface to a Java 25 and Spring Boot backend with PostgreSQL persistence, Flyway migrations, JWT/BCrypt authentication, case and criminal-record APIs, agency-scoped analytics and search, permissioned inter-agency sharing, hash-chained audit history, validated CSV ingestion, and an optional server-side Gemini analysis endpoint.
