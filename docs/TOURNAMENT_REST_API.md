# Tournament REST API Documentation

## Обзор

REST API для управления турнирами. Предоставляет CRUD операции и дополнительные эндпоинты для фильтрации и поиска турниров.

**Base URL:** `/api/tournaments`

**Версия:** v1.0

**Дата:** 2026-01-03

---

## Endpoints

### 1. Создать турнир
**POST** `/api/tournaments`

Создает новый турнир. Текущий пользователь становится организатором.

**Headers:**
- `X-User-Id`: ID пользователя (временно, до полной интеграции с Auth)
- `Content-Type`: application/json

**Request Body:**
```json
{
  "name": "Чемпионат мира 2026",
  "description": "Международный турнир",
  "format": "SWISS",
  "startDate": "2026-06-01T10:00:00",
  "endDate": "2026-06-10T18:00:00",
  "maxParticipants": 64,
  "timeControlMinutes": 15,
  "timeIncrementSeconds": 10,
  "minRating": 2000,
  "maxRating": 2800,
  "prizePool": 10000,
  "entryFee": 50,
  "isPublic": true
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "name": "Чемпионат мира 2026",
  "description": "Международный турнир",
  "format": "SWISS",
  "status": "DRAFT",
  "organizerId": 1,
  "startDate": "2026-06-01T10:00:00",
  "endDate": "2026-06-10T18:00:00",
  "maxParticipants": 64,
  "currentParticipants": 0,
  "timeControlMinutes": 15,
  "timeIncrementSeconds": 10,
  "minRating": 2000,
  "maxRating": 2800,
  "prizePool": 10000,
  "entryFee": 50,
  "isPublic": true,
  "createdAt": "2026-01-03T12:00:00",
  "updatedAt": "2026-01-03T12:00:00",
  "hasAvailableSlots": true,
  "hasStarted": false,
  "isEditable": true
}
```

---

### 2. Получить турнир по ID
**GET** `/api/tournaments/{id}`

Возвращает информацию о турнире по его ID.

**Path Parameters:**
- `id` (Long): ID турнира

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Чемпионат мира 2026",
  ...
}
```

**Errors:**
- `404 Not Found`: Турнир не найден

---

### 3. Получить все турниры
**GET** `/api/tournaments`

Возвращает список всех турниров с пагинацией.

**Query Parameters:**
- `page` (int, default=0): Номер страницы
- `size` (int, default=20): Размер страницы
- `sort` (string, default=startDate): Поле для сортировки

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "name": "Чемпионат мира 2026",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5
}
```

---

### 4. Получить публичные турниры
**GET** `/api/tournaments/public`

Возвращает список публичных турниров.

**Query Parameters:**
- `page`, `size`, `sort` (как в п.3)

**Response:** `200 OK` (аналогично п.3)

---

### 5. Получить предстоящие турниры
**GET** `/api/tournaments/upcoming`

Возвращает список предстоящих турниров (дата начала в будущем).

**Query Parameters:**
- `page`, `size`, `sort` (как в п.3)

**Response:** `200 OK` (аналогично п.3)

---

### 6. Получить турниры по статусу
**GET** `/api/tournaments/status/{status}`

Возвращает список турниров с указанным статусом.

**Path Parameters:**
- `status` (TournamentStatus): DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, IN_PROGRESS, COMPLETED, CANCELLED

**Query Parameters:**
- `page`, `size`, `sort` (как в п.3)

**Response:** `200 OK` (аналогично п.3)

---

### 7. Получить турниры организатора
**GET** `/api/tournaments/organizer/{organizerId}`

Возвращает список турниров конкретного организатора.

**Path Parameters:**
- `organizerId` (Long): ID организатора

**Query Parameters:**
- `page`, `size`, `sort` (как в п.3)

**Response:** `200 OK` (аналогично п.3)

---

### 8. Обновить турнир
**PUT** `/api/tournaments/{id}`

Обновляет данные турнира. Можно обновлять только турниры в статусе DRAFT или REGISTRATION_OPEN.

**Path Parameters:**
- `id` (Long): ID турнира

**Headers:**
- `X-User-Id`: ID пользователя

**Request Body:** (аналогично п.1)

**Response:** `200 OK` (аналогично п.1)

**Errors:**
- `404 Not Found`: Турнир не найден
- `409 Conflict`: Турнир нельзя редактировать (уже начался или завершен)

---

### 9. Изменить статус турнира
**PATCH** `/api/tournaments/{id}/status`

Изменяет статус турнира.

**Path Parameters:**
- `id` (Long): ID турнира

**Query Parameters:**
- `status` (TournamentStatus): Новый статус

**Headers:**
- `X-User-Id`: ID пользователя

**Response:** `200 OK` (аналогично п.1)

**Errors:**
- `404 Not Found`: Турнир не найден

---

### 10. Удалить турнир
**DELETE** `/api/tournaments/{id}`

Удаляет турнир. Можно удалять только турниры в статусе DRAFT.

**Path Parameters:**
- `id` (Long): ID турнира

**Headers:**
- `X-User-Id`: ID пользователя

**Response:** `204 No Content`

**Errors:**
- `404 Not Found`: Турнир не найден
- `409 Conflict`: Турнир нельзя удалить

---

## Модели данных

### TournamentRequest
```typescript
{
  name: string (3-100 символов, обязательно)
  description?: string (до 1000 символов)
  format: TournamentFormat (обязательно)
  status?: TournamentStatus (по умолчанию DRAFT)
  startDate: LocalDateTime (обязательно, в будущем)
  endDate: LocalDateTime (обязательно, после startDate)
  maxParticipants: number (2-1000, обязательно)
  timeControlMinutes: number (1-180, обязательно)
  timeIncrementSeconds?: number (0-60)
  minRating?: number (0-3000)
  maxRating?: number (0-3000, > minRating)
  prizePool?: number (>= 0)
  entryFee?: number (>= 0)
  isPublic?: boolean (по умолчанию true)
}
```

### TournamentResponse
```typescript
{
  id: number
  name: string
  description?: string
  format: TournamentFormat
  status: TournamentStatus
  organizerId: number
  startDate: LocalDateTime
  endDate: LocalDateTime
  maxParticipants: number
  currentParticipants: number
  timeControlMinutes: number
  timeIncrementSeconds?: number
  minRating?: number
  maxRating?: number
  prizePool?: number
  entryFee?: number
  isPublic: boolean
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
  hasAvailableSlots: boolean
  hasStarted: boolean
  isEditable: boolean
}
```

### TournamentFormat (Enum)
- `ROUND_ROBIN` - Круговая система
- `SWISS` - Швейцарская система
- `KNOCKOUT` - Олимпийская система (на выбывание)
- `DOUBLE_ROUND_ROBIN` - Двухкруговая система

### TournamentStatus (Enum)
- `DRAFT` - Черновик
- `REGISTRATION_OPEN` - Регистрация открыта
- `REGISTRATION_CLOSED` - Регистрация закрыта
- `IN_PROGRESS` - В процессе
- `COMPLETED` - Завершен
- `CANCELLED` - Отменен

---

## Коды ошибок

| Код | Описание |
|-----|----------|
| 200 | OK - Успешный запрос |
| 201 | Created - Ресурс создан |
| 204 | No Content - Успешное удаление |
| 400 | Bad Request - Невалидные данные |
| 401 | Unauthorized - Не авторизован |
| 404 | Not Found - Ресурс не найден |
| 409 | Conflict - Конфликт (нельзя редактировать/удалить) |
| 500 | Internal Server Error - Внутренняя ошибка сервера |

---

## Swagger UI

API документация доступна по адресу:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI спецификация:
```
http://localhost:8080/v3/api-docs
```

---

## Примеры использования

### cURL

```bash
# Создать турнир
curl -X POST http://localhost:8080/api/tournaments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "name": "Test Tournament",
    "format": "SWISS",
    "startDate": "2026-06-01T10:00:00",
    "endDate": "2026-06-10T18:00:00",
    "maxParticipants": 32,
    "timeControlMinutes": 15,
    "timeIncrementSeconds": 10
  }'

# Получить все турниры
curl http://localhost:8080/api/tournaments?page=0&size=10

# Получить турнир по ID
curl http://localhost:8080/api/tournaments/1

# Обновить статус
curl -X PATCH "http://localhost:8080/api/tournaments/1/status?status=REGISTRATION_OPEN" \
  -H "X-User-Id: 1"

# Удалить турнир
curl -X DELETE http://localhost:8080/api/tournaments/1 \
  -H "X-User-Id: 1"
```

---

## Тестирование

### Unit тесты
```bash
./gradlew test --tests "TournamentServiceTest"
```

### Integration тесты
```bash
./gradlew test --tests "TournamentControllerIntegrationTest"
```

### Все тесты
```bash
./gradlew test
```

---

## Следующие шаги

- [ ] Интеграция с полноценной аутентификацией (заменить X-User-Id на JWT)
- [ ] Добавить авторизацию (только организатор может редактировать свой турнир)
- [ ] Добавить поиск турниров по названию
- [ ] Добавить фильтрацию по рейтингу
- [ ] Добавить эндпоинт для регистрации участников
- [ ] Добавить эндпоинт для управления матчами

---

**Jira:** AIChess...AI-391  
**Дата реализации:** 2026-01-03  
**Разработчик:** Nikolay1 QA





