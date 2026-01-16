# Swagger/OpenAPI: Подробная документация

> **Дата**: 2026-01-02  
> **Версия**: springdoc-openapi 2.3.0  
> **Статус**: ✅ Работает

---

## 📋 Содержание

1. [Обзор](#обзор)
2. [Зависимости](#зависимости)
3. [Конфигурация](#конфигурация)
4. [Доступ к Swagger UI](#доступ-к-swagger-ui)
5. [Аутентификация в Swagger](#аутентификация-в-swagger)
6. [Тестирование API через Swagger](#тестирование-api-через-swagger)

---

## 🔍 Обзор

### Что такое Swagger/OpenAPI?

- **OpenAPI 3.0** — спецификация для описания REST API
- **Swagger UI** — веб-интерфейс для просмотра и тестирования API
- **springdoc-openapi** — библиотека для автогенерации OpenAPI из Spring контроллеров

### Что реализовано

| Функция                 | Статус |
| ----------------------- | ------ |
| Swagger UI              | ✅     |
| OpenAPI 3.0 JSON        | ✅     |
| JWT аутентификация      | ✅     |
| Документация эндпоинтов | ✅     |
| Rate Limiting описание  | ✅     |

---

## 📦 Зависимости

### Файл: `build.gradle`

```groovy
ext {
    set('springdocVersion', '2.3.0')
}

dependencies {
    // =====================
    // Swagger / OpenAPI
    // =====================
    implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}"
}
```

### Детали зависимости

| Параметр        | Значение                            |
| --------------- | ----------------------------------- |
| **Group ID**    | org.springdoc                       |
| **Artifact ID** | springdoc-openapi-starter-webmvc-ui |
| **Version**     | 2.3.0                               |
| **Включает**    | swagger-ui, openapi-core            |

### Транзитивные зависимости

- `io.swagger.core.v3:swagger-annotations`
- `io.swagger.core.v3:swagger-models`
- `org.webjars:swagger-ui`

---

## ⚙️ Конфигурация

### 1. application.yml

```yaml
# =====================
# OpenAPI/Swagger Configuration
# =====================
springdoc:
  api-docs:
    path: /v3/api-docs # JSON OpenAPI спецификация
  swagger-ui:
    path: /swagger-ui.html # Swagger UI интерфейс
    operationsSorter: method # Сортировка по HTTP-методу
    tagsSorter: alpha # Алфавитная сортировка тегов
```

### 2. OpenApiConfig.java

**Путь**: `src/main/java/com/chessai/tournament/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.tournament.example.com")
                    .description("Production Server")
            ))
            // JWT Security
            .addSecurityItem(new SecurityRequirement()
                .addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token")
                ));
    }

    private Info apiInfo() {
        return new Info()
            .title("Tournament Service API")
            .version("1.0.0")
            .description("REST API for Chess Tournament Management...")
            .contact(new Contact()
                .name("Tournament Service Team")
                .email("support@tournament.example.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }
}
```

### 3. SecurityConfig.java — Публичные пути

```java
public static final String[] PUBLIC_ENDPOINTS = {
    // ...
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/swagger-resources/**",
    "/webjars/**"
};
```

---

## 🌐 Доступ к Swagger UI

### URL-адреса

| Ресурс           | URL                                    |
| ---------------- | -------------------------------------- |
| **Swagger UI**   | http://localhost:8080/swagger-ui.html  |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs      |
| **OpenAPI YAML** | http://localhost:8080/v3/api-docs.yaml |

### Проверка доступности

```bash
# Swagger UI
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
# Ожидаемый код: 302 (редирект на /swagger-ui/index.html)

# OpenAPI JSON
curl http://localhost:8080/v3/api-docs | jq .info.title
# Ожидаемый ответ: "Tournament Service API"
```

---

## 🔐 Аутентификация в Swagger

### Как авторизоваться

1. Открыть http://localhost:8080/swagger-ui.html
2. Нажать кнопку **"Authorize"** (🔓) справа вверху
3. В поле **"bearerAuth"** ввести токен: `Bearer <ваш_access_token>`
4. Нажать **"Authorize"** → **"Close"**

### Получение токена

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}'
```

Ответ:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

---

## 🧪 Тестирование API через Swagger

### Доступные эндпоинты

| Метод | Путь                  | Описание             | Auth |
| ----- | --------------------- | -------------------- | ---- |
| POST  | /api/v1/auth/register | Регистрация          | ❌   |
| POST  | /api/v1/auth/login    | Вход                 | ❌   |
| POST  | /api/v1/auth/refresh  | Обновление токена    | ❌   |
| GET   | /api/v1/auth/me       | Текущий пользователь | ✅   |

### Тест через Swagger UI

1. Открыть Swagger UI
2. Раскрыть секцию **"Authentication"**
3. Нажать **"Try it out"** на нужном эндпоинте
4. Заполнить Request Body
5. Нажать **"Execute"**
6. Просмотреть Response

---

## ✅ Чек-лист проверки Swagger

```bash
# 1. Приложение запущено
curl http://localhost:8080/actuator/health

# 2. Swagger UI доступен
curl -s -I http://localhost:8080/swagger-ui.html | grep "HTTP"

# 3. OpenAPI спецификация
curl -s http://localhost:8080/v3/api-docs | jq .openapi
# Ожидаемый ответ: "3.0.1"

# 4. Правильный title
curl -s http://localhost:8080/v3/api-docs | jq .info.title
# Ожидаемый ответ: "Tournament Service API"

# 5. Security scheme настроен
curl -s http://localhost:8080/v3/api-docs | jq '.components.securitySchemes.bearerAuth.type'
# Ожидаемый ответ: "http"
```

---

## 📚 Связанные файлы

| Файл                  | Описание                           |
| --------------------- | ---------------------------------- |
| `build.gradle`        | Зависимость springdoc              |
| `application.yml`     | URL paths для Swagger              |
| `OpenApiConfig.java`  | Java конфигурация OpenAPI          |
| `SecurityConfig.java` | Публичные пути для Swagger         |
| `AuthController.java` | Аннотации @Operation, @ApiResponse |

---

> **Вывод**: Swagger/OpenAPI полностью настроен и работает корректно.
