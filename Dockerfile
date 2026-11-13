# Stage 1: Build application with Maven
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy Maven files first for caching dependencies
COPY pom.xml ./
COPY .mvn .mvn/
COPY mvnw ./
RUN chmod +x ./mvnw || true

# Download dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=build /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
