# Use Selenium Standalone Chrome (includes Chrome & ChromeDriver)
FROM selenium/standalone-chrome:latest AS selenium

# Use official OpenJDK 17 as base image for building
FROM eclipse-temurin:17-jdk AS build

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Give execution permission to Gradle wrapper
RUN chmod +x gradlew

# Copy the source code
COPY src src

# Build the application
RUN ./gradlew clean bootJar

# Use Selenium Chrome as final runtime image (includes Chrome & ChromeDriver)
FROM selenium/standalone-chrome:latest

# Set working directory
WORKDIR /app

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
