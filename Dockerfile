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
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | tee /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable

# Install ChromeDriver (Get the correct version)
RUN CHROME_VERSION=$(google-chrome --version | awk '{print $3}' | cut -d '.' -f 1) \
    && CHROME_DRIVER_VERSION=$(curl -sS https://chromedriver.storage.googleapis.com/LATEST_RELEASE_$CHROME_VERSION) \
    && wget -q -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/${CHROME_DRIVER_VERSION}/chromedriver_linux64.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ \
    && chmod +x /usr/local/bin/chromedriver \
    && rm /tmp/chromedriver.zip

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Set environment variable for ChromeDriver path
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver

# Run the application
CMD ["java", "-jar", "app.jar"]
