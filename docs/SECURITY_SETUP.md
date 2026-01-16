# Security Setup - Полное описание системы безопасности

**Дата:** 2025-01-02  
**Версия:** 0.3.0  
**Задача:** Интеграция полноценной системы безопасности с JWT-аутентификацией

---

## 📋 Содержание

1. [Обзор изменений](#обзор-изменений)
2. [Добавленные зависимости](#добавленные-зависимости)
3. [Структура проекта](#структура-проекта)
4. [JWT Аутентификация](#jwt-аутентификация)
5. [Spring Security Configuration](#spring-security-configuration)
6. [Rate Limiting](#rate-limiting)
7. [API Endpoints](#api-endpoints)
8. [Конфигурация](#конфигурация)
9. [Миграции базы данных](#миграции-базы-данных)
10. [Использование](#использование)
11. [Безопасность Production](#безопасность-production)

---

## Обзор изменений

Была реализована полноценная система безопасности с использованием:

| Компонент | Технология | Назначение |
|-----------|------------|------------|
| Аутентификация | JWT (JSON Web Tokens) | Stateless аутентификация |
| Авторизация | Spring Security | Ролевая модель доступа |
| Шифрование паролей | BCrypt (strength 12) | Безопасное хранение паролей |
| Rate Limiting | Bucket4j | Защита от DDoS и брутфорса |
| CORS | Spring Security | Контроль Cross-Origin запросов |

### Что было добавлено:

1. ✅ JWT-токены (access + refresh)
2. ✅ Ролевая модель (USER, ORGANIZER, ADMIN)
3. ✅ Регистрация и аутентификация пользователей
4. ✅ Rate Limiting для защиты API
5. ✅ Профили конфигурации (dev/prod)
6. ✅ Глобальная обработка ошибок
7. ✅ Миграция БД для таблицы пользователей

---

## Добавленные зависимости

### Файл: `pom.xml`

#### 1. JWT (JSON Web Token) - jjwt

```xml
<!-- JWT (JSON Web Token) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**Описание:**
- `jjwt-api` - API для работы с JWT (интерфейсы и классы)
- `jjwt-impl` - Реализация JWT (runtime)
- `jjwt-jackson` - Сериализация/десериализация JSON через Jackson

**Зачем нужно:**
- Генерация access и refresh токенов
- Подпись токенов алгоритмом HS256
- Валидация и парсинг токенов
- Извлечение claims (username, roles, expiration)

#### 2. Rate Limiting - Bucket4j

```xml
<!-- Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

**Описание:**
- Реализация алгоритма Token Bucket для ограничения частоты запросов

**Зачем нужно:**
- Защита от DDoS-атак
- Защита от брутфорса паролей
- Контроль нагрузки на API

#### 3. Lombok (Optional)

```xml
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Описание:**
- Генерация boilerplate-кода (getters, setters, constructors)
- Опционально - можно не использовать

---

## Структура проекта

```
src/main/java/com/chessai/tournament/
├── config/
│   └── SecurityConfig.java              # Конфигурация Spring Security
├── controller/
│   └── AuthController.java              # REST контроллер аутентификации
├── dto/
│   └── auth/
│       ├── AuthResponse.java            # Ответ с токенами
│       ├── LoginRequest.java            # Запрос на вход
│       ├── RefreshTokenRequest.java     # Запрос на обновление токена
│       └── RegisterRequest.java         # Запрос на регистрацию
├── entity/
│   ├── Role.java                        # Enum ролей пользователей
│   └── User.java                        # JPA сущность пользователя
├── exception/
│   ├── AuthException.java               # Исключение аутентификации
│   └── GlobalExceptionHandler.java      # Глобальный обработчик ошибок
├── repository/
│   └── UserRepository.java              # JPA репозиторий пользователей
└── security/
    ├── jwt/
    │   ├── JwtAuthenticationEntryPoint.java  # Обработчик ошибок JWT
    │   ├── JwtAuthenticationFilter.java      # Фильтр JWT
    │   ├── JwtProperties.java                # Конфигурация JWT
    │   └── JwtTokenProvider.java             # Генерация/валидация токенов
    ├── ratelimit/
    │   ├── RateLimitingFilter.java           # Фильтр Rate Limiting
    │   └── RateLimitingService.java          # Сервис Rate Limiting
    └── service/
        ├── AuthService.java                  # Сервис аутентификации
        └── CustomUserDetailsService.java     # Загрузка пользователей
```

---

## JWT Аутентификация

### Как работает JWT

```
┌─────────────┐                          ┌─────────────┐
│   Client    │                          │   Server    │
└─────────────┘                          └─────────────┘
       │                                        │
       │  1. POST /api/auth/login               │
       │  {username, password}                  │
       │ ────────────────────────────────────>  │
       │                                        │
       │  2. Validate credentials               │
       │  3. Generate JWT tokens                │
       │                                        │
       │  4. {accessToken, refreshToken}        │
       │ <────────────────────────────────────  │
       │                                        │
       │  5. GET /api/resource                  │
       │  Header: Authorization: Bearer <token> │
       │ ────────────────────────────────────>  │
       │                                        │
       │  6. Validate token                     │
       │  7. Process request                    │
       │                                        │
       │  8. Response                           │
       │ <────────────────────────────────────  │
```

### Типы токенов

#### Access Token
- **Время жизни:** 15 минут
- **Назначение:** Доступ к защищенным ресурсам
- **Содержит:** username, roles, issuer, expiration

#### Refresh Token
- **Время жизни:** 7 дней
- **Назначение:** Получение нового access token
- **Содержит:** username, issuer, expiration

### JwtTokenProvider.java

```java
// Генерация access-токена
public String generateAccessToken(UserDetails userDetails) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

    return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuer(jwtProperties.getIssuer())
            .claim("roles", roles)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
}

// Валидация токена
public boolean validateToken(String token) {
    try {
        parseToken(token);
        return true;
    } catch (SignatureException | MalformedJwtException | ExpiredJwtException e) {
        return false;
    }
}
```

### JwtAuthenticationFilter.java

Фильтр извлекает JWT из заголовка `Authorization` и устанавливает Authentication:

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                FilterChain filterChain) {
    String jwt = extractJwtFromRequest(request);  // Bearer <token>

    if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
        String username = jwtTokenProvider.getUsernameFromToken(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```

---

## Spring Security Configuration

### SecurityConfig.java

#### Production Configuration (по умолчанию)

```java
@Configuration
@Profile("!dev")  // Активируется когда профиль НЕ dev
public static class ProductionSecurityConfig {

    @Bean
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())  // Отключен для stateless JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

#### Development Configuration

```java
@Configuration
@Profile("dev")
public static class DevelopmentSecurityConfig {

    @Bean
    public SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll());  // Все открыто для разработки

        return http.build();
    }
}
```

### Публичные endpoints

```java
private static final String[] PUBLIC_ENDPOINTS = {
    "/api/auth/**",          // Аутентификация
    "/swagger-ui/**",        // Swagger UI
    "/swagger-ui.html",
    "/v3/api-docs/**",       // OpenAPI
    "/actuator/health",      // Health check
    "/actuator/info",
    "/error"
};
```

### Admin endpoints

```java
private static final String[] ADMIN_ENDPOINTS = {
    "/api/admin/**",         // Админ API
    "/actuator/**"           // Все actuator endpoints
};
```

### CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(
        "http://localhost:3000",    // React
        "http://localhost:5173",    // Vite
        "http://localhost:8080"     // Backend
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "X-RateLimit-Limit", "X-RateLimit-Remaining"));
    configuration.setAllowCredentials(true);
    
    return source;
}
```

### Password Encoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // strength = 12 (рекомендуется)
}
```

---

## Rate Limiting

### Типы ограничений

| Тип | Лимит | Период | Применяется к |
|-----|-------|--------|---------------|
| GENERAL | 100 запросов | 1 минута | Общие запросы |
| AUTH | 10 запросов | 1 минута | /api/auth/** |
| API | 60 запросов | 1 минута | /api/** |
| HEAVY | 10 запросов | 1 минута | Тяжелые операции |

### RateLimitingService.java

```java
public enum RateLimitType {
    GENERAL(100, Duration.ofMinutes(1)),    // 100 req/min
    AUTH(10, Duration.ofMinutes(1)),        // 10 req/min (брутфорс защита)
    API(60, Duration.ofMinutes(1)),         // 60 req/min
    HEAVY(10, Duration.ofMinutes(1));       // 10 req/min (тяжелые операции)
}

public boolean tryConsume(String key, RateLimitType type) {
    Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k -> createBucket(type));
    return bucket.tryConsume(1);
}
```

### RateLimitingFilter.java

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) {
    String clientIp = getClientIp(request);
    RateLimitType limitType = determineLimitType(request.getRequestURI());

    if (!rateLimitingService.tryConsume(clientIp, limitType)) {
        sendRateLimitExceededResponse(response);  // HTTP 429
        return;
    }

    // Добавляем заголовки с информацией о лимитах
    response.setHeader("X-RateLimit-Limit", String.valueOf(limitType.getCapacity()));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

    filterChain.doFilter(request, response);
}
```

### Заголовки ответа

```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 55
```

---

## API Endpoints

### API Versioning

API использует версионирование через URL:

```
/api/v1/resource
/api/v2/resource (будущая версия)
```

**Константы версий** определены в `ApiVersion.java`:

```java
public final class ApiVersion {
    public static final String V1 = "/api/v1";
    public static final String V1_AUTH = V1 + "/auth";
    public static final String V1_TOURNAMENTS = V1 + "/tournaments";
    public static final String V1_ADMIN = V1 + "/admin";
}
```

### Authentication API (v1)

| Method | Endpoint | Описание | Auth Required |
|--------|----------|----------|---------------|
| POST | `/api/v1/auth/register` | Регистрация | ❌ |
| POST | `/api/v1/auth/login` | Вход | ❌ |
| POST | `/api/v1/auth/refresh` | Обновление токена | ❌ |
| GET | `/api/v1/auth/me` | Информация о пользователе | ✅ |
| GET | `/api/v1/auth/validate` | Проверка токена | ✅ |

### Примеры запросов

#### Регистрация

```http
POST /api/v1/auth/register
Content-Type: application/json

{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
}
```

**Ответ:**

```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
        "id": 1,
        "username": "john_doe",
        "email": "john@example.com",
        "fullName": "John Doe",
        "roles": ["USER"]
    }
}
```

#### Вход

```http
POST /api/v1/auth/login
Content-Type: application/json

{
    "usernameOrEmail": "john_doe",
    "password": "SecurePass123!"
}
```

#### Обновление токена

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Доступ к защищенному ресурсу

```http
GET /api/v1/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Конфигурация

### application.yml

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:base64_encoded_secret_key}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:900000}    # 15 минут
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 дней
  issuer: tournament-service

# Profile
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

### Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `JWT_SECRET` | Секретный ключ (Base64) | dev-key |
| `JWT_ACCESS_EXPIRATION` | Время жизни access-токена (мс) | 900000 (15 мин) |
| `JWT_REFRESH_EXPIRATION` | Время жизни refresh-токена (мс) | 604800000 (7 дней) |
| `SPRING_PROFILES_ACTIVE` | Активный профиль | dev |

### Профили

#### dev (разработка)
- Все endpoints открыты
- Подробное логирование
- Все actuator endpoints доступны

#### prod (production)
- JWT-аутентификация обязательна
- Минимальное логирование
- Только health/info actuator

---

## Миграции базы данных

### V2__create_users_table.sql

```sql
-- Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);

-- User Roles Table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Default Admin User (password: Admin123!)
INSERT INTO users (username, email, password, first_name, last_name, enabled)
VALUES ('admin', 'admin@tournament.local', 
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.CQPxFxeGMf4c5S',
        'System', 'Administrator', true);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin';
```

### Дефолтный администратор

| Поле | Значение |
|------|----------|
| Username | `admin` |
| Email | `admin@tournament.local` |
| Password | `Admin123!` |
| Roles | `ADMIN`, `USER` |

---

## Использование

### 1. Запуск в dev-режиме

```bash
./mvnw spring-boot:run
# или
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Запуск в prod-режиме

```bash
# Установить переменные окружения
export JWT_SECRET=$(openssl rand -base64 32)
export DB_PASSWORD=your_secure_password
export SPRING_PROFILES_ACTIVE=prod

./mvnw spring-boot:run
```

### 3. Тестирование API

```bash
# Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"TestPass123!"}'

# Логин
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"test","password":"TestPass123!"}'

# Доступ к защищенному ресурсу
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <access_token>"
```

---

## Безопасность Production

### ⚠️ Обязательно для Production

1. **Сгенерируйте новый JWT_SECRET:**
   ```bash
   openssl rand -base64 32
   ```

2. **Никогда не используйте дефолтные значения:**
   - JWT_SECRET
   - DB_PASSWORD
   - Пароль admin

3. **Включите HTTPS:**
   - Используйте reverse proxy (nginx)
   - Или настройте SSL в Spring Boot

4. **Измените пароль admin:**
   ```sql
   UPDATE users SET password = '$2a$12$...' WHERE username = 'admin';
   ```

5. **Настройте CORS для production:**
   - Укажите только реальные домены

### Checklist Production

- [ ] JWT_SECRET изменен
- [ ] DB_PASSWORD установлен
- [ ] Пароль admin изменен
- [ ] HTTPS включен
- [ ] CORS настроен
- [ ] Логирование настроено
- [ ] Мониторинг настроен

---

## Troubleshooting

### Ошибка: "JWT signature does not match"

**Причина:** Токен подписан другим секретом

**Решение:**
1. Проверьте JWT_SECRET
2. Выйдите и войдите заново

### Ошибка: "Token expired"

**Причина:** Access-токен истек

**Решение:**
1. Используйте refresh-токен для получения нового access-токена
2. Или войдите заново

### Ошибка: "Rate limit exceeded"

**Причина:** Превышен лимит запросов

**Решение:**
1. Подождите 1 минуту
2. Проверьте заголовки X-RateLimit-*

---

## Полезные ссылки

- [JWT.io - Отладка токенов](https://jwt.io/)
- [BCrypt Calculator](https://bcrypt-generator.com/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Bucket4j Documentation](https://github.com/bucket4j/bucket4j)

---

**Документ создан:** 2025-01-02  
**Версия документа:** 1.0  
**Автор:** AI Assistant

