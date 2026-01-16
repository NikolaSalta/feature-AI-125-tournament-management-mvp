# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- Tournament domain model implementation (AI-125)
- REST API endpoints
- Business logic layer

---

## [0.7.2] - 2026-01-02

### Added

- **🐳 Docker Support**

  - `Dockerfile` — multi-stage build (eclipse-temurin:17-jre-alpine)
  - `docker-compose.yml` — PostgreSQL + App + Vault (optional)
  - Health checks для всех сервисов

- **📬 Postman Collection**

  - `postman/Tournament_Service_API.postman_collection.json`
  - 8 endpoints: Health, Auth (4), Swagger (2), Rate Limit Test
  - Автоматическое сохранение токенов

- **📖 Документация**
  - `docs/DOCKER_SETUP.md` — подробная документация Docker

---

## [0.7.1] - 2026-01-02

### Removed

- **🗑️ Git-crypt полностью удалён**

  - Проект использует только HashiCorp Vault для секретов
  - Удалены: `docs/GIT_CRYPT_SETUP.md`, `docs/SOPS_ALTERNATIVE.md`, `docs/SECRETS_COMPARISON.md`
  - Обновлён `.gitattributes` (убраны git-crypt фильтры)
  - Обновлён `README.md`

- **🧹 Очистка проекта**
  - Удалены неиспользуемые файлы из корня: `TEST_GIT_CRYPT.md`, `GIT_CRYPT_SUCCESS.md`, `MIGRATION_SUCCESS.md`, `GRADLE_MIGRATION_COMPLETE.md`, `VAULT_INTEGRATION_SUMMARY.md`
  - Удалена директория `target/` (Maven → Gradle `build/`)
  - Удалена директория `tournament_service 3/` (случайный дубликат)

---

## [0.7.0] - 2026-01-02

### Changed

- **⏱️ Access Token увеличен с 15 до 30 минут**
  - `application.yml`: `JWT_ACCESS_EXPIRATION` = 1800000
  - `JwtProperties.java`: default = 30 минут

### Fixed

- **🔴 Memory Leak в RateLimitingService**

  - Заменён `ConcurrentHashMap` на Caffeine TTL-кэш
  - Auto-eviction через 10 минут неактивности
  - Максимум 10,000 записей для защиты от OOM
  - Добавлена зависимость `caffeine:3.1.8`

- **🔴 IP Spoofing уязвимость**

  - Исправлен `RateLimitingFilter.getClientIp()`
  - Теперь берётся ПОСЛЕДНИЙ IP из `X-Forwarded-For`
  - Предотвращает обход Rate Limiting

- **🟡 Дублирование Role.USER при регистрации**

  - Удалён дублирующий `addRole(Role.USER)` в `AuthService.register()`
  - Роль уже добавляется в конструкторе `User`

- **🟡 Неработающая @Size валидация пароля**

  - Удалена бессмысленная `@Size` аннотация в `User.password`
  - Валидация остаётся в `RegisterRequest`

- **🟡 Неполная проверка в refreshToken**

  - Добавлены проверки `isAccountNonLocked()` и `isAccountNonExpired()`
  - Заблокированные пользователи не смогут обновить токен

- **🟡 ClassCastException в GlobalExceptionHandler**

  - Добавлена проверка `instanceof FieldError` перед кастингом
  - Обработка `ObjectError` для class-level валидаций

- **🟢 JWT issuer не проверялся**
  - Добавлен `requireIssuer()` в `JwtTokenProvider.parseToken()`
  - Токены из других сервисов теперь отклоняются

### Security

- Устранена возможность IP spoofing для обхода Rate Limiting
- Добавлена проверка статуса аккаунта при refresh token
- Валидация issuer предотвращает использование чужих токенов

---

## [0.6.1] - 2026-01-02

### Removed

- **🗑️ Полное удаление Maven**
  - Удалён `pom.xml` (173 строки)
  - Удалён `mvnw` (Maven Wrapper для Unix/macOS)
  - Удалён `mvnw.cmd` (Maven Wrapper для Windows)
  - Удалена директория `.mvn/wrapper/`

### Added

- **📚 Документация Gradle**
  - `docs/GRADLE_BUILD_CONFIGURATION.md` - подробное описание build.gradle
  - Описание всех 16 зависимостей с версиями
  - Сравнение Maven и Gradle команд
  - Руководство по миграции

### Changed

- README.md: Все команды `./mvnw` заменены на `./gradlew`
- README.md: Путь JAR изменён с `target/` на `build/libs/`

### Technical Details

- Единственная система сборки: **Gradle 8.11.1 (Groovy DSL)**
- Все зависимости полностью сохранены
- Совместимость с Spring Boot 3.5.9

---

## [0.6.0] - 2026-01-02

### Changed

- **🔄 Миграция с Maven на Gradle**
  - Создан build.gradle из pom.xml
  - Создан settings.gradle
  - Добавлен Gradle Wrapper 8.11.1
  - Обновлен .gitignore для Gradle
  - Все команды в README.md заменены на Gradle
  - Скрипты Vault обновлены для Gradle
- **📚 Документация**
  - docs/MAVEN_TO_GRADLE_MIGRATION.md - полное руководство по миграции
  - GRADLE_MIGRATION_COMPLETE.md - итоговый отчет о миграции
  - Сравнение Maven и Gradle команд
  - Troubleshooting для Gradle

### Technical Details

- Build Tool: Gradle 8.11.1 (Groovy DSL)
- Все зависимости сохранены
- Совместимость с Spring Boot 3.5.9
- Улучшена производительность сборки (до 3x быстрее)

### Migration Benefits

- ⚡ Faster builds (incremental compilation, build cache)
- 🎯 Cleaner syntax (Groovy DSL vs XML)
- 🚀 Modern tooling (parallel execution, continuous build)
- 💪 More powerful (programmable builds)

---

## [0.4.0] - 2026-01-02

### Added

- **HashiCorp Vault Integration**
  - Spring Cloud Vault Config dependency (2023.0.3)
  - VaultConfig.java for connection management
  - application-vault.yml profile configuration
  - Support for TOKEN, KUBERNETES, AWS_IAM, APPROLE authentication methods
  - Automatic secret loading from Vault KV v2 engine
- **Vault Setup Scripts**
  - setup-vault-dev.sh - Development environment setup
  - setup-vault-prod.sh - Production environment setup with policies
  - read-secrets.sh - Utility for reading secrets from Vault
- **API Versioning**
  - ApiVersion.java with version constants
  - URL-based versioning: /api/v1/...
  - Updated all endpoints to use versioned paths
  - RateLimitingFilter support for versioned endpoints
- **OpenAPI Configuration**
  - OpenApiConfig.java with JWT Bearer authentication
  - API documentation with versioning info
  - Server URLs for different environments
- **Documentation**
  - docs/VAULT_SETUP.md - Complete Vault integration guide
  - Installation instructions for macOS/Linux/Windows
  - Development and Production setup guides
  - Secret structure and management
  - Authentication methods comparison
  - Troubleshooting section

### Changed

- application.yml now supports optional Vault import
- Database and JWT properties support Vault loading
- Property loading priority: Vault → Environment → Defaults
- SecurityConfig uses ApiVersion constants
- AuthController uses versioned endpoints (/api/v1/auth)

### Security

- Centralized secret management via Vault
- Dynamic token generation with policies
- Audit logging for secret access
- Secret versioning and rollback support
- Separation of dev/prod secrets

---

## [0.3.0] - 2025-01-02

### Added

- **JWT Authentication System**

  - Access tokens (15 min) and Refresh tokens (7 days)
  - HS256 signature algorithm with configurable secret
  - JwtTokenProvider for token generation/validation
  - JwtAuthenticationFilter for request processing
  - JwtAuthenticationEntryPoint for error handling

- **User Management**

  - User entity with JPA mapping
  - Role enum (USER, ORGANIZER, ADMIN)
  - UserRepository with custom queries
  - CustomUserDetailsService for Spring Security integration

- **Authentication API**

  - POST /api/auth/register - User registration
  - POST /api/auth/login - User login
  - POST /api/auth/refresh - Token refresh
  - GET /api/auth/me - Current user info
  - AuthController with Swagger documentation
  - AuthService with business logic

- **Rate Limiting (Bucket4j)**

  - Token Bucket algorithm implementation
  - Different limits for AUTH (10/min), API (60/min), GENERAL (100/min)
  - RateLimitingFilter with IP-based tracking
  - X-RateLimit-\* response headers

- **Security Configuration**

  - Production config with JWT authentication
  - Development config with open endpoints
  - Profile-based configuration (dev/prod)
  - CORS configuration for frontend
  - BCrypt password encoder (strength 12)

- **Exception Handling**

  - GlobalExceptionHandler for consistent error responses
  - AuthException for authentication errors
  - Validation error handling

- **Database Migrations**

  - V2\_\_create_users_table.sql - Users and roles tables
  - Default admin user (admin / Admin123!)

- **Configuration**
  - application-dev.yml for development
  - application-prod.yml for production
  - JWT properties with environment variables

### Security

- Stateless session management
- CSRF disabled for REST API
- Password hashing with BCrypt
- Rate limiting for brute-force protection

### Documentation

- docs/SECURITY_SETUP.md - Complete security documentation
- Updated README with security section

---

## [0.2.1] - 2024-12-22

### Added

- **Flyway Database Migrations**
  - Added Flyway Core and PostgreSQL dependencies
  - Configured Flyway in application.yml
  - Created migration directory structure (`src/main/resources/db/migration/`)
  - Added initial migration file `V1__init.sql` (placeholder)
  - Updated README with Flyway documentation and usage guide

### Changed

- Updated `application.yml` with Flyway configuration
- Updated project structure documentation to include migration directory

### Technical Details

- Flyway enabled with baseline-on-migrate for existing databases
- Migration validation enabled for safety
- Clean command disabled for production safety

---

## [0.2.0] - 2024-12-22

### Added

- **QA Testing Framework** (AI-132)
  - Created `qa-team/` directory structure for test documentation
  - Added automated test suite with 22 test cases
  - Implemented `automated-test.sh` script for CI/CD integration
  - Created comprehensive testing guides and templates
  - Added test evidence collection (JSON responses, logs, etc.)
  - Documented testing process for future tickets

### Changed

- Fixed line endings in secret management documentation files

### Testing

- ✅ All 22 automated tests PASS
- ✅ Manual verification of application startup
- ✅ Health check endpoint verified
- ✅ Swagger UI accessibility confirmed
- ✅ Security configuration tested (dev mode)

### Commits

- `b2d5363` - feat: Add QA testing documentation and automation for AI-132
- `4f4bf3a` - docs: Fix line endings in secret management documentation

---

## [0.1.0] - 2024-12-19

### Added

- **Initial Project Setup** (AI-132)
  - Spring Boot 3.5.9 project bootstrap
  - Java 17 and Maven configuration
  - PostgreSQL database integration (without Docker)
  - JPA/Hibernate setup with validation mode
  - Application configuration via environment variables
  - Swagger/OpenAPI documentation (SpringDoc 2.3.0)
  - Basic Spring Security configuration (development mode)
- **Infrastructure**
  - Git repository initialization
  - Git-crypt for secrets encryption
  - Comprehensive `.gitignore` file
  - Environment variables setup (`.env` / `.env.example`)
- **Documentation**
  - Project README with setup instructions
  - Git-crypt setup guide
  - Secrets management comparison
  - SOPS alternative documentation

### Security

- Implemented Git-crypt for `.env` file encryption
- Configured Spring Security with all endpoints open (dev mode)
- CSRF protection disabled for development

### Commits

- `69d9dff` - Add Spring Security configuration for development mode
- `f27e6d4` - Update security section to recommend Git-crypt
- `cb5eb33` - Initial project setup with git-crypt encryption
- `7f4917e` - Setup git-crypt for secrets encryption

---

## Version Guidelines

### Version Format: MAJOR.MINOR.PATCH

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality (backwards-compatible)
- **PATCH**: Bug fixes (backwards-compatible)

### Current Status

- **Latest Version**: 0.2.0
- **Status**: In Development
- **Next Milestone**: 0.3.0 (Tournament Domain Model)

---

[Unreleased]: https://github.com/NikolaSalta/tournament-service-be/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/NikolaSalta/tournament-service-be/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/NikolaSalta/tournament-service-be/releases/tag/v0.1.0
