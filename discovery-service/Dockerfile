# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime

# Security: run as non-root user
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup --shell /usr/sbin/nologin appuser

WORKDIR /app

# Copy built artifact
COPY --from=build /build/target/*.jar app.jar

# Create log directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose Eureka server port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8761/actuator/health || exit 1

# JVM tuning for containerized environments
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
