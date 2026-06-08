# ─────────────────────────────────────────────────
# Stage 1: Build
  # ─────────────────────────────────────────────────
  FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
  
  WORKDIR /app
  
  # Copy pom.xml first — Docker caches this layer
  # So if only your source code changes, Maven won't re-download all dependencies
  COPY pom.xml .
  RUN mvn dependency:go-offline -B
  
  # Now copy source code
  COPY src ./src
  
  # Build the jar — skip tests (tests run in CI, not during Docker build)
  RUN mvn clean package -DskipTests -B
  
  # ─────────────────────────────────────────────────
  # Stage 2: Run
  # ─────────────────────────────────────────────────
  FROM eclipse-temurin:21-jre-alpine
  
  WORKDIR /app
  
  # Security: never run as root inside a container
  RUN addgroup -S appgroup && adduser -S appuser -G appgroup
  
  # Copy ONLY the built jar from stage 1 — nothing else
  COPY --from=builder /app/target/*.jar app.jar
  
  # Give ownership to the non-root user
  RUN chown appuser:appgroup app.jar
  
  USER appuser
  
  EXPOSE 8080
  
  ENTRYPOINT [ \
  "java", \
  "-Dspring.profiles.active=prod", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", \
  "app.jar" \
]