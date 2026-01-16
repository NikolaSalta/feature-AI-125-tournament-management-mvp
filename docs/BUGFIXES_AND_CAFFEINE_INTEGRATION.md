# Исправление багов и интеграция Caffeine Cache

> **Дата**: 2026-01-02  
> **Версия**: v0.7.0  
> **Автор**: AI Assistant

---

## 📋 Содержание

1. [Обзор изменений](#обзор-изменений)
2. [Caffeine Cache — подробная интеграция](#caffeine-cache--подробная-интеграция)
3. [Исправленные баги](#исправленные-баги)
4. [Изменённые файлы](#изменённые-файлы)
5. [Тестирование изменений](#тестирование-изменений)

---

## 🔄 Обзор изменений

### Основные изменения в версии 0.7.0

| Категория        | Описание                               |
| ---------------- | -------------------------------------- |
| **Конфигурация** | Access Token увеличен с 15 до 30 минут |
| **Memory Leak**  | Исправлен с помощью Caffeine TTL-кэша  |
| **Security**     | IP Spoofing, JWT issuer validation     |
| **Bug Fixes**    | 7 багов исправлено                     |

---

## ☕ Caffeine Cache — подробная интеграция

### Что такое Caffeine?

**Caffeine** — высокопроизводительная Java-библиотека кэширования, созданная как "почти оптимальный" кэш. Это преемник Guava Cache с улучшенной производительностью.

### Зачем понадобился Caffeine?

#### Проблема (Memory Leak)

Исходный код использовал `ConcurrentHashMap`:

```java
// БЫЛО: RateLimitingService.java (ПРОБЛЕМА!)
private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

public boolean tryConsume(String key, RateLimitType type) {
    String bucketKey = type.name() + ":" + key;
    Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k -> createBucket(type));
    return bucket.tryConsume(1);
}
```

**Почему это плохо:**

- Каждый уникальный IP добавляет запись в Map
- Записи **НИКОГДА не удаляются**
- При DDoS-атаке с разных IP: миллионы записей → Out of Memory
- При длительной работе: постепенное накопление записей

#### Решение (Caffeine TTL Cache)

```java
// СТАЛО: RateLimitingService.java (РЕШЕНИЕ!)
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))  // Удаление через 10 мин неактивности
        .maximumSize(10_000)                        // Максимум 10,000 записей
        .recordStats()                              // Статистика для мониторинга
        .build();

public boolean tryConsume(String key, RateLimitType type) {
    String bucketKey = type.name() + ":" + key;
    Bucket bucket = bucketCache.get(bucketKey, k -> createBucket(type));
    return bucket.tryConsume(1);
}
```

---

### Добавление зависимости

#### Файл: `build.gradle`

```groovy
// =====================
// Caffeine Cache (TTL for rate limiting)
// =====================
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
```

| Параметр        | Значение                             |
| --------------- | ------------------------------------ |
| **Group ID**    | `com.github.ben-manes.caffeine`      |
| **Artifact ID** | `caffeine`                           |
| **Version**     | `3.1.8` (стабильная, декабрь 2024)   |
| **Scope**       | `implementation` (compile + runtime) |
| **Размер JAR**  | ~600 KB                              |
| **Зависимости** | Нет внешних зависимостей             |

---

### Конфигурация Caffeine в проекте

#### Параметры кэша

```java
Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(10))  // TTL
    .maximumSize(10_000)                        // Max entries
    .recordStats()                              // Statistics
    .build();
```

| Параметр            | Значение | Назначение                                             |
| ------------------- | -------- | ------------------------------------------------------ |
| `expireAfterAccess` | 10 минут | Запись удаляется через 10 мин после последнего доступа |
| `maximumSize`       | 10,000   | Максимум записей, LRU-eviction при превышении          |
| `recordStats`       | true     | Включает сбор статистики (hit ratio, evictions)        |

#### Почему такие параметры?

**`expireAfterAccess(10 min)`**:

- Rate limiting работает с окном в 1 минуту
- 10 минут — достаточно для "запоминания" активных клиентов
- Неактивные IP автоматически удаляются

**`maximumSize(10_000)`**:

- Защита от OOM при массированных атаках
- 10K записей ≈ 5-10 MB памяти
- LRU-алгоритм вытесняет старые записи

**`recordStats()`**:

- Позволяет мониторить эффективность кэша
- Доступно через `bucketCache.stats()`

---

### API Caffeine в проекте

#### Основные методы

| Метод Caffeine            | Метод ConcurrentHashMap | Описание              |
| ------------------------- | ----------------------- | --------------------- |
| `cache.get(key, loader)`  | `map.computeIfAbsent()` | Получить или создать  |
| `cache.getIfPresent(key)` | `map.get(key)`          | Получить без создания |
| `cache.invalidate(key)`   | `map.remove(key)`       | Удалить запись        |
| `cache.invalidateAll()`   | `map.clear()`           | Очистить всё          |
| `cache.estimatedSize()`   | `map.size()`            | Примерный размер      |
| `cache.stats()`           | ❌ нет                  | Статистика кэша       |

#### Использование в RateLimitingService

```java
// Получить или создать Bucket
public boolean tryConsume(String key, RateLimitType type) {
    String bucketKey = type.name() + ":" + key;
    Bucket bucket = bucketCache.get(bucketKey, k -> createBucket(type));
    return bucket.tryConsume(1);
}

// Получить существующий (без создания)
public boolean isAllowed(String key, RateLimitType type) {
    String bucketKey = type.name() + ":" + key;
    Bucket bucket = bucketCache.getIfPresent(bucketKey);
    if (bucket == null) {
        return true;
    }
    return bucket.getAvailableTokens() > 0;
}

// Сбросить лимит
public void resetLimit(String key, RateLimitType type) {
    String bucketKey = type.name() + ":" + key;
    bucketCache.invalidate(bucketKey);
}

// Получить статистику
public String getCacheStats() {
    return bucketCache.stats().toString();
}
```

---

### Сравнение: до и после

| Аспект             | ConcurrentHashMap        | Caffeine Cache            |
| ------------------ | ------------------------ | ------------------------- |
| **Memory Leak**    | ❌ Да (бесконечный рост) | ✅ Нет (TTL + maxSize)    |
| **Auto-eviction**  | ❌ Нет                   | ✅ LRU + TTL              |
| **Max size limit** | ❌ Нет                   | ✅ 10,000 записей         |
| **Statistics**     | ❌ Нет                   | ✅ hit/miss ratio         |
| **Performance**    | ✅ Быстрый               | ✅ Быстрый (near-optimal) |
| **Thread-safe**    | ✅ Да                    | ✅ Да                     |
| **Зависимости**    | Встроенный JDK           | +1 JAR (~600KB)           |

---

## 🐛 Исправленные баги

### 1. Memory Leak в RateLimitingService

**Файл**: `RateLimitingService.java`

| До                                  | После                              |
| ----------------------------------- | ---------------------------------- |
| `ConcurrentHashMap<String, Bucket>` | `Cache<String, Bucket>` (Caffeine) |
| Бесконечный рост                    | TTL 10 мин + max 10K               |
| Риск OOM                            | Защита от OOM                      |

---

### 2. IP Spoofing уязвимость

**Файл**: `RateLimitingFilter.java`

```java
// БЫЛО (уязвимо):
return xForwardedFor.split(",")[0].trim();  // Первый IP — подделывается!

// СТАЛО (безопасно):
String[] ips = xForwardedFor.split(",");
return ips[ips.length - 1].trim();  // Последний IP — от доверенного прокси
```

**Почему это важно:**

- `X-Forwarded-For: fake-ip, real-proxy-ip`
- Атакующий может отправить: `X-Forwarded-For: 1.1.1.1`
- Первый IP подделан, последний — добавлен прокси

---

### 3. Дублирование Role.USER

**Файл**: `AuthService.java`

```java
// БЫЛО:
User user = new User(username, email, password);  // Конструктор добавляет USER
user.addRole(Role.USER);  // Дубликат!

// СТАЛО:
User user = new User(username, email, password);
// Role.USER уже добавлена в конструкторе User
```

---

### 4. @Size валидация на хешированном пароле

**Файл**: `User.java`

```java
// БЫЛО:
@Size(min = 8, message = "Password must be at least 8 characters")
private String password;  // Но здесь хранится ХЕШ (60+ символов)!

// СТАЛО:
@NotBlank(message = "Password is required")
// @Size валидация здесь не нужна - хранится хеш
// Валидация длины выполняется в RegisterRequest DTO
private String password;
```

---

### 5. Неполная проверка в refreshToken

**Файл**: `AuthService.java`

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

---

### 6. ClassCastException в GlobalExceptionHandler

**Файл**: `GlobalExceptionHandler.java`

```java
// БЫЛО (потенциальный ClassCastException):
ex.getBindingResult().getAllErrors().forEach(error -> {
    String fieldName = ((FieldError) error).getField();  // ObjectError != FieldError!
});

// СТАЛО (безопасно):
ex.getBindingResult().getAllErrors().forEach(error -> {
    if (error instanceof FieldError fieldError) {
        String fieldName = fieldError.getField();
        errors.put(fieldName, error.getDefaultMessage());
    } else {
        errors.put(error.getObjectName(), error.getDefaultMessage());
    }
});
```

---

### 7. JWT issuer не проверялся

**Файл**: `JwtTokenProvider.java`

```java
// БЫЛО:
return Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token)
    .getPayload();

// СТАЛО:
return Jwts.parser()
    .verifyWith(secretKey)
    .requireIssuer(jwtProperties.getIssuer())  // Проверка издателя!
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

---

### 8. Access Token время жизни

**Файлы**: `application.yml`, `JwtProperties.java`

```yaml
# БЫЛО:
access-token-expiration: 900000   # 15 минут

# СТАЛО:
access-token-expiration: 1800000  # 30 минут
```

---

## 📁 Изменённые файлы

### Список всех изменённых файлов

| Файл                          | Изменение                     |
| ----------------------------- | ----------------------------- |
| `build.gradle`                | +Caffeine dependency          |
| `application.yml`             | Token: 15→30 мин              |
| `JwtProperties.java`          | Default: 15→30 мин            |
| `RateLimitingService.java`    | Полная переработка (Caffeine) |
| `RateLimitingFilter.java`     | IP: first→last                |
| `AuthService.java`            | -Duplicate role, +Full checks |
| `User.java`                   | -@Size on password            |
| `GlobalExceptionHandler.java` | +instanceof check             |
| `JwtTokenProvider.java`       | +requireIssuer                |
| `README.md`                   | Документация                  |
| `CHANGELOG.md`                | v0.7.0 release notes          |

---

## 🧪 Тестирование изменений

### Проверка сборки

```bash
./gradlew clean compileJava
```

**Результат**: `BUILD SUCCESSFUL`

### Проверка зависимостей

```bash
./gradlew dependencies --configuration runtimeClasspath | grep caffeine
```

**Ожидаемый вывод**:

```
\--- com.github.ben-manes.caffeine:caffeine:3.1.8
```

### Ручное тестирование Rate Limiting

```bash
# 1. Запустить приложение
./gradlew bootRun

# 2. Отправить 11 запросов на auth (лимит 10/мин)
for i in {1..11}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test","password":"test"}'
  echo ""
done

# 11-й запрос должен вернуть 429 Too Many Requests
```

### Проверка статистики кэша

Можно добавить endpoint для мониторинга:

```java
@GetMapping("/admin/cache-stats")
public String getCacheStats() {
    return rateLimitingService.getCacheStats();
}
```

---

## 📚 Дополнительные ресурсы

- [Caffeine GitHub](https://github.com/ben-manes/caffeine)
- [Caffeine Wiki](https://github.com/ben-manes/caffeine/wiki)
- [Caffeine vs Guava Cache](https://github.com/ben-manes/caffeine/wiki/Benchmarks)
- [Bucket4j Documentation](https://bucket4j.com/)

---

> **Автор**: AI Assistant  
> **Дата создания**: 2026-01-02  
> **Последнее обновление**: 2026-01-02
