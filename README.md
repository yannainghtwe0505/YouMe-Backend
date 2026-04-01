# YouMe — Backend (REST API)

Spring Boot service that powers **YouMe**: accounts, JWT auth, profiles, discovery feed, likes/passes/superlikes, matches, chat messages, and photo metadata with presign-style URLs.

**Related docs:** [PROJECT_SPEC.md](./PROJECT_SPEC.md) (technical detail, API map, modules).

---

## Prerequisites

- **JDK 17**
- **Maven 3.8+**
- **PostgreSQL** — database and user aligned with `src/main/resources/application.yml` (or override via environment / Spring profiles)

---

## Run locally

```bash
cd backend
mvn spring-boot:run
```

- **API base URL:** `http://localhost:8090` (see `server.port` in `application.yml`)
- **OpenAPI / Swagger UI:** `http://localhost:8090/swagger-ui.html` (exact path may vary by SpringDoc version)

Apply the SQL schema before first run if `ddl-auto` is `validate` (see `src/main/resources/db/migration/`). If the DB user cannot access tables, run `scripts/grant-dating-app-privileges.sql` as a superuser (see script header).

---

## Configuration (important)

| Item | Location | Notes |
|------|-----------|--------|
| Database | `application.yml` → `spring.datasource.*` | Use env-specific files or env vars in production. |
| JWT | `application.yml` → `app.jwt.*` | **Change `secret` before production.** |
| S3 / media | `application.yml` → `app.s3.*`, `app.media.*` | Presign is placeholder until AWS SDK is wired (see spec). |
| Flyway | `application.yml` → `spring.flyway.enabled` | Off by default; enable to run migrations automatically. |
| CORS | `WebConfig.java` | Add your frontend origin(s) for production. |

---

## Project layout

```
src/main/java/com/example/dating/
  auth/           Registration & login
  controller/     REST endpoints
  service/        Business logic
  repository/     Spring Data JPA
  repositoryImpl/ UserDetailsService
  model/entity/   JPA entities
  dto/            API DTOs
  config/         Security, CORS, exception handling
  security/       JWT service & filter

src/main/resources/
  application.yml
  db/migration/   Versioned SQL (Flyway-ready)

scripts/
  grant-dating-app-privileges.sql   Optional DB grants for app role
```

---

## Build

```bash
mvn -q clean package
java -jar target/dating-app-0.0.1-SNAPSHOT.jar
```

Artifact name follows `pom.xml` `<artifactId>` and `<version>`.

---

## Frontend

The browser app lives in the sibling folder **`../frontend`**. Point the SPA’s API `baseURL` at this server (default in frontend: `http://localhost:8090`).
