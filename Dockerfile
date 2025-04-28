# Используем официальный образ OpenJDK 17 для этапа сборки
FROM openjdk:17-jdk-slim AS build

# Устанавливаем ca-certificates, если они не установлены
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Определяем переменные для сертификата
ENV JAVA_CACERTS_PATH="/usr/local/openjdk-17/lib/security/cacerts"
ENV CERT_ALIAS="kazpatent_cert"
ENV CERT_PATH="/usr/local/share/ca-certificates"
ENV CERT_FILE="kazpatent.crt"
ENV CERT_PASSWORD="changeit"

# Копируем ваш сертификат в контейнер
COPY _.kazpatent.kz.crt /usr/local/share/ca-certificates/kazpatent.crt


# Проверяем наличие сертификата в контейнере
RUN ls -l $CERT_PATH

# Импортируем сертификат в Java Keystore
RUN keytool -import -trustcacerts -keystore $JAVA_CACERTS_PATH -storepass $CERT_PASSWORD -noprompt -alias $CERT_ALIAS -file /usr/local/share/ca-certificates/kazpatent.crt

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы Gradle и билд-файлы
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Устанавливаем необходимые утилиты и зависимости для Gradle
RUN apt-get update && apt-get install -y \
    findutils \
    && rm -rf /var/lib/apt/lists/*

# Даем права на выполнение Gradle wrapper
RUN chmod +x gradlew

# Копируем исходный код
COPY src src

# Строим приложение (создаем JAR)
RUN ./gradlew clean bootJar

# Используем минимальный образ JDK для продакшн стадии
FROM openjdk:17-jdk-slim

# Устанавливаем переменные окружения снова
ENV JAVA_CACERTS_PATH="/usr/local/openjdk-17/lib/security/cacerts"
ENV CERT_ALIAS="kazpatent_cert"
ENV CERT_PASSWORD="changeit"

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR файл из стадии сборки
COPY --from=build /app/build/libs/*.jar app.jar

# Копируем сертификат в продакшн контейнер и обновляем CA сертификаты
COPY _.kazpatent.kz.crt /usr/local/share/ca-certificates/kazpatent.crt
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/* && update-ca-certificates

# Проверяем наличие сертификата в контейнере перед импортом
RUN ls -l $CERT_PATH

# Импортируем сертификат в Java Keystore в продакшн контейнере
RUN keytool -import -trustcacerts -keystore $JAVA_CACERTS_PATH -storepass $CERT_PASSWORD -noprompt -alias $CERT_ALIAS -file /usr/local/share/ca-certificates/kazpatent.crt

# Устанавливаем необходимые зависимости (например, wget, curl, unzip, gnupg) - по необходимости
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Устанавливаем Google Chrome и ChromeDriver - по необходимости
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | tee /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable

RUN wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/135.0.7049.42/linux64/chromedriver-linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /usr/local/bin/chromedriver-linux64

# Открываем порт приложения
EXPOSE 8080

# Запускаем приложение
CMD ["java", "-jar", "app.jar"]
