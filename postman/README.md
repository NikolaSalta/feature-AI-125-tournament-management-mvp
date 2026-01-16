# Postman Collection для Tournament Service API

## Описание

Коллекция содержит все endpoints для тестирования Tournament Service API.

## Содержимое

- **Auth** - Аутентификация и авторизация
  - Register User
  - Login
  - Get Current User
  
- **Tournaments** - Управление турнирами
  - Create Tournament
  - Get All Tournaments
  - Get Tournament by ID
  - Get Public Tournaments
  - Get Upcoming Tournaments
  - Get Tournaments by Status
  - Get Tournaments by Organizer
  - Update Tournament
  - Update Tournament Status
  - Delete Tournament

- **Health Check** - Проверка состояния сервиса
- **Swagger UI** - Документация API
- **OpenAPI Docs** - OpenAPI спецификация

## Установка

### 1. Импорт коллекции

1. Откройте Postman
2. Нажмите **Import**
3. Выберите файл `Tournament_API.postman_collection.json`
4. Нажмите **Import**

### 2. Импорт environment

1. Нажмите на иконку **Environments** (слева)
2. Нажмите **Import**
3. Выберите файл `Tournament_Local.postman_environment.json`
4. Нажмите **Import**
5. Выберите **Tournament Service - Local** в выпадающем списке environments

## Использование

### Базовый workflow

1. **Запустите сервис:**
   ```bash
   ./gradlew bootRun
   ```
   или
   ```bash
   docker-compose up
   ```

2. **Health Check:**
   - Запустите `Health Check` request
   - Убедитесь, что сервис отвечает `{"status":"UP"}`

3. **Регистрация пользователя:**
   - Запустите `Auth > Register User`
   - Скопируйте `accessToken` из ответа
   - Вставьте в переменную `access_token` в environment

4. **Создание турнира:**
   - Запустите `Tournaments > Create Tournament`
   - Обратите внимание на `id` созданного турнира

5. **Получение турниров:**
   - Запустите `Tournaments > Get All Tournaments`
   - Попробуйте другие фильтры (Public, Upcoming, By Status)

6. **Обновление турнира:**
   - Измените ID в URL на ID вашего турнира
   - Запустите `Tournaments > Update Tournament`

7. **Swagger UI:**
   - Запустите `Swagger UI` request
   - Откройте URL в браузере для интерактивной документации

## Переменные

### Environment Variables

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `base_url` | Базовый URL API | `http://localhost:8080` |
| `access_token` | JWT access token | (пусто) |
| `refresh_token` | JWT refresh token | (пусто) |
| `user_id` | ID текущего пользователя | `1` |

### Как обновить токен

После успешного логина:
1. Скопируйте `accessToken` из ответа
2. Откройте **Environments** (слева)
3. Выберите **Tournament Service - Local**
4. Вставьте токен в поле `access_token`
5. Нажмите **Save**

## Примеры запросов

### Создать турнир

```http
POST http://localhost:8080/api/tournaments
Content-Type: application/json
X-User-Id: 1

{
  "name": "Чемпионат мира 2026",
  "format": "SWISS",
  "startDate": "2026-06-01T10:00:00",
  "endDate": "2026-06-10T18:00:00",
  "maxParticipants": 64,
  "timeControlMinutes": 15,
  "timeIncrementSeconds": 10
}
```

### Получить все турниры

```http
GET http://localhost:8080/api/tournaments?page=0&size=20
```

### Обновить статус турнира

```http
PATCH http://localhost:8080/api/tournaments/1/status?status=REGISTRATION_OPEN
X-User-Id: 1
```

## Статусы турниров

- `DRAFT` - Черновик
- `REGISTRATION_OPEN` - Регистрация открыта
- `REGISTRATION_CLOSED` - Регистрация закрыта
- `IN_PROGRESS` - В процессе
- `COMPLETED` - Завершен
- `CANCELLED` - Отменен

## Форматы турниров

- `ROUND_ROBIN` - Круговая система
- `SWISS` - Швейцарская система
- `KNOCKOUT` - Олимпийская система
- `DOUBLE_ROUND_ROBIN` - Двухкруговая система

## Troubleshooting

### Ошибка 401 Unauthorized

- Убедитесь, что вы залогинились
- Проверьте, что `access_token` установлен в environment
- Токен может истечь (по умолчанию 30 минут)

### Ошибка 404 Not Found

- Проверьте, что сервис запущен
- Убедитесь, что используете правильный `base_url`
- Проверьте ID ресурса в URL

### Ошибка 409 Conflict

- Турнир нельзя редактировать (уже начался или завершен)
- Попробуйте создать новый турнир в статусе DRAFT

## Дополнительные ресурсы

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI Docs](http://localhost:8080/v3/api-docs)
- [API Documentation](../docs/TOURNAMENT_REST_API.md)
- [Health Check](http://localhost:8080/actuator/health)

## Версия

- **Коллекция:** v1.1.0
- **API:** v1.0.0-MVP
- **Дата:** 2026-01-03





