# Tournament Management MVP (AI-125)

A lightweight REST API for managing chess tournaments with player registration, game/pairing creation, result tracking, and standings calculation.

## Features

✅ **Player Registration Workflow** - Register players to tournaments  
✅ **Manual Game/Pairing Creation** - Create game pairings between players  
✅ **Manual Result Input** - Record game results (1=win, 0.5=draw, 0=loss)  
✅ **Multiple Winners Support** - Track multiple tournament winners  
✅ **Standings & Tournament Completion** - Calculate standings and mark tournaments complete  

## Installation

```bash
npm install
```

## Running the Server

```bash
npm start
```

The server will run on `http://localhost:3000` (or the port specified in the `PORT` environment variable).

## Running Tests

```bash
npm test
```

## API Endpoints

### Player Management

#### Register a Player
```http
POST /api/tournaments/{id}/players
Content-Type: application/json

{
  "playerId": "player1",
  "playerName": "Alice"
}
```

**Response:** `201 Created`
```json
{
  "message": "Player registered successfully",
  "player": {
    "id": "player1",
    "name": "Alice",
    "tournamentId": "tournament1"
  }
}
```

#### Get All Players
```http
GET /api/tournaments/{id}/players
```

**Response:** `200 OK`
```json
{
  "players": [
    {
      "id": "player1",
      "name": "Alice",
      "tournamentId": "tournament1"
    }
  ]
}
```

### Game Management

#### Create a Game
```http
POST /api/tournaments/{id}/games
Content-Type: application/json

{
  "gameId": "game1",
  "player1Id": "player1",
  "player2Id": "player2"
}
```

**Response:** `201 Created`
```json
{
  "message": "Game created successfully",
  "game": {
    "id": "game1",
    "player1Id": "player1",
    "player2Id": "player2",
    "result": null
  }
}
```

#### Update Game Result
```http
PATCH /api/tournaments/{id}/games
Content-Type: application/json

{
  "gameId": "game1",
  "result": 1
}
```

Valid result values:
- `1` - Player 1 wins
- `0.5` - Draw
- `0` - Player 2 wins

**Response:** `200 OK`
```json
{
  "message": "Game result updated successfully",
  "game": {
    "id": "game1",
    "player1Id": "player1",
    "player2Id": "player2",
    "result": 1
  }
}
```

#### Get All Games
```http
GET /api/tournaments/{id}/games
```

**Response:** `200 OK`
```json
{
  "games": [
    {
      "id": "game1",
      "player1Id": "player1",
      "player2Id": "player2",
      "result": 1
    }
  ]
}
```

### Winners Management

#### Add a Winner
```http
POST /api/tournaments/{id}/games/winners
Content-Type: application/json

{
  "playerId": "player1"
}
```

**Response:** `201 Created`
```json
{
  "message": "Winner added successfully",
  "winners": ["player1"]
}
```

#### Get All Winners
```http
GET /api/tournaments/{id}/games/winners
```

**Response:** `200 OK`
```json
{
  "winners": [
    {
      "id": "player1",
      "name": "Alice"
    }
  ]
}
```

### Standings & Completion

#### Get Tournament Standings
```http
GET /api/tournaments/{id}/standings
```

**Response:** `200 OK`
```json
{
  "standings": [
    {
      "playerId": "player1",
      "playerName": "Alice",
      "wins": 2,
      "draws": 1,
      "losses": 0,
      "points": 2.5
    }
  ]
}
```

#### Complete Tournament
```http
POST /api/tournaments/{id}/complete
```

**Response:** `200 OK`
```json
{
  "message": "Tournament completed successfully",
  "isCompleted": true
}
```

## Data Model

### Tournament
- `id`: Unique identifier
- `name`: Tournament name
- `players`: Array of registered players
- `games`: Array of games/pairings
- `winners`: Array of winner player IDs
- `isCompleted`: Boolean flag

### Player
- `id`: Unique identifier
- `name`: Player name
- `tournamentId`: Associated tournament ID

### Game
- `id`: Unique identifier
- `player1Id`: First player ID
- `player2Id`: Second player ID
- `tournamentId`: Associated tournament ID
- `result`: Game result (1, 0.5, 0, or null if not played)

## Storage

This MVP uses in-memory storage. Data will be lost when the server restarts. For production use, implement persistent storage (database).

## License

ISC

