# ==============================================================================
# Stage 1: Build JAR using Maven
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal Production Runtime for Render / Railway (512MB RAM)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Security: Create non-root user & group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create uploads directory and set permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose Spring Boot port
EXPOSE 8080

# Healthcheck targeting Actuator health endpoint under context-path /api/v1
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/api/v1/actuator/health || exit 1

# Container-aware JVM memory options optimized for 512MB RAM container
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
