# ============================================================
# Stage 1: Build
# Uses full Maven + JDK image to compile and package the app
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# ---- Layer 1: Dependency resolution (cached) ----
# Copy only pom.xml first. Docker caches this layer.
# It only rebuilds when pom.xml changes (rare), not on every code change.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# ---- Layer 2: Source compilation and packaging ----
# Copy source code after dependencies. This layer rebuilds frequently,
# but the heavy dependency download above stays cached.
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# Uses minimal JRE (no JDK, no Maven) = smaller, more secure
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# ---- Create non-root user ----
# Running as root is a security risk. If the app is compromised,
# the attacker has root access to the container.
RUN addgroup -S buraqgroup && \
    adduser -S buraquser -G buraqgroup

# ---- Copy the built JAR from Stage 1 ----
# The spring-boot-maven-plugin produces a single fat JAR.
# Using *.jar handles version changes without updating the Dockerfile.
COPY --from=builder /app/target/*.jar app.jar

# ---- Create uploads directory ----
# Required by app.storage.path=uploads/documents in application.properties.
# Ownership must be set so the non-root user can write files.
RUN mkdir -p /app/uploads/documents && \
    chown -R buraquser:buraqgroup /app

# ---- Switch to non-root user ----
USER buraquser

# ---- Expose the port your app actually listens on ----
# Matches server.port=8081 in application.properties
EXPOSE 8081

# ---- Health check ----
# Uses Spring Boot Actuator. Matches management.endpoints.web.base-path=/api/actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8081/api/actuator/health || exit 1

# ---- Start the application ----
# JVM memory flags: 256MB initial, 512MB max. Adjust per environment via override.
ENTRYPOINT ["java", "-jar", \
    "-Xms256m", "-Xmx512m", \
    "app.jar"]