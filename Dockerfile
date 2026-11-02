# ==============================================================================
# Stage 1: Build JAR using Maven
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================================
# Stage 2: Minimal Production Runtime
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Security: Create non-root user & group
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory and set permissions
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy executable jar from builder stage
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose Spring Boot port (context-path /api/v1)
EXPOSE 8080

# Container-aware JVM memory options
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

