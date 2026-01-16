# 🏆 Tournament Service Backend

Микросервис для управления шахматными турнирами с поддержкой JWT аутентификации, интеграцией HashiCorp Vault и Kubernetes.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Coverage](https://img.shields.io/badge/coverage-87%25-green)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen)]()
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.5-blue)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 📋 Содержание

1. [Обзор проекта](#обзор-проекта)
2. [Технологический стек](#технологический-стек)
3. [Архитектура](#архитектура)
4. [Установка и запуск](#установка-и-запуск)
   - [macOS](#macos)
   - [Windows](#windows)
   - [Linux Ubuntu](#linux-ubuntu)
5. [Конфигурация](#конфигурация)
6. [API Документация](#api-документация)
7. [Тестирование](#тестирование)
8. [Deployment](#deployment)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 Обзор проекта

Tournament Service Backend - это RESTful API для управления шахматными турнирами, предоставляющий:

- ✅ **Аутентификация и авторизация** с JWT токенами
- ✅ **CRUD операции** для турниров и пользователей
- ✅ **Интеграция с HashiCorp Vault** для безопасного хранения секретов
- ✅ **Rate Limiting** для защиты от DDoS атак
- ✅ **Optimistic Locking** для предотвращения race conditions
- ✅ **Миграции БД** через Flyway
- ✅ **OpenAPI/Swagger** документация
- ✅ **Kubernetes-ready** с ArgoCD GitOps

### Основные возможности:

- Регистрация и аутентификация пользователей
- Создание и управление турнирами
- Различные форматы турниров (Swiss, Round Robin, Knockout)
- Контроль доступа на основе ролей (USER, ORGANIZER, ADMIN)
- Автоматическая валидация данных
- Seed data для быстрого старта

---

## 🛠 Технологический стек

### Backend:
- **Java 17** - LTS версия (минимум)
- **Spring Boot 3.3.6** - основной фреймворк
- **Spring Cloud 2023.0.5** - микросервисные компоненты
- **Spring Security** - аутентификация и авторизация
- **Spring Data JPA** - работа с БД
- **PostgreSQL 16** - основная БД
- **Flyway** - миграции БД
- **JWT (jjwt 0.12.6)** - токены аутентификации

### Инфраструктура:
- **HashiCorp Vault** - управление секретами
- **Docker & Docker Compose** - контейнеризация
- **Kubernetes** - оркестрация
- **ArgoCD** - GitOps CD
- **Terraform** - IaC для Vault

### Инструменты:
- **Gradle 9.2.1** - сборка проекта
- **Lombok** - уменьшение boilerplate кода
- **Bucket4j 8.10.1** - rate limiting
- **Springdoc OpenAPI 2.6.0** - документация API
- **JUnit 5 + Mockito + AssertJ** - тестирование

### ⚠️ Совместимость версий

| Компонент | Версия | Примечание |
|-----------|--------|------------|
| Spring Boot | 3.3.6 | **НЕ использовать 3.5.x** — несовместимо со Spring Cloud |
| Spring Cloud | 2023.0.5 | Совместим с Spring Boot 3.3.x |
| Springdoc | 2.6.0 | Совместим с Spring Framework 6.1 |
| Java | 17+ | 17, 21 поддерживаются |

---

## 🏗 Архитектура

```
tournament-service-be/
├── src/
│   ├── main/
│   │   ├── java/com/chessai/tournament/
│   │   │   ├── config/           # Конфигурация (Security, Vault, OpenAPI)
│   │   │   ├── controller/       # REST контроллеры
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA сущности
│   │   │   ├── exception/        # Обработка ошибок
│   │   │   ├── repository/       # JPA репозитории
│   │   │   ├── security/         # JWT, Rate Limiting, UserDetails
│   │   │   └── service/          # Бизнес-логика
│   │   └── resources/
│   │       ├── db/
│   │       │   ├── migration/    # Flyway миграции
│   │       │   └── seed/         # Seed data (dev profile)
│   │       ├── application.yml   # Основная конфигурация
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── application-vault.yml
│   │       └── bootstrap.yml     # Vault конфигурация
│   └── test/                     # Тесты
├── k8s/                          # Kubernetes манифесты
├── build.gradle                  # Gradle конфигурация
└── README.md
```

### Паттерны проектирования:

- **Layered Architecture** - Controller → Service → Repository
- **DTO Pattern** - разделение entity и API моделей
- **Repository Pattern** - абстракция доступа к данным
- **Builder Pattern** - создание сложных объектов
- **Strategy Pattern** - различные типы rate limiting

---

## 🚀 Установка и запуск

### Предварительные требования:

- **Java 17+** (OpenJDK или Oracle JDK, рекомендуется 17 или 21)
- **PostgreSQL 16+**
- **Gradle 9.2.1+** (опционально, есть wrapper)
- **Docker & Docker Compose** (для контейнеризации)
- **Git**

---

## 🍎 macOS

### 1. Установка зависимостей:

```bash
# Установка Homebrew (если еще не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка Java 17 (или 21)
brew install openjdk@17

# Добавление Java в PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Проверка версии Java
java -version

# Установка PostgreSQL
brew install postgresql@16

# Запуск PostgreSQL
brew services start postgresql@16

# Установка Docker Desktop
# Скачайте с https://www.docker.com/products/docker-desktop/
# Или через Homebrew:
brew install --cask docker
```

### 2. Настройка PostgreSQL:

```bash
# Создание пользователя и базы данных
psql postgres

# В psql консоли:
CREATE USER tournament_user WITH PASSWORD 'vault_dev_password';
CREATE DATABASE tournament_db OWNER tournament_user;
GRANT ALL PRIVILEGES ON DATABASE tournament_db TO tournament_user;
\q
```

### 3. Клонирование проекта:

```bash
cd ~/Desktop
git clone https://github.com/your-org/tournament-service-be.git
cd tournament-service-be
```

### 4. Настройка переменных окружения:

```bash
# Создание .env файла
cp .env.example .env

# Редактирование .env
nano .env
```

Содержимое `.env`:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tournament_db
DB_USERNAME=tournament_user
DB_PASSWORD=vault_dev_password

JWT_SECRET=$(openssl rand -base64 32)
JWT_ACCESS_EXPIRATION=1800000
JWT_REFRESH_EXPIRATION=604800000

SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### 5. Запуск приложения:

```bash
# Загрузка переменных окружения
export $(cat .env | xargs)

# Запуск через Gradle Wrapper
./gradlew bootRun

# Или сборка и запуск JAR
./gradlew build
java -jar build/libs/tournament-service-be-0.0.1-SNAPSHOT.jar
```

### 6. Проверка работы:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## 🪟 Windows

### 1. Установка зависимостей:

#### Через Chocolatey (рекомендуется):

```powershell
# Установка Chocolatey (PowerShell от администратора)
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Установка Java 17 (или 21)
choco install openjdk17 -y

# Установка PostgreSQL
choco install postgresql16 -y

# Установка Git
choco install git -y

# Установка Docker Desktop
choco install docker-desktop -y

# Перезапуск PowerShell для применения изменений
```

#### Ручная установка:

1. **Java 21:**
   - Скачайте с https://adoptium.net/
   - Установите и добавьте в PATH
   - Проверка: `java -version`

2. **PostgreSQL:**
   - Скачайте с https://www.postgresql.org/download/windows/
   - Установите с паролем для postgres
   - Добавьте `C:\Program Files\PostgreSQL\16\bin` в PATH

3. **Git:**
   - Скачайте с https://git-scm.com/download/win
   - Установите с настройками по умолчанию

4. **Docker Desktop:**
   - Скачайте с https://www.docker.com/products/docker-desktop/
   - Установите и запустите

### 2. Настройка PostgreSQL:

```powershell
# Подключение к PostgreSQL
psql -U postgres

# В psql консоли:
CREATE USER tournament_user WITH PASSWORD 'vault_dev_password';
CREATE DATABASE tournament_db OWNER tournament_user;
GRANT ALL PRIVILEGES ON DATABASE tournament_db TO tournament_user;
\q
```

### 3. Клонирование проекта:

```powershell
cd $HOME\Desktop
git clone https://github.com/your-org/tournament-service-be.git
cd tournament-service-be
```

### 4. Настройка переменных окружения:

```powershell
# Создание .env файла
Copy-Item .env.example .env

# Редактирование .env
notepad .env
```

Содержимое `.env`:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tournament_db
DB_USERNAME=tournament_user
DB_PASSWORD=vault_dev_password

JWT_SECRET=your_base64_encoded_secret_here
JWT_ACCESS_EXPIRATION=1800000
JWT_REFRESH_EXPIRATION=604800000

SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

Генерация JWT_SECRET:
```powershell
# PowerShell
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### 5. Запуск приложения:

```powershell
# Загрузка переменных окружения
Get-Content .env | ForEach-Object {
    $name, $value = $_.split('=')
    Set-Item -Path "env:$name" -Value $value
}

# Запуск через Gradle Wrapper
.\gradlew.bat bootRun

# Или сборка и запуск JAR
.\gradlew.bat build
java -jar build\libs\tournament-service-be-0.0.1-SNAPSHOT.jar
```

### 6. Проверка работы:

```powershell
# Health check
Invoke-RestMethod http://localhost:8080/actuator/health

# Swagger UI
Start-Process http://localhost:8080/swagger-ui.html
```

---

## 🐧 Linux Ubuntu

### 1. Установка зависимостей:

```bash
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Установка Java 17 (или 21)
sudo apt install openjdk-17-jdk -y

# Проверка версии
java -version

# Установка PostgreSQL 16
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt update
sudo apt install postgresql-16 postgresql-contrib-16 -y

# Запуск PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Установка Git
sudo apt install git -y

# Установка Docker
sudo apt install apt-transport-https ca-certificates curl software-properties-common -y
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io -y

# Добавление пользователя в группу docker
sudo usermod -aG docker $USER
newgrp docker

# Установка Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. Настройка PostgreSQL:

```bash
# Переключение на пользователя postgres
sudo -u postgres psql

# В psql консоли:
CREATE USER tournament_user WITH PASSWORD 'vault_dev_password';
CREATE DATABASE tournament_db OWNER tournament_user;
GRANT ALL PRIVILEGES ON DATABASE tournament_db TO tournament_user;
\q
```

### 3. Клонирование проекта:

```bash
cd ~/Desktop
git clone https://github.com/your-org/tournament-service-be.git
cd tournament-service-be
```

### 4. Настройка переменных окружения:

```bash
# Создание .env файла
cp .env.example .env

# Редактирование .env
nano .env
```

Содержимое `.env`:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tournament_db
DB_USERNAME=tournament_user
DB_PASSWORD=vault_dev_password

JWT_SECRET=$(openssl rand -base64 32)
JWT_ACCESS_EXPIRATION=1800000
JWT_REFRESH_EXPIRATION=604800000

SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### 5. Запуск приложения:

```bash
# Загрузка переменных окружения
export $(cat .env | xargs)

# Запуск через Gradle Wrapper
./gradlew bootRun

# Или сборка и запуск JAR
./gradlew build
java -jar build/libs/tournament-service-be-0.0.1-SNAPSHOT.jar
```

### 6. Проверка работы:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
xdg-open http://localhost:8080/swagger-ui.html
# Или
firefox http://localhost:8080/swagger-ui.html
```

---

## 🐳 Запуск через Docker Compose (Все платформы)

### 1. Создание docker-compose.yml:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: tournament-postgres
    environment:
      POSTGRES_DB: tournament_db
      POSTGRES_USER: tournament_user
      POSTGRES_PASSWORD: vault_dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tournament_user -d tournament_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  vault:
    image: hashicorp/vault:1.21.1
    container_name: tournament-vault
    cap_add:
      - IPC_LOCK
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: dev-token-123
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    ports:
      - "8200:8200"
    command: server -dev

  app:
    build: .
    container_name: tournament-service
    depends_on:
      postgres:
        condition: service_healthy
      vault:
        condition: service_started
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: tournament_db
      DB_USERNAME: tournament_user
      DB_PASSWORD: vault_dev_password
      VAULT_HOST: vault
      VAULT_PORT: 8200
      VAULT_TOKEN: dev-token-123
      VAULT_ENABLED: false
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
```

### 2. Dockerfile (уже создан):

```dockerfile
# Multi-stage build для минимального размера образа
FROM gradle:9.2.1-jdk17 AS builder
WORKDIR /home/gradle/project
COPY build.gradle settings.gradle ./
COPY src src
RUN gradle bootJar -x test --no-daemon

# Runtime stage - минимальный образ
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3. Запуск:

```bash
# Сборка приложения
./gradlew build

# Запуск всех сервисов
docker-compose up -d

# Просмотр логов
docker-compose logs -f app

# Остановка
docker-compose down
```

---

## ⚙️ Конфигурация

### Профили Spring:

- **dev** - разработка (seed data, debug логи)
- **prod** - production (без seed data, минимальные логи)
- **test** - тестирование (H2 in-memory DB)
- **vault** - интеграция с Vault

### Переменные окружения:

| Переменная | Описание | Обязательная | По умолчанию |
|------------|----------|--------------|--------------|
| `DB_HOST` | Хост PostgreSQL | Да | localhost |
| `DB_PORT` | Порт PostgreSQL | Да | 5432 |
| `DB_NAME` | Имя БД | Да | tournament_db |
| `DB_USERNAME` | Пользователь БД | Да | tournament_user |
| `DB_PASSWORD` | Пароль БД | Да | - |
| `JWT_SECRET` | Секрет для JWT (base64) | Да | - |
| `JWT_ACCESS_EXPIRATION` | Время жизни access token (мс) | Нет | 1800000 (30 мин) |
| `JWT_REFRESH_EXPIRATION` | Время жизни refresh token (мс) | Нет | 604800000 (7 дней) |
| `SERVER_PORT` | Порт приложения | Нет | 8080 |
| `SPRING_PROFILES_ACTIVE` | Активные профили | Нет | dev |
| `VAULT_ENABLED` | Включить Vault | Нет | false |
| `VAULT_HOST` | Хост Vault | Нет | 127.0.0.1 |
| `VAULT_PORT` | Порт Vault | Нет | 8200 |
| `VAULT_TOKEN` | Токен Vault | Нет | - |

### Генерация JWT_SECRET:

**macOS/Linux:**
```bash
openssl rand -base64 32
```

**Windows PowerShell:**
```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

---

## 📚 API Документация

### Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification:
```
http://localhost:8080/v3/api-docs
```

### Основные эндпоинты:

#### Authentication:

```bash
# Регистрация
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}

# Логин
POST /api/v1/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "admin",
  "password": "Admin123!"
}

# Получение текущего пользователя
GET /api/v1/auth/me
Authorization: Bearer <token>

# Обновление токена
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

#### Tournaments:

```bash
# Получение всех турниров
GET /api/v1/tournaments?page=0&size=10

# Получение турнира по ID
GET /api/v1/tournaments/{id}

# Создание турнира
POST /api/v1/tournaments
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Spring Championship 2026",
  "description": "Annual spring tournament",
  "format": "SWISS",
  "startDate": "2026-03-01T10:00:00",
  "maxParticipants": 32,
  "timeControlMinutes": 15,
  "timeIncrementSeconds": 10,
  "isPublic": true
}

# Обновление турнира
PUT /api/v1/tournaments/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Tournament",
  ...
}

# Удаление турнира
DELETE /api/v1/tournaments/{id}
Authorization: Bearer <token>
```

### Примеры использования:

#### cURL (macOS/Linux):

```bash
# Логин
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}' \
  | jq -r .accessToken)

# Создание турнира
curl -X POST http://localhost:8080/api/v1/tournaments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Test Tournament",
    "format":"SWISS",
    "startDate":"2026-03-01T10:00:00",
    "maxParticipants":16,
    "isPublic":true
  }'
```

#### PowerShell (Windows):

```powershell
# Логин
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"usernameOrEmail":"admin","password":"Admin123!"}'

$token = $response.accessToken

# Создание турнира
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/tournaments" `
  -Method POST `
  -Headers @{ "Authorization" = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{
    "name":"Test Tournament",
    "format":"SWISS",
    "startDate":"2026-03-01T10:00:00",
    "maxParticipants":16,
    "isPublic":true
  }'
```

---

## 🧪 Тестирование

### Запуск всех тестов:

```bash
# macOS/Linux
./gradlew test

# Windows
.\gradlew.bat test
```

### Запуск integration tests:

```bash
./gradlew test --tests "*IntegrationTest"
```

### Запуск с покрытием:

```bash
./gradlew test jacocoTestReport

# Отчет: build/reports/jacoco/test/html/index.html
```

### Postman коллекция:

1. Импортируйте `Tournament_Service_Tests.postman_collection.json`
2. Нажмите "Run Collection"
3. Проверьте результаты (15+ тестов)

---

## 🚢 Deployment

### Kubernetes:

```bash
# Применение манифестов
kubectl apply -f k8s/

# Проверка статуса
kubectl get pods -n chessverse-tournament

# Логи
kubectl logs -f deployment/tournament-service -n chessverse-tournament
```

### ArgoCD:

```bash
# Синхронизация приложения
argocd app sync tournament-service

# Проверка статуса
argocd app get tournament-service
```

---

## 🔧 Troubleshooting

### Проблема: Приложение не запускается

**Решение:**
```bash
# Проверка Java
java -version

# Проверка PostgreSQL
psql -U tournament_user -d tournament_db -h localhost

# Проверка портов
# macOS/Linux:
lsof -i :8080
# Windows:
netstat -ano | findstr :8080
```

### Проблема: Ошибка подключения к БД

**Решение:**
```bash
# Проверка что PostgreSQL запущен
# macOS:
brew services list | grep postgresql
# Linux:
sudo systemctl status postgresql
# Windows:
Get-Service postgresql*

# Проверка подключения
psql -U tournament_user -d tournament_db -h localhost
```

### Проблема: JWT ошибки

**Решение:**
- Проверьте что `JWT_SECRET` установлен и имеет минимум 256 бит (32 байта в base64)
- Проверьте время жизни токенов

---

## 📞 Контакты и поддержка

- **Email:** support@tournament.example.com
- **GitHub:** https://github.com/your-org/tournament-service-be
- **Documentation:** https://docs.tournament.example.com

---

## 📄 Лицензия

MIT License - см. [LICENSE](LICENSE) файл

---

## 🙏 Благодарности

- Spring Boot Team
- HashiCorp Vault
- PostgreSQL Community

---

**Версия:** 1.0.0-MVP  
**Последнее обновление:** 2026-01-15

---

## 📊 Результаты тестирования

| Категория | Тестов | Пройдено | Покрытие |
|-----------|--------|----------|----------|
| Unit Tests | 93 | ✅ 93 | ~85% |
| Integration Tests | 127 | ✅ 127 | ~90% |
| **Всего** | **220** | **✅ 220** | **~87%** |

### Docker Deployment Status

```
✅ PostgreSQL:        Healthy (port 5433)
✅ Tournament Service: Healthy (port 8080)
✅ Health endpoint:    {"status":"UP"}
✅ Swagger UI:         http://localhost:8080/swagger-ui/index.html
✅ OpenAPI Spec:       http://localhost:8080/v3/api-docs
✅ JWT Authentication: Working
```
