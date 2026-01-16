# Подробное руководство по тестированию AI-132: Initialize Tournament Service Backend

## 📌 Оглавление
1. [Подготовка окружения](#1-подготовка-окружения)
2. [Получение кода](#2-получение-кода)
3. [Установка зависимостей](#3-установка-зависимостей)
4. [Настройка базы данных](#4-настройка-базы-данных)
5. [Детальное тестирование](#5-детальное-тестирование)
6. [Документирование результатов](#6-документирование-результатов)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Подготовка окружения

### 1.1 Необходимое ПО

| Компонент | Версия | Проверка версии | Ссылка для скачивания |
|-----------|--------|-----------------|----------------------|
| Java | 17+ | `java -version` | https://adoptium.net/temurin/releases/ |
| Git | 2.x | `git --version` | https://git-scm.com/downloads |
| PostgreSQL | 12+ | `psql --version` | https://www.postgresql.org/download/ |
| cURL | любая | `curl --version` | Обычно предустановлен |

---

## 2. Получение кода

```bash
# Клонирование репозитория
git clone https://github.com/NikolaSalta/tournament-service-be.git
cd tournament-service-be
```

---

## 3. Установка зависимостей

```bash
chmod +x mvnw
./mvnw clean install -DskipTests
```

---

## 4. Настройка базы данных

```sql
CREATE DATABASE tournament_db;
CREATE USER tournament_user WITH PASSWORD 'test123';
GRANT ALL PRIVILEGES ON DATABASE tournament_db TO tournament_user;
```

---

## 5. Детальное тестирование

### TEST CASE 1: Запуск приложения
```bash
export DB_PASSWORD=test123
./mvnw spring-boot:run
```

### TEST CASE 2: Health Check
```bash
curl http://localhost:8080/actuator/health
```

### TEST CASE 3: Swagger UI
Браузер: http://localhost:8080/swagger-ui/index.html

### TEST CASE 4: OpenAPI Docs
```bash
curl http://localhost:8080/v3/api-docs
```

### TEST CASE 5: Проверка отсутствия бизнес-логики
```bash
find src -name "*Entity.java" -o -name "*Controller.java" -o -name "*Service.java"
```

### TEST CASE 6: Security Dev Mode
```bash
curl -I http://localhost:8080/actuator/health
curl -I http://localhost:8080/swagger-ui/index.html
```

---

## 6. Документирование результатов

Создайте отчет TEST_REPORT_AI-132.md со всеми результатами и скриншотами.

---

## 7. Troubleshooting

### Port 8080 already in use
```bash
lsof -i :8080
kill -9 [PID]
```

### PostgreSQL connection refused
```bash
pg_isready
psql -h localhost -U tournament_user -d tournament_db
```
