# Stage 1: Build the application
FROM gradle:8.12-jdk21 AS builder

WORKDIR /app

# Copy gradle files first for better caching
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the fat JAR
RUN gradle buildFatJar --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /app/build/libs/*-all.jar app.jar

RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# JVM memory settings for 1GB VPS
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Run the application in production mode
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dktor.development=false -jar app.jar"]
