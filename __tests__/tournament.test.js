const request = require('supertest');
const app = require('../src/app');

describe('Tournament Management API', () => {
  const tournamentId = 'test-tournament-1';
  
  describe('Player Registration', () => {
    it('should register a player successfully', async () => {
      const response = await request(app)
        .post(`/api/tournaments/${tournamentId}/players`)
        .send({
          playerId: 'player1',
          playerName: 'Alice'
        });

      expect(response.status).toBe(201);
      expect(response.body.player.id).toBe('player1');
      expect(response.body.player.name).toBe('Alice');
    });

    it('should return 400 if player already registered', async () => {
      await request(app)
        .post(`/api/tournaments/${tournamentId}/players`)
        .send({
          playerId: 'player2',
          playerName: 'Bob'
        });

      const response = await request(app)
        .post(`/api/tournaments/${tournamentId}/players`)
        .send({
          playerId: 'player2',
          playerName: 'Bob Duplicate'
        });

      expect(response.status).toBe(400);
      expect(response.body.error).toBe('Player already registered');
    });

    it('should get all players in a tournament', async () => {
      const response = await request(app)
        .get(`/api/tournaments/${tournamentId}/players`);

      expect(response.status).toBe(200);
      expect(response.body.players).toBeDefined();
      expect(response.body.players.length).toBeGreaterThan(0);
    });
  });

  describe('Game Creation and Results', () => {
    beforeAll(async () => {
      // Register players for game tests
      await request(app)
        .post(`/api/tournaments/${tournamentId}/players`)
        .send({ playerId: 'player3', playerName: 'Charlie' });
      
      await request(app)
        .post(`/api/tournaments/${tournamentId}/players`)
        .send({ playerId: 'player4', playerName: 'David' });
    });

    it('should create a game successfully', async () => {
      const response = await request(app)
        .post(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game1',
          player1Id: 'player3',
          player2Id: 'player4'
        });

      expect(response.status).toBe(201);
      expect(response.body.game.id).toBe('game1');
      expect(response.body.game.player1Id).toBe('player3');
      expect(response.body.game.player2Id).toBe('player4');
    });

    it('should update game result with win (1)', async () => {
      const response = await request(app)
        .patch(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game1',
          result: 1
        });

      expect(response.status).toBe(200);
      expect(response.body.game.result).toBe(1);
    });

    it('should update game result with draw (0.5)', async () => {
      await request(app)
        .post(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game2',
          player1Id: 'player3',
          player2Id: 'player4'
        });

      const response = await request(app)
        .patch(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game2',
          result: 0.5
        });

      expect(response.status).toBe(200);
      expect(response.body.game.result).toBe(0.5);
    });

    it('should update game result with loss (0)', async () => {
      await request(app)
        .post(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game3',
          player1Id: 'player3',
          player2Id: 'player4'
        });

      const response = await request(app)
        .patch(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game3',
          result: 0
        });

      expect(response.status).toBe(200);
      expect(response.body.game.result).toBe(0);
    });

    it('should return 400 for invalid result', async () => {
      const response = await request(app)
        .patch(`/api/tournaments/${tournamentId}/games`)
        .send({
          gameId: 'game1',
          result: 2
        });

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('result must be');
    });

    it('should get all games in a tournament', async () => {
      const response = await request(app)
        .get(`/api/tournaments/${tournamentId}/games`);

      expect(response.status).toBe(200);
      expect(response.body.games).toBeDefined();
      expect(response.body.games.length).toBeGreaterThan(0);
    });
  });

  describe('Winners Management', () => {
    it('should add a winner successfully', async () => {
      const response = await request(app)
        .post(`/api/tournaments/${tournamentId}/games/winners`)
        .send({
          playerId: 'player3'
        });

      expect(response.status).toBe(201);
      expect(response.body.winners).toContain('player3');
    });

    it('should get all winners', async () => {
      const response = await request(app)
        .get(`/api/tournaments/${tournamentId}/games/winners`);

      expect(response.status).toBe(200);
      expect(response.body.winners).toBeDefined();
      expect(response.body.winners.length).toBeGreaterThan(0);
    });

    it('should support multiple winners', async () => {
      await request(app)
        .post(`/api/tournaments/${tournamentId}/games/winners`)
        .send({ playerId: 'player4' });

      const response = await request(app)
        .get(`/api/tournaments/${tournamentId}/games/winners`);

      expect(response.status).toBe(200);
      expect(response.body.winners.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Standings and Tournament Completion', () => {
    it('should calculate standings correctly', async () => {
      const response = await request(app)
        .get(`/api/tournaments/${tournamentId}/standings`);

      expect(response.status).toBe(200);
      expect(response.body.standings).toBeDefined();
      expect(Array.isArray(response.body.standings)).toBe(true);
      
      // Check standings structure
      if (response.body.standings.length > 0) {
        const standing = response.body.standings[0];
        expect(standing).toHaveProperty('playerId');
        expect(standing).toHaveProperty('playerName');
        expect(standing).toHaveProperty('wins');
        expect(standing).toHaveProperty('draws');
        expect(standing).toHaveProperty('losses');
        expect(standing).toHaveProperty('points');
      }
    });

    it('should complete tournament', async () => {
      const response = await request(app)
        .post(`/api/tournaments/${tournamentId}/complete`);

      expect(response.status).toBe(200);
      expect(response.body.isCompleted).toBe(true);
    });
  });
});
