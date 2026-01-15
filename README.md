# Tournament Management MVP (AI-125)

A minimal viable product for managing tournaments, including player registration, match pairing, and standings calculation.

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA / Hibernate**
- **PostgreSQL** (production) / **H2** (development/testing)
- **Maven**

## Features

- CRUD operations for tournaments
- Tournament lifecycle management (DRAFT → REGISTRATION → ONGOING → FINISHED)
- Player registration to tournaments
- Manual match pairing
- Result submission and management
- Standings calculation based on match results

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (optional, H2 is used by default for development)

### Running the Application

1. Clone the repository:
```bash
git clone <repository-url>
cd feature-AI-125-tournament-management-mvp
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### Database Configuration

By default, the application uses an in-memory H2 database. To use PostgreSQL:

1. Create a PostgreSQL database:
```sql
CREATE DATABASE tournament_db;
```

2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tournament_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### H2 Console (Development)

Access the H2 console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:tournamentdb`
- Username: `sa`
- Password: (empty)

## API Documentation

### Tournament Endpoints

#### Create Tournament
```http
POST /api/tournaments
Content-Type: application/json

{
  "name": "Summer Championship 2024",
  "description": "Annual summer chess tournament",
  "maxPlayers": 32
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Summer Championship 2024",
  "description": "Annual summer chess tournament",
  "status": "DRAFT",
  "maxPlayers": 32,
  "playerCount": 0,
  "matchCount": 0,
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

#### Get Tournament by ID
```http
GET /api/tournaments/{id}
```

#### Get All Tournaments
```http
GET /api/tournaments
GET /api/tournaments?status=ONGOING
```

#### Update Tournament
```http
PUT /api/tournaments/{id}
Content-Type: application/json

{
  "name": "Updated Tournament Name",
  "description": "Updated description",
  "maxPlayers": 64
}
```
*Note: Only allowed when tournament is in DRAFT status.*

#### Update Tournament Status
```http
PATCH /api/tournaments/{id}/status?status=REGISTRATION
```

**Valid Status Transitions:**
- DRAFT → REGISTRATION
- REGISTRATION → ONGOING (requires at least 2 players)
- REGISTRATION → DRAFT
- ONGOING → FINISHED

#### Delete Tournament
```http
DELETE /api/tournaments/{id}
```
*Note: Only allowed when tournament is in DRAFT status.*

---

### Player Endpoints

#### Register Player
```http
POST /api/tournaments/{tournamentId}/players
Content-Type: application/json

{
  "name": "Magnus Carlsen",
  "email": "magnus@chess.com"
}
```
*Note: Only allowed when tournament is in REGISTRATION status.*

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Magnus Carlsen",
  "email": "magnus@chess.com",
  "tournamentId": 1,
  "registeredAt": "2024-01-15T11:00:00"
}
```

#### Get Players in Tournament
```http
GET /api/tournaments/{tournamentId}/players
```

#### Get Player by ID
```http
GET /api/tournaments/{tournamentId}/players/{playerId}
```

#### Remove Player
```http
DELETE /api/tournaments/{tournamentId}/players/{playerId}
```
*Note: Only allowed when tournament is in DRAFT or REGISTRATION status.*

---

### Match Endpoints

#### Create Match (Manual Pairing)
```http
POST /api/tournaments/{tournamentId}/matches
Content-Type: application/json

{
  "player1Id": 1,
  "player2Id": 2,
  "round": 1
}
```
*Note: Only allowed when tournament is in ONGOING status.*

**Response (201 Created):**
```json
{
  "id": 1,
  "tournamentId": 1,
  "player1Id": 1,
  "player1Name": "Magnus Carlsen",
  "player2Id": 2,
  "player2Name": "Fabiano Caruana",
  "round": 1,
  "hasResult": false,
  "result": null,
  "createdAt": "2024-01-15T12:00:00"
}
```

#### Get Matches in Tournament
```http
GET /api/tournaments/{tournamentId}/matches
GET /api/tournaments/{tournamentId}/matches?round=1
```

#### Get Match by ID
```http
GET /api/tournaments/{tournamentId}/matches/{matchId}
```

#### Get Rounds in Tournament
```http
GET /api/tournaments/{tournamentId}/matches/rounds
```

#### Delete Match
```http
DELETE /api/tournaments/{tournamentId}/matches/{matchId}
```
*Note: Not allowed if match has a result or tournament is FINISHED.*

---

### Result Endpoints

#### Submit Result
```http
POST /api/matches/{matchId}/result
Content-Type: application/json

{
  "player1Score": 2,
  "player2Score": 1
}
```
*Note: Only allowed when tournament is in ONGOING status.*

**Response (201 Created):**
```json
{
  "id": 1,
  "matchId": 1,
  "player1Score": 2,
  "player2Score": 1,
  "winnerId": 1,
  "winnerName": "Magnus Carlsen",
  "isDraw": false,
  "submittedAt": "2024-01-15T13:00:00"
}
```

#### Update Result
```http
PUT /api/matches/{matchId}/result
Content-Type: application/json

{
  "player1Score": 3,
  "player2Score": 2
}
```
*Note: Not allowed when tournament is FINISHED.*

#### Get Result by Match ID
```http
GET /api/matches/{matchId}/result
```

#### Get All Results in Tournament
```http
GET /api/tournaments/{tournamentId}/results
```

#### Delete Result
```http
DELETE /api/matches/{matchId}/result
```
*Note: Not allowed when tournament is FINISHED.*

---

### Standings Endpoint

#### Get Tournament Standings
```http
GET /api/tournaments/{tournamentId}/standings
```

**Response (200 OK):**
```json
[
  {
    "rank": 1,
    "playerId": 1,
    "playerName": "Magnus Carlsen",
    "wins": 5,
    "draws": 1,
    "losses": 0,
    "points": 16,
    "matchesPlayed": 6
  },
  {
    "rank": 2,
    "playerId": 2,
    "playerName": "Fabiano Caruana",
    "wins": 4,
    "draws": 1,
    "losses": 1,
    "points": 13,
    "matchesPlayed": 6
  }
]
```

**Points System:**
- Win: 3 points
- Draw: 1 point
- Loss: 0 points

---

## Example Workflow

1. **Create a tournament:**
```bash
curl -X POST http://localhost:8080/api/tournaments \
  -H "Content-Type: application/json" \
  -d '{"name": "Chess Tournament", "description": "Local chess event", "maxPlayers": 8}'
```

2. **Open registration:**
```bash
curl -X PATCH "http://localhost:8080/api/tournaments/1/status?status=REGISTRATION"
```

3. **Register players:**
```bash
curl -X POST http://localhost:8080/api/tournaments/1/players \
  -H "Content-Type: application/json" \
  -d '{"name": "Player One", "email": "player1@example.com"}'

curl -X POST http://localhost:8080/api/tournaments/1/players \
  -H "Content-Type: application/json" \
  -d '{"name": "Player Two", "email": "player2@example.com"}'
```

4. **Start the tournament:**
```bash
curl -X PATCH "http://localhost:8080/api/tournaments/1/status?status=ONGOING"
```

5. **Create matches:**
```bash
curl -X POST http://localhost:8080/api/tournaments/1/matches \
  -H "Content-Type: application/json" \
  -d '{"player1Id": 1, "player2Id": 2, "round": 1}'
```

6. **Submit results:**
```bash
curl -X POST http://localhost:8080/api/matches/1/result \
  -H "Content-Type: application/json" \
  -d '{"player1Score": 2, "player2Score": 1}'
```

7. **View standings:**
```bash
curl http://localhost:8080/api/tournaments/1/standings
```

8. **Finish the tournament:**
```bash
curl -X PATCH "http://localhost:8080/api/tournaments/1/status?status=FINISHED"
```

## Running Tests

```bash
mvn test
```

## Project Structure

```
src/
├── main/
│   ├── java/com/tournament/management/
│   │   ├── TournamentManagementApplication.java
│   │   ├── config/
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── controller/
│   │   │   ├── TournamentController.java
│   │   │   ├── PlayerController.java
│   │   │   ├── MatchController.java
│   │   │   ├── ResultController.java
│   │   │   └── StandingsController.java
│   │   ├── dto/
│   │   │   ├── TournamentRequest.java
│   │   │   ├── TournamentResponse.java
│   │   │   ├── PlayerRequest.java
│   │   │   ├── PlayerResponse.java
│   │   │   ├── MatchRequest.java
│   │   │   ├── MatchResponse.java
│   │   │   ├── ResultRequest.java
│   │   │   ├── ResultResponse.java
│   │   │   └── StandingResponse.java
│   │   ├── entity/
│   │   │   ├── Tournament.java
│   │   │   ├── TournamentStatus.java
│   │   │   ├── Player.java
│   │   │   ├── Match.java
│   │   │   └── Result.java
│   │   ├── repository/
│   │   │   ├── TournamentRepository.java
│   │   │   ├── PlayerRepository.java
│   │   │   ├── MatchRepository.java
│   │   │   └── ResultRepository.java
│   │   └── service/
│   │       ├── TournamentService.java
│   │       ├── PlayerService.java
│   │       ├── MatchService.java
│   │       ├── ResultService.java
│   │       └── StandingsService.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/tournament/management/service/
        ├── TournamentServiceTest.java
        ├── PlayerServiceTest.java
        ├── MatchServiceTest.java
        ├── ResultServiceTest.java
        └── StandingsServiceTest.java
```

## Out of Scope

- Authentication/authorization
- Swiss/round-robin automation
- External integrations (Alerts, Video, CV, etc.)

## License

This project is for internal use as part of the AI-125 feature development.
