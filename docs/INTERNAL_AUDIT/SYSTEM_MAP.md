# System Map — Tournament Service Backend

## Date: 2026-01-15
## Auditor: Cursor AI (Principal Backend Engineer + SRE + Security Reviewer)

---

## Service Overview

| Property | Value |
|----------|-------|
| **Name** | tournament-service-be |
| **Port** | 8080 |
| **Profiles** | dev / prod / vault |
| **Database** | PostgreSQL (port 5432) |
| **Cache** | Caffeine (in-memory) |
| **Framework** | Spring Boot 3.5.9 |
| **Java Version** | 17 |
| **Build Tool** | Gradle |
| **Migration** | Flyway |
| **Auth** | JWT (Spring Security) |
| **API Docs** | OpenAPI 3.0 / Swagger UI |

---

## Architecture

```
src/main/java/com/chessai/tournament/
├── config/                 # Spring configurations
│   ├── ApiVersion.java     # API versioning constants
│   ├── OpenApiConfig.java  # Swagger/OpenAPI config
│   ├── SecurityConfig.java # Spring Security config
│   └── VaultConfig.java    # HashiCorp Vault integration
│
├── controller/             # REST API controllers
│   ├── AuthController.java           # /api/v1/auth/*
│   ├── GameController.java           # /api/tournaments/{id}/games/*
│   ├── TournamentController.java     # /api/tournaments/*
│   └── TournamentPlayerController.java # /api/tournaments/{id}/players/*
│
├── service/                # Business logic
│   ├── GameService.java
│   ├── TournamentPlayerService.java
│   └── TournamentService.java
│
├── repository/             # JPA repositories
│   ├── GameRepository.java
│   ├── TournamentPlayerRepository.java
│   ├── TournamentRepository.java
│   ├── TournamentWinnerRepository.java
│   └── UserRepository.java
│
├── entity/                 # JPA entities
│   ├── Game.java
│   ├── Role.java (enum)
│   ├── Tournament.java
│   ├── TournamentFormat.java (enum)
│   ├── TournamentPlayer.java
│   ├── TournamentStatus.java (enum)
│   ├── TournamentWinner.java
│   └── User.java
│
├── dto/                    # Request/Response DTOs
│   ├── auth/               # Auth DTOs
│   ├── GameRequest.java
│   ├── GameResponse.java
│   ├── GameResultRequest.java
│   ├── StandingsResponse.java
│   ├── TournamentPlayerRequest.java
│   ├── TournamentPlayerResponse.java
│   ├── TournamentRequest.java
│   ├── TournamentResponse.java
│   ├── WinnerRequest.java
│   └── WinnerResponse.java
│
├── security/               # Security components
│   ├── jwt/                # JWT authentication
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   └── RefreshTokenService.java
│   ├── ratelimit/          # Rate limiting
│   │   ├── RateLimitingFilter.java
│   │   └── RateLimitingService.java
│   └── service/            # Auth services
│       ├── AuthService.java
│       └── CustomUserDetailsService.java
│
├── exception/              # Custom exceptions
│   ├── AuthException.java
│   ├── GlobalExceptionHandler.java
│   ├── TournamentNotEditableException.java
│   └── TournamentNotFoundException.java
│
└── TournamentServiceBeApplication.java  # Main class
```

---

## Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `Tournament` | `tournaments` | Шахматный турнир с lifecycle (DRAFT → REGISTRATION_OPEN → IN_PROGRESS → COMPLETED) |
| `TournamentPlayer` | `tournament_players` | Участник турнира с очками и статусом |
| `Game` | `games` | Партия/матч между двумя игроками |
| `TournamentWinner` | `tournament_winners` | Победитель турнира (поддержка нескольких) |
| `User` | `users` | Пользователь системы (auth) |

---

## API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/register` | Регистрация нового пользователя | ❌ |
| POST | `/login` | Вход в систему | ❌ |
| POST | `/refresh` | Обновление токенов | ❌ |
| GET | `/validate` | Валидация токена | ❌ |
| GET | `/me` | Текущий пользователь | ✅ |
| POST | `/logout` | Выход из системы | ✅ |

### Tournaments (`/api/tournaments`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Список турниров | ✅ |
| POST | `/` | Создать турнир | ✅ |
| GET | `/{id}` | Получить турнир | ✅ |
| PUT | `/{id}` | Обновить турнир | ✅ (organizer) |
| DELETE | `/{id}` | Удалить турнир | ✅ (organizer) |
| PATCH | `/{id}/status` | Изменить статус | ✅ (organizer) |

### Tournament Players (`/api/tournaments/{id}/players`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Список участников | ✅ |
| POST | `/` | Зарегистрировать участника | ✅ |
| GET | `/{playerId}` | Получить участника | ✅ |
| DELETE | `/{playerId}` | Снять участника | ✅ |
| PATCH | `/{playerId}/disqualify` | Дисквалифицировать | ✅ (organizer) |

### Games (`/api/tournaments/{id}/games`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Список партий | ✅ |
| POST | `/` | Создать партию | ✅ (organizer) |
| DELETE | `/{gameId}` | Удалить партию | ✅ (organizer) |
| PATCH | `/{gameId}/result` | Установить результат | ✅ (organizer) |
| GET | `/standings` | Турнирная таблица | ✅ |
| POST | `/complete` | Завершить турнир | ✅ (organizer) |
| GET | `/winners` | Список победителей | ✅ |
| POST | `/winners` | Добавить победителя | ✅ (organizer) |
| POST | `/winners/all` | Все участники - победители | ✅ (organizer) |
| DELETE | `/winners/{winnerId}` | Удалить победителя | ✅ (organizer) |

---

## Database Migrations (Flyway)

| Version | Description |
|---------|-------------|
| V1 | Initial schema setup |
| V2 | Create users table |
| V3 | Create tournaments table |
| V4 | Add version to tournaments (optimistic locking) |
| V5 | Add composite indexes |
| V6 | Create tournament_players table |
| V7 | Create games table |
| V8 | Add winner_id to tournaments |
| V9 | Create tournament_winners table |

---

## Dependencies

| Dependency | Purpose | Required |
|------------|---------|----------|
| PostgreSQL 15+ | Primary database | ✅ Required |
| Flyway | Database migrations | ✅ Required |
| JWT | Authentication | ✅ Required |
| Caffeine | In-memory cache | ✅ Built-in |
| HashiCorp Vault | Secrets management | ⚠️ Optional (prod) |

---

## Configuration Files

| File | Purpose |
|------|---------|
| `application.yml` | Base configuration |
| `application-dev.yml` | Development profile |
| `application-prod.yml` | Production profile |
| `application-vault.yml` | Vault integration |
| `bootstrap.yml` | Bootstrap config (Vault) |

---

## Infrastructure

| Component | File |
|-----------|------|
| Docker | `Dockerfile` |
| Docker Compose | `docker-compose.yml` |
| Kubernetes | `k8s/*.yaml` |

---

## Test Structure

```
src/test/java/com/chessai/tournament/
├── config/                           # Test configurations
│   ├── TestSecurityConfig.java
│   └── WebMvcTestSecurityConfig.java
├── controller/                       # Integration tests
│   ├── AuthControllerIntegrationTest.java
│   ├── GameControllerIntegrationTest.java
│   └── TournamentPlayerControllerIntegrationTest.java
├── entity/                           # Validation tests
│   └── EntityValidationTest.java
├── repository/                       # Repository tests
│   ├── GameRepositoryTest.java
│   ├── TournamentPlayerRepositoryTest.java
│   └── TournamentRepositoryTest.java
├── security/                         # Security tests
│   ├── jwt/JwtTokenProviderTest.java
│   └── service/AuthServiceTest.java
├── service/                          # Unit tests
│   ├── GameServiceTest.java
│   ├── TournamentPlayerServiceTest.java
│   └── TournamentServiceTest.java
└── TournamentServiceBeApplicationTests.java
```

---

## Tier Classification

- **Tier-1**: Core tournament management (critical path)
- **SLA**: 99.9% availability (target)
- **RTO**: < 1 hour
- **RPO**: < 5 minutes

---

## Security Features

- JWT-based authentication
- Role-based access control (USER, ORGANIZER, ADMIN)
- Rate limiting (Caffeine-based)
- CORS configuration
- Password encryption (BCrypt)
- Refresh token rotation

---

## Monitoring Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Metrics (admin only) |
| `/swagger-ui.html` | API documentation |
| `/v3/api-docs` | OpenAPI spec |
