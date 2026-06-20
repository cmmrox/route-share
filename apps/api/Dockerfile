# syntax=docker/dockerfile:1
# Multi-stage build for the RouteShare API (Spring Boot 3 / Java 21).

# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Cache dependencies first.
COPY pom.xml .
RUN mvn -q -B -e -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# --- runtime stage ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
# curl is used by the container HEALTHCHECK; run as a non-root user.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system routeshare \
    && useradd --system --gid routeshare --home /app routeshare
COPY --from=build /workspace/target/api-*.jar /app/app.jar
RUN chown -R routeshare:routeshare /app
USER routeshare

ENV SPRING_PROFILES_ACTIVE=json \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

# Liveness probe hits the Spring Boot Actuator liveness group (no DB dependency).
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD ["sh", "-c", "curl -fsS http://localhost:8080/actuator/health/liveness || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
