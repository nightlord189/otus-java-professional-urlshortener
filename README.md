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

## Test

```bash
./gradlew test
```

Integration tests start PostgreSQL in Docker via Testcontainers.
