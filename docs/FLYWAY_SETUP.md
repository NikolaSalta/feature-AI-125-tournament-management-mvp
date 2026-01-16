# Flyway Database Migrations - Подробное описание установки и настройки

**Дата:** 2024-12-22  
**Версия:** 0.2.1  
**Задача:** Добавление Flyway для управления миграциями базы данных

---

## 📋 Содержание

1. [Обзор изменений](#обзор-изменений)
2. [Добавленные зависимости](#добавленные-зависимости)
3. [Конфигурация приложения](#конфигурация-приложения)
4. [Структура файлов](#структура-файлов)
5. [Детальное описание изменений](#детальное-описание-изменений)
6. [Как работает Flyway](#как-работает-flyway)
7. [Примеры использования](#примеры-использования)
8. [Проверка работы](#проверка-работы)

---

## Обзор изменений

Flyway был интегрирован в проект для управления версионированием схемы базы данных PostgreSQL. Это позволяет:
- Контролировать изменения структуры БД через версионированные SQL-скрипты
- Автоматически применять миграции при запуске приложения
- Отслеживать историю изменений схемы БД
- Обеспечивать консистентность БД между окружениями (dev, staging, prod)

### Что было изменено:

1. ✅ Добавлены Gradle зависимости в `build.gradle`
2. ✅ Настроена конфигурация Flyway в `application.yml`
3. ✅ Создана структура директорий для миграций
4. ✅ Добавлена первая миграция-заглушка
5. ✅ Обновлена документация проекта

---

## Добавленные зависимости

### Файл: `build.gradle`

**Местоположение:** После зависимости PostgreSQL (строка ~62), перед Actuator

**Добавленные зависимости:**

```groovy
// Flyway (Database migrations)
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

### Детальное описание зависимостей:

#### 1. `flyway-core`
- **Группа:** `org.flywaydb`
- **Артефакт:** `flyway-core`
- **Версия:** Управляется Spring Boot BOM - автоматически подтягивается совместимая версия
- **Назначение:** Основная библиотека Flyway, содержит:
  - Ядро системы миграций
  - Механизм обнаружения и выполнения миграций
  - API для работы с миграциями
  - Валидацию SQL-скриптов
  - Управление метаданными миграций

#### 2. `flyway-database-postgresql`
- **Группа:** `org.flywaydb`
- **Артефакт:** `flyway-database-postgresql`
- **Версия:** Управляется Spring Boot Parent
- **Назначение:** Специфичный драйвер для PostgreSQL, обеспечивает:
  - Оптимизированную работу с PostgreSQL
  - Поддержку специфичных для PostgreSQL функций
  - Корректную работу с типами данных PostgreSQL
  - Поддержку расширений PostgreSQL (если потребуется)

### Почему две зависимости?

- `flyway-core` - универсальная библиотека, работает с любой БД через JDBC
- `flyway-database-postgresql` - оптимизированный драйвер для PostgreSQL, обеспечивает лучшую производительность и поддержку специфичных функций

**Примечание:** Spring Boot автоматически определяет версии этих зависимостей на основе версии `spring-boot-starter-parent` (3.5.9), что гарантирует совместимость.

---

## Конфигурация приложения

### Файл: `src/main/resources/application.yml`

**Местоположение:** После секции `spring.jpa`, перед секцией `server`

**Добавленная конфигурация:**

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    clean-disabled: true
```

### Детальное описание каждого параметра:

#### 1. `enabled: true`
- **Тип:** Boolean
- **Значение по умолчанию:** `true` (можно не указывать)
- **Назначение:** Включает/выключает Flyway
- **Когда менять:** 
  - Установить `false` если нужно временно отключить миграции (например, для отладки)
  - В production всегда должно быть `true`

#### 2. `locations: classpath:db/migration`
- **Тип:** String (или массив строк)
- **Значение по умолчанию:** `classpath:db/migration`
- **Назначение:** Указывает путь к директории с миграциями
- **Формат:** 
  - `classpath:` - ресурсы из `src/main/resources/`
  - `filesystem:` - файлы из файловой системы
  - Можно указать несколько путей через запятую
- **Текущее значение:** Миграции ищутся в `src/main/resources/db/migration/`

#### 3. `baseline-on-migrate: true`
- **Тип:** Boolean
- **Значение по умолчанию:** `false`
- **Назначение:** Создает baseline для существующих баз данных
- **Как работает:**
  - Если БД уже существует и в ней нет таблицы `flyway_schema_history`
  - Flyway создаст baseline с версией `baseline-version` (0)
  - Все последующие миграции будут применяться после baseline
- **Зачем нужно:** Позволяет начать использовать Flyway на уже существующей БД без потери данных

#### 4. `baseline-version: 0`
- **Тип:** Integer или String
- **Значение по умолчанию:** `1`
- **Назначение:** Версия baseline для существующих БД
- **Как работает:**
  - Если `baseline-on-migrate: true` и БД существует
  - Flyway пометит текущее состояние БД как версию `0`
  - Все миграции с версией > 0 будут применены
- **Текущее значение:** `0` - означает, что существующая БД считается "нулевой версией"

#### 5. `validate-on-migrate: true`
- **Тип:** Boolean
- **Значение по умолчанию:** `true`
- **Назначение:** Проверяет целостность миграций перед применением
- **Что проверяет:**
  - Соответствие checksum миграций (защита от изменений уже примененных миграций)
  - Отсутствие "дыр" в версиях миграций
  - Корректность имен файлов миграций
- **Зачем нужно:** Предотвращает проблемы при развертывании, если миграции были изменены после применения

#### 6. `clean-disabled: true`
- **Тип:** Boolean
- **Значение по умолчанию:** `false` (clean разрешен)
- **Назначение:** Отключает команду `flyway.clean()`
- **Что делает clean:**
  - Удаляет все объекты из схемы БД (таблицы, индексы, последовательности и т.д.)
  - Очищает таблицу `flyway_schema_history`
- **Зачем отключать:** Команда clean очень опасна в production, может удалить все данные
- **Текущее значение:** `true` - команда clean отключена для безопасности

### Дополнительные параметры (не использованы, но доступны):

```yaml
spring:
  flyway:
    # Дополнительные опции (примеры):
    # schemas: public                    # Схемы для миграций (по умолчанию default schema)
    # table: flyway_schema_history       # Имя таблицы для хранения истории (по умолчанию)
    # sql-migration-prefix: V            # Префикс для версионированных миграций (по умолчанию V)
    # sql-migration-separator: __        # Разделитель в имени файла (по умолчанию __)
    # sql-migration-suffixes: .sql       # Расширения файлов миграций
    # placeholder-replacement: true      # Замена плейсхолдеров в SQL
    # placeholders:                      # Словарь плейсхолдеров
    #   key1: value1
    #   key2: value2
    # out-of-order: false                # Разрешить применение миграций не по порядку
    # ignore-missing-migrations: false    # Игнорировать отсутствующие миграции
    # ignore-ignored-migrations: false   # Игнорировать помеченные как ignored
    # group: false                       # Группировать миграции в транзакции
    # mixed: false                       # Разрешить миграции и откаты (undo)
    # validate-migration-naming: true    # Валидация имен файлов миграций
```

---

## Структура файлов

### Созданные директории и файлы:

```
src/main/resources/
└── db/
    └── migration/
        └── V1__init.sql
```

### Детальное описание:

#### Директория: `src/main/resources/db/migration/`
- **Путь:** `src/main/resources/db/migration/`
- **Назначение:** Стандартная директория для хранения SQL-миграций Flyway
- **Почему именно этот путь:**
  - `src/main/resources/` - ресурсы, доступные через classpath
  - `db/migration/` - стандартное соглашение Flyway
  - Все файлы из этой директории попадают в JAR при сборке

#### Файл: `V1__init.sql`
- **Полный путь:** `src/main/resources/db/migration/V1__init.sql`
- **Имя файла:** `V1__init.sql`
- **Формат имени:** `V{version}__{description}.sql`
  - `V` - префикс для версионированных миграций
  - `1` - номер версии (целое число)
  - `__` - разделитель (два подчеркивания)
  - `init` - описание миграции
  - `.sql` - расширение файла

**Содержимое файла:**

```sql
-- Flyway migration: Initial database schema
-- Version: 1.0
-- Description: Initial migration file for Tournament Service
-- 
-- This file is a placeholder for future database migrations.
-- When domain entities are implemented, add corresponding CREATE TABLE statements here.
--
-- Migration naming convention: V{version}__{description}.sql
-- Example: V1__init.sql, V2__create_tournaments_table.sql

-- Placeholder: No tables created yet
-- Tables will be added in subsequent migrations when domain model is implemented
```

**Назначение:** 
- Заглушка для первой миграции
- Создает baseline версию в таблице `flyway_schema_history`
- Содержит документацию по формату имен файлов
- Готова для добавления реальных CREATE TABLE statements

---

## Детальное описание изменений

### 1. Изменение `pom.xml`

**Файл:** `pom.xml`  
**Строки:** 64-72

**Было:**
```xml
        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Actuator (health, metrics, readiness) -->
```

**Стало:**
```xml
        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway (Database migrations) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Actuator (health, metrics, readiness) -->
```

**Объяснение:**
- Зависимости добавлены после PostgreSQL, так как Flyway работает с БД
- Зависимости добавлены перед Actuator для логической группировки
- Обе зависимости без указания версии - версии управляются Spring Boot Parent
- Обе зависимости без `<scope>` - по умолчанию `compile`, доступны во время выполнения

### 2. Изменение `application.yml`

**Файл:** `src/main/resources/application.yml`  
**Строки:** 20-26

**Было:**
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

server:
```

**Стало:**
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    clean-disabled: true

server:
```

**Объяснение:**
- Конфигурация Flyway добавлена в секцию `spring:`
- Расположена после `jpa:` для логической группировки (оба относятся к БД)
- Все параметры настроены для безопасной работы с существующими БД
- `ddl-auto: validate` остается - Flyway управляет схемой, Hibernate только валидирует

### 3. Создание структуры директорий

**Команда:**
```bash
mkdir -p src/main/resources/db/migration
```

**Результат:**
- Создана директория `db/` в `src/main/resources/`
- Создана поддиректория `migration/` в `db/`
- Директории пустые, готовы для миграций

### 4. Создание первой миграции

**Файл:** `src/main/resources/db/migration/V1__init.sql`  
**Содержимое:** SQL-комментарии с описанием и placeholder

**Назначение:**
- Создает первую запись в истории миграций
- Документирует формат имен файлов
- Готова для добавления реальных SQL-команд

---

## Как работает Flyway

### Процесс запуска приложения:

1. **Инициализация Flyway**
   - Spring Boot автоматически обнаруживает Flyway в classpath
   - Читает конфигурацию из `application.yml`
   - Подключается к БД через DataSource

2. **Проверка таблицы истории**
   - Flyway проверяет наличие таблицы `flyway_schema_history`
   - Если таблицы нет:
     - При `baseline-on-migrate: true` - создает baseline
     - Иначе - создает таблицу с нуля

3. **Обнаружение миграций**
   - Сканирует директорию `classpath:db/migration/`
   - Находит все файлы, соответствующие паттерну `V{version}__{description}.sql`
   - Сортирует по версии (число после `V`)

4. **Валидация миграций**
   - При `validate-on-migrate: true` проверяет:
     - Checksum уже примененных миграций
     - Отсутствие пропущенных версий
     - Корректность имен файлов

5. **Применение миграций**
   - Находит непримененные миграции (версии > последней примененной)
   - Применяет их по порядку в транзакциях
   - Записывает результат в `flyway_schema_history`

6. **Завершение**
   - Выводит информацию о примененных миграциях в логи
   - Приложение продолжает запуск

### Таблица `flyway_schema_history`

Flyway автоматически создает таблицу для отслеживания миграций:

```sql
CREATE TABLE flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200),
    type VARCHAR(20),
    script VARCHAR(1000),
    checksum INTEGER,
    installed_by VARCHAR(100),
    installed_on TIMESTAMP,
    execution_time INTEGER,
    success BOOLEAN
);
```

**Колонки:**
- `installed_rank` - порядковый номер применения
- `version` - версия миграции (из имени файла)
- `description` - описание (из имени файла)
- `type` - тип миграции (SQL, JAVA и т.д.)
- `script` - имя файла миграции
- `checksum` - контрольная сумма содержимого файла
- `installed_by` - пользователь БД, применивший миграцию
- `installed_on` - дата и время применения
- `execution_time` - время выполнения в миллисекундах
- `success` - успешность применения

---

## Примеры использования

### Создание новой миграции

#### Пример 1: Создание таблицы турниров

**Файл:** `V2__create_tournaments_table.sql`

```sql
-- Migration: Create tournaments table
-- Version: 2
-- Description: Initial tournaments table structure

CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tournaments_status ON tournaments(status);
CREATE INDEX idx_tournaments_start_date ON tournaments(start_date);

COMMENT ON TABLE tournaments IS 'Шахматные турниры';
COMMENT ON COLUMN tournaments.status IS 'Статус турнира: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED';
```

#### Пример 2: Добавление колонки

**Файл:** `V3__add_max_players_to_tournaments.sql`

```sql
-- Migration: Add max_players column to tournaments
-- Version: 3

ALTER TABLE tournaments 
ADD COLUMN max_players INTEGER;

COMMENT ON COLUMN tournaments.max_players IS 'Максимальное количество участников';
```

#### Пример 3: Создание таблицы участников

**Файл:** `V4__create_players_table.sql`

```sql
-- Migration: Create players table
-- Version: 4

CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    rating INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_players_tournament_id ON players(tournament_id);
```

### Формат имен файлов

**Правила:**
- Префикс: `V` (версионированные) или `R` (repeatable)
- Версия: целое число (1, 2, 3...) или дата (20241222)
- Разделитель: `__` (два подчеркивания)
- Описание: латинские буквы, цифры, подчеркивания
- Расширение: `.sql`

**Примеры правильных имен:**
- ✅ `V1__init.sql`
- ✅ `V2__create_tournaments.sql`
- ✅ `V3__add_column.sql`
- ✅ `V20241222__initial_schema.sql`
- ✅ `R__update_views.sql` (repeatable миграция)

**Примеры неправильных имен:**
- ❌ `1_init.sql` (нет префикса V)
- ❌ `V1_init.sql` (один подчеркивание вместо двух)
- ❌ `V1.1__init.sql` (версия не целое число)
- ❌ `V1 init.sql` (пробел в имени)

### Repeatable миграции

Миграции с префиксом `R` применяются каждый раз, если их checksum изменился:

**Файл:** `R__update_statistics_view.sql`

```sql
-- Repeatable migration: Update statistics view
-- Применяется каждый раз при изменении

CREATE OR REPLACE VIEW tournament_statistics AS
SELECT 
    t.id,
    t.name,
    COUNT(p.id) as player_count
FROM tournaments t
LEFT JOIN players p ON p.tournament_id = t.id
GROUP BY t.id, t.name;
```

---

## Проверка работы

### 1. Проверка зависимостей

```bash
./mvnw dependency:tree | grep flyway
```

**Ожидаемый вывод:**
```
[INFO] +- org.flywaydb:flyway-core:jar:10.x.x:compile
[INFO] +- org.flywaydb:flyway-database-postgresql:jar:10.x.x:compile
```

### 2. Запуск приложения

```bash
./mvnw spring-boot:run
```

**Ожидаемый вывод в логах:**
```
Flyway Community Edition 10.x.x by Redgate
Database: jdbc:postgresql://localhost:5432/tournament_db
Successfully validated 1 migration (execution time 00:00.012s)
Current version of schema "public": 1
Schema "public" is up to date. No migration necessary.
```

### 3. Проверка таблицы истории

```sql
SELECT * FROM flyway_schema_history;
```

**Ожидаемый результат:**
```
installed_rank | version | description | type | script          | success
---------------|--------|-------------|------|-----------------|--------
1              | 1      | init        | SQL  | V1__init.sql    | true
```

### 4. Проверка конфигурации

```bash
# Проверить, что Flyway включен
grep -A 5 "flyway:" src/main/resources/application.yml
```

### 5. Тестирование новой миграции

1. Создайте тестовую миграцию `V2__test.sql`:
```sql
-- Test migration
SELECT 1;
```

2. Запустите приложение
3. Проверьте логи - миграция должна примениться
4. Удалите тестовую миграцию

---

## Важные замечания

### ⚠️ Безопасность

1. **Никогда не изменяйте уже примененные миграции**
   - Flyway проверяет checksum
   - Изменение примененной миграции вызовет ошибку
   - Создайте новую миграцию для изменений

2. **Команда clean отключена**
   - `clean-disabled: true` защищает от случайного удаления данных
   - В production никогда не включайте clean

3. **Валидация включена**
   - `validate-on-migrate: true` проверяет целостность
   - Не отключайте без крайней необходимости

### 📝 Best Practices

1. **Именование миграций**
   - Используйте понятные описания
   - Один файл - одна логическая задача
   - Не пропускайте версии

2. **Содержимое миграций**
   - Пишите идемпотентные SQL (где возможно)
   - Используйте транзакции для безопасности
   - Добавляйте комментарии

3. **Тестирование**
   - Тестируйте миграции на копии БД
   - Проверяйте откат (rollback) если нужен
   - Документируйте breaking changes

4. **Версионирование**
   - Используйте последовательные номера версий
   - Для датированных миграций: `V20241222__description.sql`
   - Не смешивайте форматы

---

## Интеграция с Hibernate

### Текущая конфигурация:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Hibernate только валидирует схему
  flyway:
    enabled: true         # Flyway управляет схемой
```

### Как это работает:

1. **Flyway применяет миграции** - создает/изменяет структуру БД
2. **Hibernate валидирует** - проверяет, что JPA entities соответствуют схеме БД
3. **При несоответствии** - приложение не запустится с ошибкой валидации

### Альтернативные режимы:

- `ddl-auto: none` - Hibernate ничего не делает (только Flyway)
- `ddl-auto: update` - Hibernate может изменять схему (конфликт с Flyway!)
- `ddl-auto: create` - Hibernate создает схему заново (конфликт с Flyway!)
- `ddl-auto: create-drop` - Hibernate создает и удаляет (конфликт с Flyway!)

**Рекомендация:** Использовать `validate` с Flyway для безопасности.

---

## Troubleshooting

### Проблема: "FlywayException: Validate failed"

**Причина:** Изменена уже примененная миграция

**Решение:**
1. Восстановите оригинальное содержимое миграции
2. Или создайте новую миграцию для изменений

### Проблема: "Migration checksum mismatch"

**Причина:** Checksum миграции не совпадает с сохраненным

**Решение:**
1. Проверьте, не изменяли ли вы примененную миграцию
2. Если изменение необходимо - создайте новую миграцию

### Проблема: Миграции не применяются

**Проверьте:**
1. `spring.flyway.enabled=true` в конфигурации
2. Файлы миграций в правильной директории
3. Правильный формат имен файлов
4. Подключение к БД работает

### Проблема: "Baseline required"

**Причина:** БД существует, но нет baseline

**Решение:**
1. Убедитесь, что `baseline-on-migrate: true`
2. Или выполните baseline вручную через Flyway API

---

## Ссылки и ресурсы

- [Официальная документация Flyway](https://flywaydb.org/documentation/)
- [Spring Boot + Flyway Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
- [Flyway Best Practices](https://flywaydb.org/documentation/learnmore/bestpractices)

---

## История изменений

- **2024-12-22** - Первоначальная интеграция Flyway (v0.2.1)
  - Добавлены зависимости
  - Настроена конфигурация
  - Создана первая миграция-заглушка

---

**Документ создан:** 2024-12-22  
**Версия документа:** 1.0  
**Автор:** AI Assistant



