# Tournament Domain Model

**Дата:** 2026-01-02  
**Версия:** 1.0.0-MVP  
**Jira:** AIChess...AI-390

---

## 📋 Обзор

Реализована доменная модель турнира для управления шахматными турнирами.

---

## 🎯 Acceptance Criteria

| Критерий | Статус |
|----------|--------|
| Tournament entity created | ✅ |
| Tournament repository implemented | ✅ |
| Entity is persisted and retrieved from database | ✅ |

---

## 📦 Компоненты

### 1. Entity Classes

#### Tournament
**Файл:** `src/main/java/com/chessai/tournament/entity/Tournament.java`

Основная сущность турнира с полями:

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Первичный ключ |
| name | String(100) | Название турнира |
| description | String(2000) | Описание |
| format | TournamentFormat | Формат (ROUND_ROBIN, SWISS, KNOCKOUT, DOUBLE_ROUND_ROBIN) |
| status | TournamentStatus | Статус турнира |
| organizerId | Long | ID организатора (FK → users) |
| startDate | LocalDateTime | Дата начала |
| endDate | LocalDateTime | Дата окончания |
| maxParticipants | Integer | Макс. участников |
| currentParticipants | Integer | Текущее кол-во участников |
| timeControlMinutes | Integer | Контроль времени (минуты) |
| timeIncrementSeconds | Integer | Добавка времени (секунды) |
| minRating | Integer | Мин. рейтинг для участия |
| maxRating | Integer | Макс. рейтинг для участия |
| prizePool | Integer | Призовой фонд |
| entryFee | Integer | Взнос за участие |
| isPublic | Boolean | Публичный ли турнир |
| createdAt | LocalDateTime | Дата создания |
| updatedAt | LocalDateTime | Дата обновления |

**Бизнес-методы:**
- `canRegisterParticipant()` — проверка возможности регистрации
- `isRatingEligible(int rating)` — проверка рейтинга игрока
- `isOrganizer(Long userId)` — проверка организатора
- `isEditable()` — можно ли редактировать
- `hasStarted()` — начался ли турнир

#### TournamentStatus
**Файл:** `src/main/java/com/chessai/tournament/entity/TournamentStatus.java`

```java
public enum TournamentStatus {
    DRAFT,                  // Черновик
    REGISTRATION_OPEN,      // Регистрация открыта
    REGISTRATION_CLOSED,    // Регистрация закрыта
    IN_PROGRESS,            // В процессе
    COMPLETED,              // Завершён
    CANCELLED               // Отменён
}
```

#### TournamentFormat
**Файл:** `src/main/java/com/chessai/tournament/entity/TournamentFormat.java`

```java
public enum TournamentFormat {
    ROUND_ROBIN,            // Круговой
    SWISS,                  // Швейцарская система
    KNOCKOUT,               // Олимпийская система
    DOUBLE_ROUND_ROBIN      // Двухкруговой
}
```

---

### 2. Repository

**Файл:** `src/main/java/com/chessai/tournament/repository/TournamentRepository.java`

Методы запросов:

| Метод | Описание |
|-------|----------|
| `findByStatus(status)` | Поиск по статусу |
| `findByOrganizerId(id)` | Турниры организатора |
| `findByIsPublicTrue()` | Публичные турниры |
| `findByStatusAndIsPublicTrue(status)` | Публичные по статусу |
| `findByStartDateBetween(start, end)` | По диапазону дат |
| `findActiveTournaments()` | Активные турниры |
| `findUpcomingTournaments(now)` | Предстоящие турниры |
| `findCompletedTournamentsByOrganizer(id)` | Завершённые турниры организатора |
| `countByOrganizerIdAndStatus(id, status)` | Подсчёт турниров |
| `findTournamentsWithAvailableSlots()` | Турниры с местами |
| `existsByNameAndOrganizerId(name, id)` | Проверка существования |

---

### 3. Database Migration

**Файл:** `src/main/resources/db/migration/V3__create_tournaments_table.sql`

**Таблица:** `tournaments`

**Индексы:**
- `idx_tournament_status` — по статусу
- `idx_tournament_organizer` — по организатору
- `idx_tournament_start_date` — по дате начала
- `idx_tournament_public` — по публичности
- `idx_tournament_format` — по формату

**Constraints:**
- `chk_participants` — текущие участники >= 0
- `chk_max_participants` — макс. участники >= 2
- `chk_dates` — end_date >= start_date
- `chk_rating_range` — валидация рейтингов (0-3500)
- `chk_time_control` — контроль времени >= 1
- `chk_time_increment` — добавка >= 0
- `chk_prize_pool` — призовой фонд >= 0
- `chk_entry_fee` — взнос >= 0

**Foreign Keys:**
- `organizer_id` → `users(id)` ON DELETE CASCADE

**Тестовые данные:**
- Создаётся турнир "Spring Championship 2026" от admin пользователя

---

### 4. Tests

**Файл:** `src/test/java/com/chessai/tournament/repository/TournamentRepositoryTest.java`

**Группы тестов:**

| Группа | Тестов | Описание |
|--------|--------|----------|
| CRUD Operations | 4 | Save, Find, Update, Delete |
| Query Methods | 6 | Различные запросы к БД |
| Business Logic | 4 | Бизнес-методы сущности |
| Edge Cases | 3 | Граничные случаи |

**Всего: 17 тестов** ✅

---

## 🗄️ Database Schema

```sql
tournaments
├── id (PK)
├── name
├── description
├── format
├── status
├── organizer_id (FK → users)
├── start_date
├── end_date
├── max_participants
├── current_participants
├── time_control_minutes
├── time_increment_seconds
├── min_rating
├── max_rating
├── prize_pool
├── entry_fee
├── is_public
├── created_at
└── updated_at
```

---

## 🚀 Использование

### Создание турнира

```java
Tournament tournament = Tournament.builder()
    .name("Spring Championship")
    .description("Open tournament")
    .format(TournamentFormat.SWISS)
    .status(TournamentStatus.DRAFT)
    .organizerId(userId)
    .startDate(LocalDateTime.now().plusDays(7))
    .maxParticipants(32)
    .timeControlMinutes(15)
    .timeIncrementSeconds(10)
    .isPublic(true)
    .build();

tournamentRepository.save(tournament);
```

### Поиск турниров

```java
// Публичные турниры с открытой регистрацией
List<Tournament> open = tournamentRepository
    .findByStatusAndIsPublicTrue(TournamentStatus.REGISTRATION_OPEN);

// Предстоящие турниры
List<Tournament> upcoming = tournamentRepository
    .findUpcomingTournaments(LocalDateTime.now());

// Турниры организатора
List<Tournament> myTournaments = tournamentRepository
    .findByOrganizerId(userId);
```

### Бизнес-логика

```java
// Проверка возможности регистрации
if (tournament.canRegisterParticipant()) {
    // Регистрация участника
}

// Проверка рейтинга
if (tournament.isRatingEligible(playerRating)) {
    // Игрок подходит по рейтингу
}

// Проверка прав
if (tournament.isOrganizer(userId)) {
    // Пользователь — организатор
}
```

---

## 📊 Статистика

| Метрика | Значение |
|---------|----------|
| Entity классов | 3 |
| Repository методов | 11 |
| Полей в Tournament | 21 |
| Бизнес-методов | 5 |
| Тестов | 17 |
| Строк кода (entity) | ~280 |
| Строк кода (tests) | ~400 |
| Строк SQL (migration) | ~100 |

---

## ✅ Выполнено

- [x] Tournament entity создан
- [x] TournamentStatus enum
- [x] TournamentFormat enum
- [x] TournamentRepository с 11 методами
- [x] Flyway миграция V3
- [x] 17 unit-тестов
- [x] Бизнес-методы в entity
- [x] Валидация полей
- [x] Индексы и constraints
- [x] Тестовые данные

---

## 🔄 Следующие шаги (Out of Scope)

- REST API endpoints для турниров
- Бизнес-логика регистрации участников
- Управление матчами турнира
- Подсчёт результатов
- Генерация пар


