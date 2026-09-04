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
| Quotations (quotes) | `/quotations`                 |
| Financial entries   | `/financial-entries`          |

JSON uses **camelCase**. Enums are **English** (e.g. `CustomerStatus`: `ACTIVE`, `INACTIVE`, `PROSPECT`).

## Transactional e-mail (Resend)

The stack targets **[Resend](https://resend.com)** over HTTPS (`443`). Set **`EMAIL_RESEND_API_KEY`** and **`EMAIL_FROM`** (verified sender/domain in Resend). This matches **Railway Free/Hobby/Trial**, where **outbound SMTP is disabled**; Railway documents transactional APIs (Resend, SendGrid, etc.) as the supported path.

Routing lives in **`MailDispatchConfiguration`** (priority: Resend key → SMTP host+password+`JavaMailSender` → log). Copy for all mails is centralized in **`TransactionalMailBody`**; adding another HTTPS provider means implementing **`TransactionalMailChannel`** and one `if` branch.

| Variable | Role |
|----------|------|
| `EMAIL_RESEND_API_KEY` | Resend API key (`re_...`) — **required for real sends** in production |
| `EMAIL_FROM` | `From` header (must be allowed in Resend for your domain) |
| `EMAIL_RESEND_CONNECT_TIMEOUT_MS` / `EMAIL_RESEND_READ_TIMEOUT_MS` | HTTP timeouts (defaults `15000` / `30000` ms) |

Without `EMAIL_RESEND_API_KEY`, a **no-op channel** logs subjects and bodies (`[EMAIL-NOOP]`) instead of sending — fine for local dev.

### Optional SMTP (Railway Pro+ or self-hosted)

Only if you deliberately use JavaMail: set **`SMTP_HOST`**, **`SMTP_USERNAME`**, **`SMTP_PASSWORD`**, and **`EMAIL_FROM`**, and **leave `EMAIL_RESEND_API_KEY` unset** (Resend always wins when the key is present).

| Variable | Role |
|----------|------|
| `SMTP_HOST` | e.g. `smtp.zoho.com` |
| `SMTP_PORT` | Default `587` |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Mailbox credentials |
| `SMTP_CONNECTION_TIMEOUT_MS` / `SMTP_READ_TIMEOUT_MS` / `SMTP_WRITE_TIMEOUT_MS` | Socket timeouts (default `15000` ms) |

## Configuration

Environment / `application.yml` overrides:

| Variable     | Default     |
|-------------|-------------|
| `PORT`      | `8080`      |
| `EMAIL_FROM` | `contato@agenciashub.com.br` |
| `EMAIL_RESEND_API_KEY` | _(empty — use for real mail)_ |
| `DB_HOST`   | `localhost` |
| `DB_PORT`   | `5432` (use `5433` with bundled Docker + `docker` profile) |
| `DB_NAME`   | `agenciahub` |
| `DB_USER`   | `agenciahub` |
| `DB_PASSWORD` | `agenciahub` |

### Produção: perfil obrigatório

Qualquer deploy real (Railway ou outro host) **precisa** definir `SPRING_PROFILES_ACTIVE=production` (ou `prod`).
Com esse perfil ativo, `ProductionSecurityConfiguration` recusa subir a aplicação se `JWT_SECRET` ou
`CORS_ALLOWED_ORIGINS` não estiverem definidos com um valor seguro explícito — evita repetir em silêncio o
segredo de desenvolvimento ou liberar CORS para qualquer origem. Sem esse perfil, a validação **não roda**;
confirme a variável no painel do serviço a cada novo ambiente de produção.

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
