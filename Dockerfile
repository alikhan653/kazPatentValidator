# Используем официальный образ OpenJDK 17
FROM openjdk:17-jdk-slim

# Устанавливаем ca-certificates (если не установлены)
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Определяем переменные
ENV JAVA_CACERTS_PATH="/usr/local/openjdk-17/lib/security/cacerts"
ENV CERT_ALIAS="kazpatent_cert"
ENV CERT_PATH="/usr/local/share/ca-certificates"
ENV CERT_FILE="kazpatent.crt"
ENV CERT_PASSWORD="changeit"

# Копируем ваш сертификат в контейнер
COPY kazpatent.crt $CERT_PATH/$CERT_FILE

# Импортируем сертификат в Java Keystore
RUN keytool -import -trustcacerts -keystore $JAVA_CACERTS_PATH -storepass $CERT_PASSWORD -noprompt -alias $CERT_ALIAS -file $CERT_PATH/$CERT_FILE

# Проверяем, добавлен ли сертификат (опционально)
RUN keytool -list -keystore $JAVA_CACERTS_PATH -storepass $CERT_PASSWORD | grep $CERT_ALIAS

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
FROM openjdk:17-jdk-slim

# Set environment variables again in the production image
ENV JAVA_CACERTS_PATH=/usr/lib/jvm/java-17-openjdk/lib/security/cacerts
ENV CERT_ALIAS=kazpatent_cert
ENV CERT_PASSWORD=changeit

# Set the working directory
WORKDIR /app

# Copy built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy the certificate into the production container and update CA certificates
COPY _.kazpatent.kz.crt $CERT_PATH
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/* && update-ca-certificates

# Import certificate into Java keystore in production (Corrected Path)
RUN keytool -import -trustcacerts -keystore $JAVA_CACERTS_PATH -storepass $CERT_PASSWORD -noprompt -alias $CERT_ALIAS -file $CERT_PATH/_.kazpatent.kz.crt

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
RUN wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/133.0.6943.141/linux64/
