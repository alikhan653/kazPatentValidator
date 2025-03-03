# Use OpenJDK base image for building
FROM openjdk:17-jdk as build

# Set working directory
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

# Use a minimal JRE image for production
FROM eclipse-temurin:17-jre

# Set environment variables
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV CERT_ALIAS=gosreestr
ENV CERT_PATH=/usr/local/share/ca-certificates/_.kazpatent.kz.crt
ENV CACERTS_PATH=/etc/ssl/certs/java/cacerts
ENV STOREPASS=changeit

# Set working directory
WORKDIR /app

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy the certificate into the container
COPY _.kazpatent.kz.crt $CERT_PATH

# Import the certificate into the Java Keystore
RUN keytool -import -trustcacerts -keystore $CACERTS_PATH -storepass $STOREPASS -noprompt -alias $CERT_ALIAS -file $CERT_PATH

# Verify that the certificate was added
RUN keytool -list -keystore $CACERTS_PATH -storepass $STOREPASS | grep $CERT_ALIAS

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]

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

# Install ChromeDriver
RUN wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/133.0.6943.141/linux64/chromedriver-linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /usr/local/bin/chromedriver-linux64

