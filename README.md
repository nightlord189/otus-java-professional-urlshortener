# otus-java-professional-urlshortener

Project work for Otus Java Professional course.

URL shortener written in Java, using Spring Boot.

## Run locally

Requires Docker (for PostgreSQL) and JDK 21 or newer.

```bash
./gradlew bootRun
```

Starts PostgreSQL from `compose.yaml` and the application on http://localhost:8080.
Health check: http://localhost:8080/actuator/health

## API

Swagger UI: http://localhost:8080/swagger-ui.html

Register with `POST /api/v1/auth/register`, get a token with `POST /api/v1/auth/login`
and send it as `Authorization: Bearer <token>`.

An admin user is created on first start from `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`
(defaults: `admin@example.com` / `admin123`).

## Test

```bash
./gradlew test
```

Integration tests start PostgreSQL in Docker via Testcontainers.
