# Use official OpenJDK 17 as base image
FROM eclipse-temurin:17-jdk as build

# Set the working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Give execution permission to Gradle wrapper
RUN chmod +x gradlew

# Copy the source code
COPY src src

# Build the application
RUN ./gradlew clean bootJar

# Use a minimal JDK image for production
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port (adjust if needed)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]

# Install dependencies
# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libappindicator3-1 \
    libasound2 \
    libgbm-dev \
    libgtk-3-0 \
    libnss3 \
    lsb-release \
    xdg-utils \
    && rm -rf /var/lib/apt/lists/*

# Install Google Chrome
RUN wget -q -O google-chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    dpkg -i google-chrome.deb || apt-get -fy install && \
    rm google-chrome.deb

# Set environment variable for Chrome binary
ENV CHROME_BIN=/usr/bin/google-chrome

# Get Chrome version
RUN CHROME_VERSION=$(google-chrome --version | awk '{print $3}') && \
    echo "Detected Chrome version: $CHROME_VERSION" && \
    CHROMEDRIVER_VERSION=$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json" | \
    jq -r --arg ver "$CHROME_VERSION" '.channels.Stable.versions[] | select(.version | startswith($ver[:4])) | .version') && \
    echo "Matching ChromeDriver version: $CHROMEDRIVER_VERSION" && \
    wget -q -O /tmp/chromedriver.zip "https://storage.googleapis.com/chrome-for-testing-public/$CHROMEDRIVER_VERSION/linux64/chromedriver-linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    rm /tmp/chromedriver.zip && \
    chmod +x /usr/local/bin/chromedriver

# Set ChromeDriver environment variable
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver
