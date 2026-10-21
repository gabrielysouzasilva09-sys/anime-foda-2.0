# ═══════════════════════════════════════════════════════════════════════════
# AniReview — Docker image for a Spring Boot application (Java 17)
#
# Multi-stage build:
#   stage 1 (build)   -> compiles the project with Maven + JDK 17
#   stage 2 (runtime) -> keeps only the JRE and the executable JAR
#
# This Dockerfile lives at the repository root; the Maven project lives in
# the "anime-platform/" subfolder, so all copy paths point to that folder.
# ═══════════════════════════════════════════════════════════════════════════

# ── Stage 1: build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the POM first so Docker can cache the downloaded dependencies.
# They are downloaded again only when pom.xml changes.
COPY anime-platform/pom.xml .
RUN mvn -B dependency:go-offline

# Copy the sources and package the executable Spring Boot JAR.
COPY anime-platform/src ./src
RUN mvn -B clean package -DskipTests

# ── Stage 2: runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user (container security best practice).
RUN addgroup -S app && adduser -S app -G app

# Copy only the final JAR produced by the build stage.
COPY --from=build /app/target/anime-platform-*.jar app.jar
RUN chown -R app:app /app
USER app

# Render injects the port to use through the PORT variable; locally 8080.
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
