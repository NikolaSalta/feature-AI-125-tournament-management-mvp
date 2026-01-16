# Конфигурация сборки Gradle (Groovy DSL)

> **Дата миграции**: 2026-01-02
> **Версия Gradle**: 8.11.1
> **Версия Spring Boot**: 3.5.9

---

## 📋 Содержание

1. [Обзор миграции](#обзор-миграции)
2. [Удалённые файлы Maven](#удалённые-файлы-maven)
3. [Структура файлов Gradle](#структура-файлов-gradle)
4. [Детальное описание build.gradle](#детальное-описание-buildgradle)
5. [Описание всех зависимостей](#описание-всех-зависимостей)
6. [Команды сборки](#команды-сборки)
7. [Сравнение Maven vs Gradle](#сравнение-maven-vs-gradle)

---

## 🔄 Обзор миграции

Проект полностью переведён на использование **Gradle с Groovy DSL**. Все файлы, связанные с Maven, были удалены для обеспечения единственной точки конфигурации сборки.

### Причины выбора Gradle

| Аспект                 | Gradle                                 | Maven                             |
| ---------------------- | -------------------------------------- | --------------------------------- |
| **Производительность** | ✅ Инкрементальная сборка, кэширование | ❌ Пересборка при каждом запуске  |
| **Гибкость**           | ✅ Groovy/Kotlin DSL, кастомные задачи | ❌ XML, ограниченная кастомизация |
| **Читаемость**         | ✅ Краткий синтаксис                   | ❌ Многословный XML               |
| **Современность**      | ✅ Активная разработка                 | ❌ Медленное развитие             |
| **Daemon**             | ✅ Фоновый процесс ускоряет сборку     | ❌ Нет                            |

---

## 🗑️ Удалённые файлы Maven

Следующие файлы и директории были **полностью удалены** из проекта:

### 1. `pom.xml` (173 строки)

**Что это было**: Главный конфигурационный файл Maven, определяющий:

- Зависимости проекта
- Плагины сборки
- Свойства проекта
- Управление зависимостями (dependencyManagement)

**Почему удалено**: Дублирование с `build.gradle`. Все зависимости и настройки уже определены в Gradle.

### 2. `mvnw` (11,790 байт)

**Что это было**: Maven Wrapper скрипт для Unix/Linux/macOS.
Позволял запускать Maven без его предварительной установки.

**Почему удалено**: Заменён на `gradlew` (Gradle Wrapper).

### 3. `mvnw.cmd` (8,292 байт)

**Что это было**: Maven Wrapper скрипт для Windows.

**Почему удалено**: Заменён на `gradlew.bat` (Gradle Wrapper для Windows).

### 4. `.mvn/` директория

**Что содержало**:

```
.mvn/
└── wrapper/
    ├── maven-wrapper.jar         # JAR для скачивания Maven
    └── maven-wrapper.properties  # Версия Maven и URL
```

**Почему удалено**: Заменено на `.gradle/` и `gradle/wrapper/`.

---

## 📁 Структура файлов Gradle

Текущая структура сборочных файлов проекта:

```
tournament-service-be/
├── build.gradle           # Главный файл конфигурации (Groovy DSL)
├── settings.gradle        # Настройки проекта (имя, модули)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar          # JAR для автоскачивания Gradle
│       └── gradle-wrapper.properties   # Версия Gradle (8.11.1)
├── gradlew               # Gradle Wrapper для Unix/macOS
├── gradlew.bat           # Gradle Wrapper для Windows
└── .gradle/              # Кэш Gradle (в .gitignore)
```

---

## 📝 Детальное описание build.gradle

Полное описание каждой секции `build.gradle` (130 строк):

### Секция: Plugins (строки 1-5)

```groovy
plugins {
    id 'java'                                        // Компиляция Java
    id 'org.springframework.boot' version '3.5.9'   // Spring Boot плагин
    id 'io.spring.dependency-management' version '1.1.7'  // BOM управление
}
```

| Плагин                            | Версия       | Назначение                                                 |
| --------------------------------- | ------------ | ---------------------------------------------------------- |
| `java`                            | (встроенный) | Компиляция, тестирование, JAR-упаковка Java-кода           |
| `org.springframework.boot`        | 3.5.9        | Создание fat JAR, bootRun, управление зависимостями Spring |
| `io.spring.dependency-management` | 1.1.7        | Импорт Spring BOM для автоматического определения версий   |

### Секция: Project Metadata (строки 7-9)

```groovy
group = 'com.chessai'
version = '1.0.0-MVP'
description = 'Tournament Service Backend (MVP)'
```

| Свойство      | Значение              | Использование                                  |
| ------------- | --------------------- | ---------------------------------------------- |
| `group`       | `com.chessai`         | GroupId в Maven Repository, пакет по умолчанию |
| `version`     | `1.0.0-MVP`           | Версия артефакта, включается в имя JAR         |
| `description` | Tournament Service... | Описание для документации                      |

### Секция: Java Configuration (строки 11-14)

```groovy
java {
    sourceCompatibility = '17'
    targetCompatibility = '17'
}
```

| Свойство              | Значение | Описание                          |
| --------------------- | -------- | --------------------------------- |
| `sourceCompatibility` | Java 17  | Версия исходного кода (синтаксис) |
| `targetCompatibility` | Java 17  | Версия байт-кода (JVM target)     |

### Секция: Configurations (строки 16-20)

```groovy
configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}
```

**Назначение**: Настройка для Lombok. Аннотации `@Getter`, `@Setter` и т.д. обрабатываются во время компиляции, но не включаются в runtime JAR.

### Секция: Repositories (строки 22-24)

```groovy
repositories {
    mavenCentral()
}
```

**Maven Central** — главный публичный репозиторий Java-библиотек.
URL: https://repo.maven.apache.org/maven2/

### Секция: Version Properties (строки 26-31)

```groovy
ext {
    set('jjwtVersion', '0.12.6')
    set('bucket4jVersion', '8.10.1')
    set('springCloudVersion', '2023.0.3')
    set('springdocVersion', '2.3.0')
}
```

| Переменная           | Версия   | Библиотека               |
| -------------------- | -------- | ------------------------ |
| `jjwtVersion`        | 0.12.6   | JJWT (JSON Web Token)    |
| `bucket4jVersion`    | 8.10.1   | Bucket4j (Rate Limiting) |
| `springCloudVersion` | 2023.0.3 | Spring Cloud BOM         |
| `springdocVersion`   | 2.3.0    | Springdoc OpenAPI        |

---

## 📦 Описание всех зависимостей

### REST / Web

```groovy
implementation 'org.springframework.boot:spring-boot-starter-web'
```

| Аспект           | Описание                                      |
| ---------------- | --------------------------------------------- |
| **Что включает** | Embedded Tomcat, Spring MVC, Jackson JSON     |
| **Для чего**     | REST API, HTTP-контроллеры, JSON сериализация |
| **Scope**        | `implementation` (compile + runtime)          |

---

### Validation

```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

| Аспект           | Описание                                           |
| ---------------- | -------------------------------------------------- |
| **Что включает** | Hibernate Validator, Jakarta Validation API        |
| **Для чего**     | Аннотации `@Valid`, `@NotBlank`, `@Email`, `@Size` |
| **Scope**        | `implementation`                                   |

---

### Security

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
```

| Аспект           | Описание                                                  |
| ---------------- | --------------------------------------------------------- |
| **Что включает** | Spring Security Core, Web Security, Crypto                |
| **Для чего**     | Аутентификация, авторизация, BCrypt, фильтры безопасности |
| **Scope**        | `implementation`                                          |

---

### JWT (JSON Web Token)

```groovy
implementation "io.jsonwebtoken:jjwt-api:${jjwtVersion}"
runtimeOnly "io.jsonwebtoken:jjwt-impl:${jjwtVersion}"
runtimeOnly "io.jsonwebtoken:jjwt-jackson:${jjwtVersion}"
```

| Артефакт       | Scope          | Описание                                 |
| -------------- | -------------- | ---------------------------------------- |
| `jjwt-api`     | implementation | API интерфейсы для создания/парсинга JWT |
| `jjwt-impl`    | runtimeOnly    | Имплементация (не нужна при компиляции)  |
| `jjwt-jackson` | runtimeOnly    | Jackson-сериализация для JWT claims      |

**Версия**: 0.12.6 (последняя стабильная, декабрь 2024)

---

### Rate Limiting

```groovy
implementation "com.bucket4j:bucket4j-core:${bucket4jVersion}"
```

| Аспект       | Описание                                           |
| ------------ | -------------------------------------------------- |
| **Что это**  | Java-имплементация алгоритма Token Bucket          |
| **Для чего** | Ограничение частоты запросов (10 req/min для auth) |
| **Версия**   | 8.10.1                                             |

---

### Lombok

```groovy
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

| Scope                 | Описание                                   |
| --------------------- | ------------------------------------------ |
| `compileOnly`         | Аннотации доступны при компиляции          |
| `annotationProcessor` | Генерация кода (геттеры, сеттеры, билдеры) |

**Аннотации**: `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Data`, `@Slf4j`

---

### JPA / Hibernate

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

| Аспект              | Описание                                 |
| ------------------- | ---------------------------------------- |
| **Что включает**    | Hibernate ORM, Spring Data JPA, HikariCP |
| **Для чего**        | ORM маппинг, репозитории, транзакции     |
| **Connection Pool** | HikariCP (по умолчанию)                  |

---

### PostgreSQL

```groovy
runtimeOnly 'org.postgresql:postgresql'
```

| Аспект      | Описание                                |
| ----------- | --------------------------------------- |
| **Что это** | JDBC-драйвер PostgreSQL                 |
| **Scope**   | `runtimeOnly` (не нужен при компиляции) |
| **Версия**  | Управляется Spring Boot BOM             |

---

### Flyway (Database Migrations)

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

| Артефакт                     | Описание                       |
| ---------------------------- | ------------------------------ |
| `flyway-core`                | Ядро Flyway для миграций       |
| `flyway-database-postgresql` | PostgreSQL-специфичные функции |

**Миграции расположены**: `src/main/resources/db/migration/`

---

### Actuator

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

| Аспект        | Описание                                                  |
| ------------- | --------------------------------------------------------- |
| **Что это**   | Production-ready features                                 |
| **Endpoints** | `/actuator/health`, `/actuator/info`, `/actuator/metrics` |
| **Для чего**  | Мониторинг, health checks, метрики                        |

---

### Swagger / OpenAPI

```groovy
implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}"
```

| Аспект         | Описание                              |
| -------------- | ------------------------------------- |
| **Что это**    | Автогенерация OpenAPI 3.0 spec        |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **JSON spec**  | http://localhost:8080/v3/api-docs     |
| **Версия**     | 2.3.0                                 |

---

### HashiCorp Vault

```groovy
implementation 'org.springframework.cloud:spring-cloud-starter-vault-config'
```

| Аспект       | Описание                                           |
| ------------ | -------------------------------------------------- |
| **Что это**  | Интеграция со Spring Cloud Vault                   |
| **Для чего** | Загрузка секретов (DB пароль, JWT secret) из Vault |
| **Версия**   | Из Spring Cloud BOM 2023.0.3                       |

---

### Testing

```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

| Артефакт                   | Описание                                                 |
| -------------------------- | -------------------------------------------------------- |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, Spring Test                   |
| `spring-security-test`     | `@WithMockUser`, `@WithUserDetails`, Security test utils |
| `junit-platform-launcher`  | JUnit 5 platform для запуска тестов                      |

---

### Dependency Management

```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

**BOM (Bill of Materials)** — централизованное управление версиями для Spring Cloud зависимостей.

---

## ⚙️ Конфигурация задач

### Test Configuration

```groovy
tasks.named('test') {
    useJUnitPlatform()
}
```

Использует JUnit 5 Platform для запуска тестов.

### BootJar Configuration

```groovy
tasks.named('bootJar') {
    archiveFileName = "${project.name}-${project.version}.jar"
}
```

Результат: `tournament-service-be-1.0.0-MVP.jar`

### Build Info

```groovy
springBoot {
    buildInfo()
}
```

Генерирует `META-INF/build-info.properties` с информацией о сборке (время, версия, Git commit).

---

## 🚀 Команды сборки

### Основные команды

| Команда             | Описание              |
| ------------------- | --------------------- |
| `./gradlew bootRun` | Запуск приложения     |
| `./gradlew build`   | Полная сборка + тесты |
| `./gradlew bootJar` | Создание fat JAR      |
| `./gradlew test`    | Запуск тестов         |
| `./gradlew clean`   | Очистка build/        |

### Продвинутые команды

| Команда                                                   | Описание                      |
| --------------------------------------------------------- | ----------------------------- |
| `./gradlew dependencies`                                  | Дерево зависимостей           |
| `./gradlew dependencyInsight --dependency <name>`         | Анализ конкретной зависимости |
| `./gradlew bootRun --args='--spring.profiles.active=dev'` | Запуск с профилем             |
| `./gradlew build -x test`                                 | Сборка без тестов             |
| `./gradlew --stop`                                        | Остановка Gradle Daemon       |

### Полезные флаги

| Флаг            | Описание                    |
| --------------- | --------------------------- |
| `--info`        | Подробный вывод             |
| `--debug`       | Debug-логирование           |
| `--stacktrace`  | Stack trace при ошибках     |
| `--parallel`    | Параллельная сборка модулей |
| `--build-cache` | Использовать кэш сборки     |

---

## 📊 Сравнение Maven vs Gradle

### Эквивалентность команд

| Maven                 | Gradle                          | Описание                    |
| --------------------- | ------------------------------- | --------------------------- |
| `mvn clean`           | `./gradlew clean`               | Очистка                     |
| `mvn compile`         | `./gradlew compileJava`         | Компиляция                  |
| `mvn test`            | `./gradlew test`                | Тесты                       |
| `mvn package`         | `./gradlew bootJar`             | Сборка JAR                  |
| `mvn install`         | `./gradlew publishToMavenLocal` | Публикация в локальный репо |
| `mvn spring-boot:run` | `./gradlew bootRun`             | Запуск                      |
| `mvn dependency:tree` | `./gradlew dependencies`        | Зависимости                 |

### Эквивалентность конфигурации

| Maven (pom.xml)          | Gradle (build.gradle)                           |
| ------------------------ | ----------------------------------------------- |
| `<dependency>`           | `implementation` / `runtimeOnly`                |
| `<plugin>`               | `plugins { id '...' }`                          |
| `<properties>`           | `ext { }`                                       |
| `<dependencyManagement>` | `dependencyManagement { imports { mavenBom } }` |
| `<parent>`               | Spring Boot plugin                              |

---

## ✅ Проверка успешности миграции

После миграции выполните:

```bash
# 1. Проверка сборки
./gradlew clean build

# 2. Запуск приложения
./gradlew bootRun

# 3. Проверка что Maven полностью удалён
ls pom.xml mvnw mvnw.cmd .mvn  # Должно быть: No such file or directory

# 4. Проверка версий
./gradlew --version
```

---

## 📚 Дополнительные ресурсы

- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/gradle-plugin/index.html)
- [Migrating from Maven to Gradle](https://docs.gradle.org/current/userguide/migrating_from_maven.html)

---

> **Автор**: AI Assistant
> **Дата создания**: 2026-01-02
> **Последнее обновление**: 2026-01-02
