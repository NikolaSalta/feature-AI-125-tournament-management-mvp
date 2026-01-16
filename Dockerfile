# Multi-stage build для минимального размера образа
FROM gradle:9.2.1-jdk17 AS builder

WORKDIR /home/gradle/project

# Копируем Gradle конфигурацию
COPY build.gradle .
COPY settings.gradle .

# Копируем исходный код
COPY src src

# Сборка приложения (без тестов для ускорения)
RUN gradle bootJar -x test --no-daemon

# ========================================
# Runtime stage - минимальный образ
# ========================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Устанавливаем curl для healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Создаем пользователя для безопасности
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Копируем JAR из builder stage
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Устанавливаем владельца
RUN chown spring:spring app.jar

# Переключаемся на non-root пользователя
USER spring:spring

# Переменные окружения по умолчанию
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check (используем curl, установленный выше)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose порт
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
