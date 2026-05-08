# AgenciaHub API

Spring Boot REST API for **customers**, **opportunities** (sales / attendance), and **financial entries**.  
**No authentication** in this phase — add Spring Security later.

## Stack

- Java 17+
- Spring Boot 3.4, Spring Data JPA, Validation
- PostgreSQL + Flyway
- OpenAPI (Swagger UI)

## Run locally

### 1. PostgreSQL (Docker)

The compose file maps Postgres to **host port `5433`** so it does not collide with a **system PostgreSQL** often listening on **`5432`**. If the app used `5432` while Docker was on another port—or you had no `agenciahub` user on the local server—you get **`FATAL: password authentication failed for user "agenciahub"`** (SQLState `28P01`).

```bash
cd agencia-hub-api
docker compose up -d
```

### 2. API (use profile `docker` with the compose file above)

```bash
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run
```

The `docker` profile points the datasource at **`localhost:5433`** with user/password **`agenciahub`**.

- Base path: **http://localhost:8080/api/v1**
- **Swagger UI** (all REST endpoints + Try it out): **http://localhost:8080/api/v1/swagger-ui/index.html**
- Shortcut: **http://localhost:8080/api/v1/docs** → redirects to Swagger UI
- OpenAPI JSON: **http://localhost:8080/api/v1/v3/api-docs**

Set **`OPENAPI_SERVER_URL`** if the public base URL differs (e.g. behind a proxy), so “Try it out” hits the correct host.

### Using your own PostgreSQL (no Docker)

Create a role and database, then align env vars (defaults target `localhost:5432`):

```sql
CREATE USER agenciahub WITH PASSWORD 'agenciahub';
CREATE DATABASE agenciahub OWNER agenciahub;
```

Run **without** the `docker` profile, or set `DB_PORT`, `DB_USER`, and `DB_PASSWORD` to match.

### Troubleshooting `28P01` (password authentication failed)

1. **Wrong server**: App connects to whatever is on `DB_HOST`/`DB_PORT`. A local Postgres on `5432` is not the Docker instance unless you map Docker to `5432` and use matching credentials.
2. **Stale Docker volume** (user/password changed in compose): reset data and recreate:
   ```bash
   docker compose down -v
   docker compose up -d
   ```
3. **Empty password in the IDE**: Run configuration must not set `DB_PASSWORD` to blank; that overrides the default.

## Build & test

```bash
mvn -q test
mvn -q package
```

Tests use **H2** (in-memory) with the same Flyway migrations.

## API layout

| Resource            | Path prefix (after `/api/v1`) |
|---------------------|-------------------------------|
| Customers           | `/customers`                  |
| Opportunities       | `/opportunities`              |
| Quotations (quotes) | `/quotations`                 |
| Financial entries   | `/financial-entries`          |

JSON uses **camelCase**. Enums are **English** (e.g. `CustomerStatus`: `ACTIVE`, `INACTIVE`, `PROSPECT`).

## SMTP & outbound mail

Transactional e-mail uses **`spring-boot-starter-mail`** (`SmtpEmailService`) when `SMTP_PASSWORD` is set. Defaults target **Zoho** (`smtp.zoho.com:587`, STARTTLS).

| Variable | Role |
|----------|------|
| `SMTP_HOST` | SMTP host (default `smtp.zoho.com`) |
| `SMTP_PORT` | Port (default `587`) |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Mailbox credentials |
| `EMAIL_FROM` | `From` address |
| `SMTP_CONNECTION_TIMEOUT_MS` / `SMTP_READ_TIMEOUT_MS` / `SMTP_WRITE_TIMEOUT_MS` | Socket timeouts (default `15000` ms) |

**If logs show `MailConnectException` / `Connection timed out` to `smtp.zoho.com:587`:**

1. **Outgoing SMTP blocked** — Many hosts block outbound **587** (and 25/465) from containers/serverless. Check your provider’s firewall, security groups, NetworkPolicies, or “disable SMTP” policies.
2. **Sanity check from the same runtime** — `nc -zv smtp.zoho.com 587` or TLS probe; if it hangs, the problem is network, not JavaMail credentials.
3. **Auth vs network** — A hang at TCP connect is almost always **egress**, not wrong password (those usually fail later with an SMTP error code).
4. **Alternative** — Use an **HTTPS API** provider (Resend, SendGrid API, SES API) if SMTP egress is not allowed.

If `SMTP_PASSWORD` is empty, **`LoggingEmailService`** is used instead (no network).

## Configuration

Environment / `application.yml` overrides:

| Variable     | Default     |
|-------------|-------------|
| `PORT`      | `8080`      |
| `DB_HOST`   | `localhost` |
| `DB_PORT`   | `5432` (use `5433` with bundled Docker + `docker` profile) |
| `DB_NAME`   | `agenciahub` |
| `DB_USER`   | `agenciahub` |
| `DB_PASSWORD` | `agenciahub` |

## Project layout (clean / layered)

```
com.agenciahub.api
├── config          # CORS, OpenAPI
├── controller      # REST
├── domain          # Enums
├── dto             # Request/response records
├── entity          # JPA entities
├── exception       # Api errors + not found
├── repository      # Spring Data (+ specifications)
└── service         # Business logic
```

Inspired by a typical **controller → service → repository → entity** flow (similar in spirit to larger integration services, without unnecessary complexity).
