# ============================================================
# Multi-stage Dockerfile for Config Service
# Stage 1: Build with Maven
# Stage 2: Minimal JRE runtime
# ============================================================

# --- Build Stage ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy POM first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-jammy AS runtime

# Security: run as non-root
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --create-home appuser

# Create directories for logs and config
RUN mkdir -p /app/logs /var/config-repo && \
    chown -R appuser:appgroup /app /var/config-repo

WORKDIR /app

# Copy the built JAR
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose Config Server port
EXPOSE 8888

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -sf http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
