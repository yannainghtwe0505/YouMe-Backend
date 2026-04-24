# Multi-stage build: Maven → JRE (Spring Boot executable JAR).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Match server.port default in application.yml (${SERVER_PORT:8090}).
ENV SERVER_PORT=8090
EXPOSE 8090

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
