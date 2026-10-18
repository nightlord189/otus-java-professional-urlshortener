# URL shortener

Course project for the OTUS "Java Developer Professional" course: a URL shortening service with a REST API,
click statistics and an admin area.

## Features

- Short links with a random 7-character code or a custom alias, optional expiry date
- `GET /{code}` redirect, served from an in-memory cache
- Registration and login with JWT; a user sees and manages only their own links
- Click statistics per link: total, per day, top referers. Clicks are recorded asynchronously
- Admin API: list and block links and users, global statistics
- Swagger UI, health check and metrics

## Stack

Java 21, Spring Boot 3.5 (Web MVC, Data JPA, Security, Cache), PostgreSQL 17, Flyway, Caffeine,
springdoc-openapi, Micrometer, JUnit 5, Mockito, Testcontainers, Docker Compose.

## Run

### Everything in Docker

```bash
docker compose -f docker-compose.yml up --build
```

Builds the image and starts the application with PostgreSQL. Swagger UI: http://localhost:8080/swagger-ui.html

### For development

Requires JDK 21 and Docker.

```bash
./gradlew bootRun
```

PostgreSQL from `compose.yaml` is started automatically. Stop it later with `docker compose down`.

## Quick demo

Register, log in, shorten a URL and follow it:

```bash
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email": "demo@example.com", "password": "password123"}'

TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email": "demo@example.com", "password": "password123"}' | jq -r .accessToken)

curl -s -X POST localhost:8080/api/v1/links -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"originalUrl": "https://spring.io/projects/spring-boot"}'

curl -i localhost:8080/<code>
```

The same flow, plus the admin calls, is in [http/demo.http](http/demo.http) for the IntelliJ HTTP client.

The first start creates an admin user from `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`
(defaults `admin@example.com` / `admin123`).

## API

All endpoints except the redirect and `/api/v1/auth/**` require `Authorization: Bearer <token>`.
Errors are returned as `application/problem+json`.

| Method | Path | Description |
|---|---|---|
| GET | `/{code}` | Redirect (302). 404 unknown, 410 disabled, blocked or expired |
| POST | `/api/v1/auth/register` | Register |
| POST | `/api/v1/auth/login` | Get a JWT |
| GET | `/api/v1/users/me` | Current user |
| POST | `/api/v1/links` | Create a link: `originalUrl`, optional `alias`, `expiresAt` |
| GET | `/api/v1/links?status&page&size` | Own links |
| GET | `/api/v1/links/{id}` | One link |
| PATCH | `/api/v1/links/{id}` | Change `status` (ACTIVE / DISABLED) or `expiresAt` |
| DELETE | `/api/v1/links/{id}/expiry` | Remove the expiry date |
| DELETE | `/api/v1/links/{id}` | Delete (the code is never reused) |
| GET | `/api/v1/links/{id}/stats?from&to` | Clicks: total, by day, top referers (default: last 30 days) |
| GET | `/api/v1/admin/links?ownerId&status&page&size` | All links (admin) |
| GET | `/api/v1/admin/links/{id}` | Any link (admin) |
| POST | `/api/v1/admin/links/{id}/block`, `/unblock` | Moderation (admin) |
| GET | `/api/v1/admin/users`, `/api/v1/admin/users/{id}` | Users (admin) |
| POST | `/api/v1/admin/users/{id}/block`, `/unblock` | Block login (admin) |
| GET | `/api/v1/admin/stats` | Global statistics (admin) |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/metrics/shortener.redirects` | Redirect counter by outcome (admin) |

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/urlshortener` | JDBC URL |
| `DB_USERNAME`, `DB_PASSWORD` | `urlshortener` | Database credentials |
| `APP_BASE_URL` | `http://localhost:8080` | Public URL used to build short links |
| `APP_JWT_SECRET` | dev value | HMAC key for JWT, at least 32 bytes |
| `APP_JWT_TTL` | `1h` | Access token lifetime |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | `admin@example.com` / `admin123` | First admin account |
| `APP_IP_SALT` | dev value | Salt for hashing client IPs in click records |

## Tests

```bash
./gradlew test
```

Unit tests for code generation and link validation, integration tests for every endpoint against a real
PostgreSQL started by Testcontainers (Docker required).

## How it works

- **Redirect.** `GET /{code}` resolves the code through a Caffeine cache that stores the URL together with
  the link status and expiry, so status and expiry are checked on every hit without a database query.
  Changing or deleting a link evicts its cache entry after the transaction commits.
- **302, not 301.** Browsers cache a 301 permanently and later clicks would never reach the server.
- **Codes.** 7 random Base62 characters (62^7 ≈ 3.5·10^12 values). The generator checks the database and
  retries on the rare collision. Custom aliases are validated against a reserved list (`api`, `admin`, ...).
- **Soft delete.** A deleted link keeps its row with status `DELETED`; the code is never handed out again,
  and the destination URL of a link cannot be changed. A short link that is already shared must not start
  pointing somewhere else.
- **Clicks.** The redirect publishes a Spring application event; an `@Async` listener writes the click
  (time, referer, user agent, salted hash of the IP) so the redirect does not wait for the insert.
- **Security.** Stateless JWT (HS256) issued and verified with Spring Security's own `JwtEncoder` /
  `JwtDecoder`; the `role` claim maps to `ROLE_USER` / `ROLE_ADMIN`.
- **Validation.** Request bodies are plain records; rules live in the services and produce
  RFC 9457 problem details with a 400 status.
