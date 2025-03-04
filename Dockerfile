# Use OpenJDK base image for the build stage
FROM openjdk:17-jdk as build

# Set environment variables
ENV CERT_PATH=/usr/local/share/ca-certificates/

# Install ca-certificates package to enable update-ca-certificates command
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Copy the certificate into the container
COPY _.kazpatent.kz.crt $CERT_PATH

# Update the CA certificates
RUN update-ca-certificates

# Set the working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Install necessary utilities and Gradle dependencies
RUN apt-get update && apt-get install -y \
    findutils \
    && rm -rf /var/lib/apt/lists/*

# Give execution permission to Gradle wrapper
RUN chmod +x gradlew

# Copy the source code
COPY src src

# Build the application (create the JAR)
RUN ./gradlew clean bootJar

# Use a minimal JDK image for the production stage
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy the certificate into the production container and update CA certificates
COPY _.kazpatent.kz.crt $CERT_PATH
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/* && update-ca-certificates

# Install required dependencies (e.g., wget, curl, unzip, gnupg)
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Install Google Chrome
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | tee /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable

# Install ChromeDriver
RUN wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/133.0.6943.141/linux64/chromedriver-linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /usr/local/bin/chromedriver-linux64

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
