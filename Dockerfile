# ── Stage 1: build ──────────────────────────────────────────────────────────
# Uses the official Maven image to compile and package the fat JAR.
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Pull dependencies first so Docker can cache this layer separately from source changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
# Lean JRE image — no compiler, no Maven, much smaller final image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy only the fat JAR produced by maven-shade-plugin
COPY --from=builder /app/target/uno.jar ./uno.jar

# Copy the players file (used by future features; harmless if absent)
COPY players.txt .

# Create logs directory so Logback can write game logs at runtime
RUN mkdir -p logs

# Default: 3 bots, 1 game, non-quiet output
ENTRYPOINT ["java", "-jar", "uno.jar"]
CMD ["--bots", "3", "--games", "1"]
