# Use a lightweight Java image
FROM openjdk:17-slim

# Set working directory
WORKDIR /app

# Update package lists and install dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    unzip \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libgbm-dev \
    libgtk-3-0 \
    libnss3 \
    lsb-release \
    xdg-utils \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Google Chrome
RUN wget -q -O google-chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    dpkg -i google-chrome.deb || apt-get -fy install && \
    rm google-chrome.deb

# Set environment variable for Chrome binary
ENV CHROME_BIN=/usr/bin/google-chrome

# Get Chrome version and install matching ChromeDriver
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

# Copy project files
COPY target/myapp.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
