# Многостадийная сборка для оптимизации размера образа
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

# Копируем Gradle wrapper и зависимости
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

# Копируем исходный код
COPY src src

# Даем права на выполнение gradlew
RUN chmod +x gradlew

# Собираем приложение
RUN ./gradlew clean bootJar -x test

# Финальный образ
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Устанавливаем временную зону
RUN apk add --no-cache tzdata
ENV TZ=Europe/Moscow

# Создаем пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем JAR из стадии сборки
COPY --from=builder /app/build/libs/*.jar app.jar

# Оптимизация для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Открываем порт
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java -javaagent:/otel/opentelemetry-javaagent.jar $JAVA_OPTS -jar /app/app.jar"]