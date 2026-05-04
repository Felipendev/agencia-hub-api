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
