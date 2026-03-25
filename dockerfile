# ================================================================
# PANDADOCS API - PRODUCTION DOCKERFILE
# ================================================================
# Multi-stage build optimized for Google Cloud Run
# ================================================================

# ================================================================
# STAGE 1: BUILD
# ================================================================
FROM maven:3.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build argument to bust cache (AFTER dependency download)
ARG CACHE_BUST=unknown

# Copy source and build
# Cache bust: print arg to invalidate cache when needed
RUN echo "Cache bust: ${CACHE_BUST}"
COPY src ./src
RUN mvn clean package -DskipTests -B && \
    echo "=== BUILD COMPLETED ===" && \
    echo "Listing all files in target:" && \
    ls -la /app/target/ && \
    echo "=== JAR FILES ONLY ===" && \
    find /app/target -name "*.jar" -type f

# ================================================================
# STAGE 2: RUNTIME
# ================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage (explicit filename)
COPY --from=build --chown=spring:spring /app/target/api-0.0.1-SNAPSHOT.jar app.jar

# Verify JAR exists
RUN ls -la /app/app.jar

# Firebase credentials will be injected via Cloud Run Secret Manager
# No need to copy here

# Switch to non-root user
USER spring:spring

# Cloud Run uses PORT environment variable (default 8080)
EXPOSE 8080

# JVM optimizations for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]