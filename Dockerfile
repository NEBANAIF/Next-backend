# ============================================================
#  Neba Backend — Multi-stage Docker build
#  Stage 1: Maven build (Java 21)
#  Stage 2: Minimal JRE runtime image
# ============================================================

# ── Stage 1: Build ────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy dependency manifest first for layer caching
COPY pom.xml .
# Pre-download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B -q

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Stage 2: Runtime ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Health check dependency
RUN apk add --no-cache curl

# Copy artifact from build stage
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser
EXPOSE 8080

# JVM tuned for containers — G1GC, container-aware memory, fast startup entropy
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:G1HeapRegionSize=4m", \
  "-XX:+OptimizeStringConcat", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
