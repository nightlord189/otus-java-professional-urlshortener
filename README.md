# URL shortener

Final project for the OTUS "Java Developer Professional" course.

Users register, log in and create short links (random code or own alias, optional expiry date).
`GET /{code}` redirects to the original URL and records a click. The owner can see click statistics
(total, by day, top referers), disable or delete the link. Admin can block links and users and see global stats.

Stack: Java 21, Spring Boot (Web, Data JPA, Security + JWT, Cache), PostgreSQL, Flyway, Caffeine, Testcontainers.

## How to run

Postgres in Docker, app from Gradle:

```bash
make up
make run
```

Swagger UI: http://localhost:8080/swagger-ui.html

Admin user `admin@example.com` / `admin123` is created on first start (see `application.yml`).

Everything in Docker: `docker compose -f compose.full.yaml up --build`

## Endpoints

- `POST /api/v1/auth/register`, `POST /api/v1/auth/login` - returns JWT
- `GET /api/v1/users/me`
- `POST /api/v1/links`, `GET /api/v1/links`, `GET /api/v1/links/{id}`, `PATCH /api/v1/links/{id}` (status, expiresAt),
  `DELETE /api/v1/links/{id}/expiry`, `DELETE /api/v1/links/{id}`
- `GET /api/v1/links/{id}/stats?from&to`
- `GET /{code}` - redirect
- admin: `GET /api/v1/admin/links`, `POST /api/v1/admin/links/{id}/block|unblock`,
  `GET /api/v1/admin/users`, `POST /api/v1/admin/users/{id}/block|unblock`, `GET /api/v1/admin/stats`

## Tests

`make test` - unit and integration tests (Postgres is started by Testcontainers, Docker needed).

`make api-test` - runs the main scenario with curl-like requests against a running app
(`integration-test/api-test.js`, needs Node 18+).
