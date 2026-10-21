# ═══════════════════════════════════════════════════════════════════════════
# AniReview — imagem Docker para Spring Boot (Java 17)
# Build multi-etapa: compila com Maven/JDK 17 e gera uma imagem final leve
# apenas com o JRE e o JAR executavel, pronta a correr no Render.
# ═══════════════════════════════════════════════════════════════════════════

# ── Etapa 1: build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar primeiro apenas o pom.xml para aproveitar a cache de camadas do
# Docker: as dependencias so sao descarregadas de novo se o pom.xml mudar.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiar o codigo fonte e empacotar o JAR executavel do Spring Boot.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ── Etapa 2: runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Correr como utilizador nao-root (boa pratica de seguranca em contentores).
RUN addgroup -S app && adduser -S app -G app

# Copiar apenas o JAR final da etapa de build.
COPY --from=build /app/target/anime-platform-*.jar app.jar
RUN chown -R app:app /app
USER app

# O Render injeta a porta a usar na variavel PORT; localmente usa-se 8080.
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
