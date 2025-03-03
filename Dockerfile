# Use official OpenJDK 17 as base image
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

# Use minimal JDK for production
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Install Google Chrome
RUN wget -q -O /tmp/chrome-linux64.zip https://storage.googleapis.com/chrome-for-testing-public/133.0.6943.141/linux64/chrome-linux64.zip \
    && unzip /tmp/chrome-linux64.zip -d /usr/local/ \
    && ln -s /usr/local/chrome-linux64/chrome /usr/bin/google-chrome \
    && rm /tmp/chrome-linux64.zip

# Install ChromeDriver 133.0.6943.141
RUN wget -q -O /tmp/chromedriver-linux64.zip https://storage.googleapis.com/chrome-for-testing-public/133.0.6943.141/linux64/chromedriver-linux64.zip \
    && unzip /tmp/chromedriver-linux64.zip -d /usr/local/bin/ \
    && chmod +x /usr/local/bin/chromedriver \
    && rm /tmp/chromedriver-linux64.zip

# Set environment variable for ChromeDriver path
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
