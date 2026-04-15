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

**Recommended for local work** (runs Flyway migrations; avoids broken schema after login):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- **API base URL:** set via `app.urls.api-public-base-url` / `APP_API_PUBLIC_BASE_URL` (default in `application.yml` is LAN-friendly). See [../docs/ENVIRONMENT_URLS.md](../docs/ENVIRONMENT_URLS.md).
- **OpenAPI / Swagger UI:** `{apiRoot}/swagger-ui.html` (exact path may vary by SpringDoc version)

Default config uses `spring.jpa.hibernate.ddl-auto: update` with Flyway **off** so a local DB picks up new entity columns (e.g. `messages.message_kind`). For production, set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` and run Flyway. See `src/main/resources/db/migration/`.

The **`dev`** profile uses `ddl-auto: update` and keeps Flyway **off**, so local databases can catch up on missing tables/columns without table-ownership errors. If something still returns **500** after login, run SQL migrations as a superuser or enable Flyway when your DB user is allowed to run DDL.

If the DB user cannot access tables, run `scripts/grant-dating-app-privileges.sql` as PostgreSQL superuser (see script header). That script includes **`CREATE` on schema `public`** so the app role can run Flyway DDL when needed.

---

## Configuration (important)

| Item | Location | Notes |
|------|-----------|--------|
| Database | `application.yml` → `spring.datasource.*` | Use env-specific files or env vars in production. |
| JWT | `application.yml` → `app.jwt.*` | **Change `secret` before production.** |
| S3 / media | `application.yml` → `app.s3.*`, `app.media.*` | Presign is placeholder until AWS SDK is wired (see spec). |
| Flyway | `application.yml` → `spring.flyway.enabled` | Off by default; enable to run migrations automatically. |
| URLs + CORS | `application.yml` → `app.urls.*`, `WebConfig.java` | Set `APP_CORS_ALLOWED_ORIGIN_PATTERNS` (comma-separated) and `SPRING_PROFILES_ACTIVE=prod` for production. |

---

## Docker

- **API image:** `Dockerfile` in this folder (multi-stage Maven build, JRE 17, listens on **8090**).
- **Full stack (Postgres + API + SPA):** from the **repo root**, run `docker compose up --build` — see [../docs/ENVIRONMENT_URLS.md](../docs/ENVIRONMENT_URLS.md#docker-full-stack).
- **Postgres only** (API on host): `docker compose -f docker-compose.yml up` in this folder.

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

The browser app lives in the sibling folder **`../frontend`**. API URL is configured with **`VITE_API_URL`** (see `../docs/ENVIRONMENT_URLS.md`).
