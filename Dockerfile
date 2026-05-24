# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom and source code
COPY pom.xml .
COPY src ./src
# Build the JAR file, skipping tests to avoid requiring a live database during the Docker build
RUN mvn clean package -DskipTests=true

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built JAR from the build stage (using wildcard to match the version)
COPY --from=build /app/target/*.jar app.jar
# Expose the default Spring Boot port
EXPOSE 8080
# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
