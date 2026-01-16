# Docker: Подробная документация

> **Дата**: 2026-01-02  
> **Версия**: 1.0

---

## 📋 Содержание

1. [Обзор Docker-конфигурации](#обзор-docker-конфигурации)
2. [Dockerfile](#dockerfile)
3. [Docker Compose](#docker-compose)
4. [Запуск проекта](#запуск-проекта)
5. [Postman Collection](#postman-collection)

---

## 🐳 Обзор Docker-конфигурации

### Созданные файлы

| Файл                 | Описание                                   |
| -------------------- | ------------------------------------------ |
| `Dockerfile`         | Multi-stage build для оптимального размера |
| `docker-compose.yml` | Оркестрация всех сервисов                  |
| `postman/*.json`     | Коллекция для тестирования API             |

### Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Network                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   PostgreSQL    │  │ Tournament     │  │   Vault      │ │
│  │   :5432         │←─│ Service :8080  │──│   :8200      │ │
│  │   (Required)    │  │ (Main App)     │  │   (Optional) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Dockerfile

### Структура (Multi-stage Build)

```dockerfile
# Stage 1: Builder - компиляция
FROM eclipse-temurin:17-jdk-alpine AS builder
# ... сборка с Gradle

# Stage 2: Runtime - минимальный образ
FROM eclipse-temurin:17-jre-alpine
# ... только JAR и JRE
```

### Особенности

| Аспект           | Реализация                                  |
| ---------------- | ------------------------------------------- |
| **Base Image**   | eclipse-temurin:17-jre-alpine (минимальный) |
| **Размер**       | ~150-200 MB (вместо 500+ MB)                |
| **Security**     | Non-root пользователь `spring`              |
| **Health Check** | wget на /actuator/health                    |
| **JVM**          | Configurable через JAVA_OPTS                |

### Переменные окружения

```dockerfile
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod
```

### Сборка образа

```bash
# Сборка
docker build -t tournament-service:latest .

# Проверка размера
docker images tournament-service
```

---

## 🐙 Docker Compose

### Сервисы

| Сервис               | Образ                | Порт | Описание              |
| -------------------- | -------------------- | ---- | --------------------- |
| `postgres`           | postgres:16-alpine   | 5432 | База данных           |
| `tournament-service` | ./Dockerfile         | 8080 | Приложение            |
| `vault`              | hashicorp/vault:1.15 | 8200 | Секреты (опционально) |

### Конфигурация PostgreSQL

```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: tournament_db
    POSTGRES_USER: tournament_user
    POSTGRES_PASSWORD: tournament_password
  volumes:
    - postgres_data:/var/lib/postgresql/data # Persistence
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U tournament_user"]
```

### Конфигурация Tournament Service

```yaml
tournament-service:
  build: .
  depends_on:
    postgres:
      condition: service_healthy # Ждёт готовности DB
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tournament_db
    JWT_SECRET: <base64-secret>
```

### Vault (опционально)

```yaml
vault:
  image: hashicorp/vault:1.15
  environment:
    VAULT_DEV_ROOT_TOKEN_ID: dev-root-token
  profiles:
    - vault # Только с --profile vault
```

---

## 🚀 Запуск проекта

### Вариант 1: Docker Compose (рекомендуется)

```bash
# Запуск PostgreSQL + App
docker-compose up -d

# Проверка статуса
docker-compose ps

# Логи
docker-compose logs -f tournament-service

# Остановка
docker-compose down
```

### Вариант 2: С Vault

```bash
# Запуск с Vault
docker-compose --profile vault up -d
```

### Вариант 3: Локально (без Docker)

```bash
# 1. Запустить PostgreSQL
docker-compose up -d postgres

# 2. Запустить приложение локально
./gradlew bootRun
```

### Проверка работоспособности

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}'
```

---

## 📬 Postman Collection

### Расположение

```
postman/Tournament_Service_API.postman_collection.json
```

### Импорт в Postman

1. Открыть Postman
2. **Import** → **File**
3. Выбрать `postman/Tournament_Service_API.postman_collection.json`
4. Начать тестирование

### Доступные запросы

| Категория | Запрос           | Метод | Путь                  |
| --------- | ---------------- | ----- | --------------------- |
| Health    | Health Status    | GET   | /actuator/health      |
| Health    | Info             | GET   | /actuator/info        |
| Auth      | Register User    | POST  | /api/v1/auth/register |
| Auth      | Login            | POST  | /api/v1/auth/login    |
| Auth      | Refresh Token    | POST  | /api/v1/auth/refresh  |
| Auth      | Get Current User | GET   | /api/v1/auth/me       |
| Swagger   | Swagger UI       | GET   | /swagger-ui.html      |
| Swagger   | OpenAPI JSON     | GET   | /v3/api-docs          |
| Test      | Rate Limit Test  | POST  | /api/v1/auth/login    |

### Автоматическое сохранение токенов

При успешном Login/Register токены автоматически сохраняются в переменные коллекции:

- `{{accessToken}}`
- `{{refreshToken}}`

### Тест Rate Limiting

Запустите запрос "Rate Limit Test" 11 раз:

- Запросы 1-10: `401 Unauthorized`
- Запрос 11: `429 Too Many Requests`

---

## 🔧 Команды для разработки

### Docker

```bash
# Пересборка образа
docker-compose build --no-cache

# Очистка volumes
docker-compose down -v

# Просмотр логов DB
docker-compose logs postgres

# Запуск только DB
docker-compose up -d postgres

# Shell в контейнере
docker-compose exec tournament-service sh
```

### Gradle

```bash
# Сборка без тестов
./gradlew build -x test

# Запуск тестов
./gradlew test

# Создание JAR
./gradlew bootJar

# Запуск локально
./gradlew bootRun
```

---

## 📊 Результаты проверки

| Проверка                   | Статус              |
| -------------------------- | ------------------- |
| `./gradlew build`          | ✅ BUILD SUCCESSFUL |
| Dockerfile создан          | ✅                  |
| docker-compose.yml создан  | ✅                  |
| Postman collection создана | ✅                  |
| Swagger задокументирован   | ✅                  |

---

> **Автор**: AI Assistant  
> **Дата**: 2026-01-02
