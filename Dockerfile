# Stage 1: Build (using pre-built artifacts)
# To build locally first: ./gradlew assemble -x test
FROM eclipse-temurin:25-jre-jammy AS build

WORKDIR /app

# Copy the entire quarkus-app directory
COPY build/quarkus-app ./quarkus-app

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the quarkus-app from build stage
COPY --from=build /app/quarkus-app ./quarkus-app

# Expose the HTTP port
EXPOSE 8080

# Set Java options for production
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dquarkus.http.host=0.0.0.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/quarkus-app/quarkus-run.jar"]
