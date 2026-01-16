# System Map — Tournament Service Backend

## Service Overview
- **Name**: tournament-service-be
- **Port**: 8080
- **Profiles**: dev / prod / test / vault
- **Database**: PostgreSQL (port 5432)
- **Cache**: Caffeine (in-memory)
- **Authentication**: JWT-based
- **API Version**: v1
- **Framework**: Spring Boot 3.3.6 + Spring Cloud 2023.0.5

---

## 🏗 Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Client    │    │  Mobile App     │    │   Web Client    │
│                 │    │                 │    │                 │
│      JWT        │    │      JWT        │    │      JWT        │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                Tournament Service Backend                        │
├─────────────────────────────────────────────────────────────────┤
│  Controllers → Services → Repositories → PostgreSQL            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 Entities & Database Schema

| Entity | Table | Description | Key Relationships |
|--------|-------|-------------|-------------------|
| **Tournament** | `tournaments` | Шахматный турнир | 1:N → TournamentPlayer, Game, TournamentWinner |
| **TournamentPlayer** | `tournament_players` | Участник турнира | N:1 → Tournament, User |
| **Game** | `games` | Партия/матч | N:1 → Tournament, TournamentPlayer (white/black) |
| **TournamentWinner** | `tournament_winners` | Победитель (multiple) | N:1 → Tournament, TournamentPlayer |
| **User** | `users` | Пользователь системы | 1:N → TournamentPlayer |

### Entity Status Enums:
- **TournamentStatus**: `DRAFT`, `REGISTRATION_OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
- **TournamentFormat**: `SWISS`, `ROUND_ROBIN`, `KNOCKOUT`, `ARENA`
- **PlayerStatus**: `REGISTERED`, `ACTIVE`, `WITHDRAWN`, `DISQUALIFIED`
- **GameStatus**: `SCHEDULED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`
- **Role**: `USER`, `ORGANIZER`, `ADMIN`

---

## 🛣 API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/register` | Регистрация пользователя | ❌ |
| POST | `/login` | Вход в систему | ❌ |
| POST | `/refresh` | Обновление токена | ❌ |
| GET | `/me` | Профиль пользователя | ✅ JWT |
| GET | `/validate` | Валидация токена | ✅ JWT |

### Tournaments (`/api/v1/tournaments`)
| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/` | Список турниров | ❌ (public) |
| POST | `/` | Создание турнира | ✅ ORGANIZER+ |
| GET | `/{id}` | Детали турнира | ❌ (public) |
| PUT | `/{id}` | Обновление турнира | ✅ ORGANIZER |
| DELETE | `/{id}` | Удаление турнира | ✅ ORGANIZER |

### Tournament Players (`/api/v1/tournaments/{id}/players`)
| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/` | Список участников | ❌ (public) |
| POST | `/` | Регистрация в турнир | ✅ USER+ |
| GET | `/{playerId}` | Детали участника | ❌ (public) |
| DELETE | `/{playerId}` | Снятие с турнира | ✅ USER (self) / ORGANIZER |
| PATCH | `/{playerId}/disqualify` | Дисквалификация | ✅ ORGANIZER |

### Games (`/api/v1/tournaments/{id}/games`)
| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/` | Список партий | ❌ (public) |
| POST | `/` | Создание партии | ✅ ORGANIZER |
| PATCH | `/{gameId}/result` | Ввод результата | ✅ ORGANIZER |
| GET | `/standings` | Турнирная таблица | ❌ (public) |
| POST | `/complete` | Завершение турнира | ✅ ORGANIZER |
| POST | `/winners` | Добавление победителя | ✅ ORGANIZER |
| POST | `/winners/all` | Все как победители | ✅ ORGANIZER |
| GET | `/winners` | Список победителей | ❌ (public) |
| DELETE | `/winners/{winnerId}` | Удаление победителя | ✅ ORGANIZER |

---

## 🔧 Technical Dependencies

### Runtime Dependencies
- **PostgreSQL 16+** (required, port 5432)
- **Flyway** (database migrations)
- **JWT** (authentication tokens)
- **Caffeine** (in-memory caching)
- **Bucket4j** (rate limiting)

### Optional Dependencies
- **HashiCorp Vault** (secrets management, port 8200)
- **Docker** (containerization)
- **Kubernetes** (orchestration)

### Development Dependencies
- **H2 Database** (in-memory testing)
- **JUnit 5** (testing framework)
- **Mockito** (mocking)
- **AssertJ** (assertions)
- **Spring Boot Test** (integration testing)

---

## 🏷 Tier Classification

| Tier | Service | Criticality | Dependencies |
|------|---------|-------------|--------------|
| **Tier-1** | Tournament Management | CRITICAL | PostgreSQL, JWT |
| **Tier-2** | User Management | HIGH | PostgreSQL |
| **Tier-3** | Authentication | HIGH | JWT, Rate Limiting |
| **Tier-4** | Documentation | MEDIUM | Swagger UI |
| **Tier-5** | Monitoring | LOW | Actuator |

---

## 📁 Project Structure

```
src/main/java/com/chessai/tournament/
├── config/                    # Spring configurations
│   ├── ApiVersion.java        # API versioning
│   ├── OpenApiConfig.java     # Swagger/OpenAPI setup
│   ├── SecurityConfig.java    # Spring Security + JWT
│   └── VaultConfig.java       # HashiCorp Vault integration
├── controller/                # REST API controllers
│   ├── AuthController.java    # Authentication endpoints
│   ├── GameController.java    # Game management
│   ├── TournamentController.java
│   └── TournamentPlayerController.java
├── dto/                       # Data Transfer Objects
│   ├── auth/                  # Authentication DTOs
│   └── *.java                 # Request/Response DTOs
├── entity/                    # JPA entities
│   ├── Tournament.java        # Core tournament entity
│   ├── TournamentPlayer.java  # Tournament participants
│   ├── Game.java              # Chess games/matches
│   ├── TournamentWinner.java  # Tournament winners
│   ├── User.java              # System users
│   └── *.java                 # Enums (Status, Format, Role)
├── exception/                 # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   └── *.java                 # Domain exceptions
├── repository/                # JPA repositories
│   └── *.java                 # Data access layer
├── security/                  # Security components
│   ├── jwt/                   # JWT implementation
│   ├── ratelimit/             # Rate limiting
│   └── service/               # Security services
├── service/                   # Business logic
│   ├── GameService.java       # Game management
│   ├── TournamentService.java # Tournament management
│   └── TournamentPlayerService.java
└── TournamentServiceBeApplication.java
```

---

## 🔍 Key Configuration Files

| File | Purpose | Environment |
|------|---------|-------------|
| `application.yml` | Base configuration | All |
| `application-dev.yml` | Development overrides | dev |
| `application-prod.yml` | Production overrides | prod |
| `application-vault.yml` | Vault integration | vault |
| `bootstrap.yml` | Vault bootstrap | vault |
| `docker-compose.yml` | Local infrastructure | dev |
| `Dockerfile` | Container build | prod |

---

## 🚀 Deployment Targets

| Environment | Configuration | Database | Secrets |
|-------------|---------------|----------|---------|
| **Development** | dev profile | Docker PostgreSQL | .env files |
| **Testing** | test profile | H2 in-memory | Hardcoded |
| **Staging** | prod profile | External PostgreSQL | Vault |
| **Production** | prod profile | External PostgreSQL | Vault |

---

*Generated by Internal Audit System*  
*Date: 2026-01-16*