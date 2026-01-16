# QA Team: Подробный отчёт об изменениях v0.7.0

> **Дата**: 2026-01-02  
> **Статус**: ✅ Все 49 тестов пройдены  
> **Документ для**: QA Team

---

## 📋 Содержание

1. [Сводка изменений](#сводка-изменений)
2. [Изменение времени токена](#изменение-времени-токена)
3. [Интеграция Caffeine Cache](#интеграция-caffeine-cache)
4. [Исправления безопасности](#исправления-безопасности)
5. [Все изменённые файлы](#все-изменённые-файлы)
6. [Инструкции для тестирования](#инструкции-для-тестирования)
7. [Результаты автотестов](#результаты-автотестов)

---

## 🔄 Сводка изменений

### Версия 0.7.0 — 2026-01-02

| Категория    | Кол-во изменений |
| ------------ | ---------------- |
| Конфигурация | 1                |
| Зависимости  | 1                |
| Безопасность | 4                |
| Bug Fixes    | 3                |
| **Всего**    | **9**            |

### Критичность изменений

| Уровень           | Описание        | Кол-во |
| ----------------- | --------------- | ------ |
| 🔴 Критический    | Security/Memory | 3      |
| 🟡 Средний        | Logic bugs      | 4      |
| 🟢 Незначительный | Config/Docs     | 2      |

---

## ⏱️ Изменение времени токена

### Что изменено

Access Token увеличен с **15 минут** до **30 минут**.

### Затронутые файлы

#### 1. `src/main/resources/application.yml`

```yaml
# БЫЛО:
jwt:
  access-token-expiration: 900000  # 15 минут

# СТАЛО:
jwt:
  access-token-expiration: 1800000  # 30 минут
```

#### 2. `src/main/java/.../security/jwt/JwtProperties.java`

```java
// БЫЛО:
private long accessTokenExpiration = 900000;  // 15 минут

// СТАЛО:
private long accessTokenExpiration = 1800000;  // 30 минут
```

#### 3. `README.md`

```markdown
# БЫЛО:

- **Access Token**: 15 минут
- JWT_ACCESS_EXPIRATION: 900000

# СТАЛО:

- **Access Token**: 30 минут
- JWT_ACCESS_EXPIRATION: 1800000
```

### Как проверить

```bash
# 1. Проверить конфигурацию
grep -r "1800000" src/main/resources/application.yml

# 2. Проверить defaults в Java
grep "accessTokenExpiration" src/main/java/com/chessai/tournament/security/jwt/JwtProperties.java

# 3. Функциональный тест: получить токен и проверить exp claim
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}'
# Декодировать JWT на jwt.io и проверить exp - iat = 1800 сек
```

---

## ☕ Интеграция Caffeine Cache

### Проблема (Memory Leak)

**Файл**: `RateLimitingService.java`

```java
// БЫЛО: Утечка памяти!
private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
```

**Проблема**:

- ConcurrentHashMap растёт бесконечно
- Каждый IP добавляет запись
- Записи НИКОГДА не удаляются
- DDoS → миллионы записей → OutOfMemoryError

### Решение

#### 1. Добавлена зависимость в `build.gradle`

```groovy
// Добавлено после bucket4j:
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
```

| Параметр | Значение                      |
| -------- | ----------------------------- |
| Group    | com.github.ben-manes.caffeine |
| Artifact | caffeine                      |
| Version  | 3.1.8                         |
| Size     | ~600 KB                       |

#### 2. Переписан `RateLimitingService.java`

```java
// СТАЛО: Caffeine с TTL и лимитом
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(10))  // TTL 10 мин
    .maximumSize(10_000)                        // Max 10K записей
    .recordStats()                              // Статистика
    .build();
```

### Параметры Caffeine

| Параметр            | Значение | Назначение                         |
| ------------------- | -------- | ---------------------------------- |
| `expireAfterAccess` | 10 мин   | Удаление через 10 мин неактивности |
| `maximumSize`       | 10,000   | Защита от OOM, LRU eviction        |
| `recordStats`       | true     | Мониторинг hit/miss ratio          |

### Изменённые методы

| Метод           | Было                | Стало                    |
| --------------- | ------------------- | ------------------------ |
| `tryConsume`    | `computeIfAbsent()` | `cache.get(key, loader)` |
| `isAllowed`     | `map.get()`         | `cache.getIfPresent()`   |
| `resetLimit`    | `map.remove()`      | `cache.invalidate()`     |
| `clearCache`    | `map.clear()`       | `cache.invalidateAll()`  |
| `getCacheSize`  | `map.size()`        | `cache.estimatedSize()`  |
| `getCacheStats` | ❌ нет              | ✅ `cache.stats()`       |

### Как проверить

```bash
# 1. Проверить зависимость
grep "caffeine" build.gradle

# 2. Проверить использование Caffeine
grep "Caffeine" src/main/java/com/chessai/tournament/security/ratelimit/RateLimitingService.java

# 3. Проверить компиляцию
./gradlew compileJava

# 4. Нагрузочный тест (rate limiting работает)
for i in {1..15}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/auth/login \
    -X POST -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test","password":"test"}'
done
# После 10 запросов должен быть 429 Too Many Requests
```

---

## 🔐 Исправления безопасности

### 1. IP Spoofing (RateLimitingFilter.java)

**Проблема**: Атакующий мог обойти Rate Limiting, подделывая заголовок `X-Forwarded-For`.

```java
// БЫЛО (уязвимо):
return xForwardedFor.split(",")[0].trim();  // Первый IP - подделывается!

// СТАЛО (безопасно):
String[] ips = xForwardedFor.split(",");
return ips[ips.length - 1].trim();  // Последний IP - от прокси
```

**Как проверить**:

```bash
grep "ips.length - 1" src/main/java/com/chessai/tournament/security/ratelimit/RateLimitingFilter.java
```

---

### 2. JWT Issuer Validation (JwtTokenProvider.java)

**Проблема**: Токены из других сервисов с тем же secret принимались.

```java
// БЫЛО:
return Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token);

// СТАЛО:
return Jwts.parser()
    .verifyWith(secretKey)
    .requireIssuer(jwtProperties.getIssuer())  // Проверка издателя!
    .build()
    .parseSignedClaims(token);
```

**Как проверить**:

```bash
grep "requireIssuer" src/main/java/com/chessai/tournament/security/jwt/JwtTokenProvider.java
```

---

### 3. Неполная проверка refreshToken (AuthService.java)

**Проблема**: Заблокированные пользователи могли обновить токен.

```java
// БЫЛО:
if (!user.isEnabled()) {
    throw new AuthException("User account is disabled");
}

// СТАЛО:
if (!user.isEnabled()) {
    throw new AuthException("User account is disabled");
}
if (!user.isAccountNonLocked()) {
    throw new AuthException("User account is locked");
}
if (!user.isAccountNonExpired()) {
    throw new AuthException("User account is expired");
}
```

**Как проверить**:

```bash
grep "isAccountNonLocked\|isAccountNonExpired" src/main/java/com/chessai/tournament/security/service/AuthService.java
```

---

### 4. Дублирование Role.USER (AuthService.java)

**Проблема**: Роль добавлялась дважды — в конструкторе User и в register().

```java
// БЫЛО:
User user = new User(username, email, password);  // Конструктор добавляет USER
user.addRole(Role.USER);  // Дубликат!

// СТАЛО:
User user = new User(username, email, password);
// Role.USER уже добавлена в конструкторе
```

**Как проверить**:

```bash
# Должен показать 0 результатов:
grep "user.addRole(Role.USER)" src/main/java/com/chessai/tournament/security/service/AuthService.java
```

---

## 🐛 Дополнительные Bug Fixes

### 5. @Size на хешированном пароле (User.java)

```java
// БЫЛО (бессмысленно):
@Size(min = 8, message = "Password must be at least 8 characters")
private String password;  // Хранится хеш 60+ символов!

// СТАЛО:
@NotBlank(message = "Password is required")
// Валидация в RegisterRequest DTO
private String password;
```

---

### 6. ClassCastException (GlobalExceptionHandler.java)

```java
// БЫЛО (потенциальный краш):
String fieldName = ((FieldError) error).getField();

// СТАЛО (безопасно):
if (error instanceof FieldError fieldError) {
    String fieldName = fieldError.getField();
    errors.put(fieldName, error.getDefaultMessage());
} else {
    errors.put(error.getObjectName(), error.getDefaultMessage());
}
```

---

## 📁 Все изменённые файлы

### Таблица изменений

| #   | Файл                          | Тип изменения            | Критичность |
| --- | ----------------------------- | ------------------------ | ----------- |
| 1   | `build.gradle`                | +Caffeine dependency     | 🔴          |
| 2   | `application.yml`             | Token 15→30 мин          | 🟢          |
| 3   | `JwtProperties.java`          | Default 15→30 мин        | 🟢          |
| 4   | `RateLimitingService.java`    | Полная переработка       | 🔴          |
| 5   | `RateLimitingFilter.java`     | IP: first→last           | 🔴          |
| 6   | `AuthService.java`            | -Duplicate role, +Checks | 🟡          |
| 7   | `User.java`                   | -@Size on password       | 🟡          |
| 8   | `GlobalExceptionHandler.java` | +instanceof check        | 🟡          |
| 9   | `JwtTokenProvider.java`       | +requireIssuer           | 🟡          |
| 10  | `README.md`                   | Обновление документации  | 🟢          |
| 11  | `CHANGELOG.md`                | v0.7.0 release notes     | 🟢          |

### Новые файлы документации

| Файл                                        | Описание                    |
| ------------------------------------------- | --------------------------- |
| `docs/BUGFIXES_AND_CAFFEINE_INTEGRATION.md` | Детальное описание Caffeine |
| `docs/SEMGREP_SECURITY_REPORT.md`           | Отчёт Semgrep               |

---

## 🧪 Инструкции для тестирования

### Предварительные требования

```bash
# 1. Java 17
java -version  # openjdk 17.x.x

# 2. PostgreSQL запущен
pg_isready  # /var/run/postgresql:5432 - accepting connections

# 3. База данных создана
psql -c "SELECT 1 FROM pg_database WHERE datname='tournament_db'"
```

### Запуск автотестов

```bash
cd /Users/nikolay/Desktop/Tournament_service_Be/tournament-service-be

# Запуск полного набора тестов (49 test cases)
./qa-team/AI-132-initialize-backend/test-scripts/automated-test.sh
```

### Ручное тестирование

#### Тест 1: Сборка проекта

```bash
./gradlew clean compileJava
# Ожидаемый результат: BUILD SUCCESSFUL
```

#### Тест 2: Запуск приложения

```bash
./gradlew bootRun

# Ожидаемые логи:
# Started TournamentServiceBeApplication in X seconds
# Exposing 1 endpoint(s) beneath base path '/actuator'
```

#### Тест 3: Health Check

```bash
curl http://localhost:8080/actuator/health
# Ожидаемый ответ: {"status":"UP","components":{...}}
```

#### Тест 4: Swagger UI

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
# Ожидаемый код: 200
```

#### Тест 5: Rate Limiting

```bash
# Отправить 11 login-запросов (лимит 10/мин)
for i in {1..11}; do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test","password":"test"}')
  echo "Request $i: $CODE"
done
# Запросы 1-10: 401 (bad credentials)
# Запрос 11: 429 (too many requests)
```

#### Тест 6: Аутентификация (требуется админ)

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}'

# Ожидаемый ответ: {"accessToken":"...", "refreshToken":"...", ...}
```

---

## 📊 Результаты автотестов

### Последний запуск: 2026-01-02 12:54

```
╔══════════════════════════════════════════════════════════════╗
║  TEST SUMMARY                                                ║
╚══════════════════════════════════════════════════════════════╝

  Total Passed: 49
  Total Failed: 0
  Total Tests:  49

  Result: ✅ ALL TESTS PASSED
```

### Breakdown по категориям

| Suite                  | Тесты | Результат |
| ---------------------- | ----- | --------- |
| 1. Project Structure   | 7/7   | ✅        |
| 2. Dependencies        | 10/10 | ✅        |
| 3. Configuration       | 5/5   | ✅        |
| 4. Security Components | 6/6   | ✅        |
| 5. Bug Fixes v0.7.0    | 7/7   | ✅        |
| 6. Database Migrations | 3/3   | ✅        |
| 7. Documentation       | 6/6   | ✅        |
| 8. Git Repository      | 4/4   | ✅        |
| 9. Build Verification  | 1/1   | ✅        |

### Evidence Location

```
qa-team/AI-132-initialize-backend/test-evidence/
├── project-structure.txt    # Список Java файлов
├── dependencies.txt         # Зависимости из build.gradle
└── last-test-run.txt        # Дата последнего запуска
```

---

## ✅ Чек-лист для QA

- [ ] Клонировать репозиторий
- [ ] Проверить версию Java (17+)
- [ ] Проверить PostgreSQL
- [ ] Запустить `./gradlew compileJava`
- [ ] Запустить автотесты `./qa-team/AI-132-initialize-backend/test-scripts/automated-test.sh`
- [ ] Все 49 тестов ✅
- [ ] Запустить приложение `./gradlew bootRun`
- [ ] Проверить health endpoint
- [ ] Проверить Swagger UI
- [ ] Проверить Rate Limiting (10 req/min)
- [ ] Проверить аутентификацию

---

> **Вывод**: Все изменения v0.7.0 протестированы и работают корректно.  
> **Дата документа**: 2026-01-02  
> **Автор**: AI Assistant
