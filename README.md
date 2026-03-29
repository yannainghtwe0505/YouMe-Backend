# Dating App MVP (Java 17, Spring Boot 3)

Features: JWT auth, profiles, likes → matches, messaging, Swagger UI, Flyway, Postgres, tests, Dockerfile, CI.

## Run locally
1. `docker compose up -d` (Postgres)
2. `mvn spring-boot:run`
3. Swagger: http://localhost:8080/swagger-ui/index.html

### Auth
- POST /auth/register {email,password}
- POST /auth/login {email,password} -> {token}
Send `Authorization: Bearer <token>` for protected routes.
