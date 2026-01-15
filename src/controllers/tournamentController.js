const Tournament = require('../models/Tournament');
const Player = require('../models/Player');
const Game = require('../models/Game');

// In-memory storage for MVP
const tournaments = new Map();

class TournamentController {
  static createTournament(id, name) {
    const tournament = new Tournament(id, name);
    tournaments.set(id, tournament);
    return tournament;
  }

  static getTournament(id) {
    return tournaments.get(id);
  }

  static registerPlayer(req, res) {
    const { id: tournamentId } = req.params;
    const { playerId, playerName } = req.body;

    if (!playerId || !playerName) {
      return res.status(400).json({ error: 'playerId and playerName are required' });
    }

    let tournament = tournaments.get(tournamentId);
    if (!tournament) {
      tournament = TournamentController.createTournament(tournamentId, `Tournament ${tournamentId}`);
    }

    // Check if player already exists
    if (tournament.getPlayer(playerId)) {
      return res.status(400).json({ error: 'Player already registered' });
    }

    const player = new Player(playerId, playerName, tournamentId);
    tournament.addPlayer(player);

    res.status(201).json({
      message: 'Player registered successfully',
      player: {
        id: player.id,
        name: player.name,
        tournamentId: player.tournamentId
      }
    });
  }

  static getPlayers(req, res) {
    const { id: tournamentId } = req.params;
    const tournament = tournaments.get(tournamentId);

    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    res.json({
      players: tournament.players.map(p => ({
        id: p.id,
        name: p.name,
        tournamentId: p.tournamentId
      }))
    });
  }

  static createGame(req, res) {
    const { id: tournamentId } = req.params;
    const { gameId, player1Id, player2Id } = req.body;

    if (!gameId || !player1Id || !player2Id) {
      return res.status(400).json({ error: 'gameId, player1Id, and player2Id are required' });
    }

    const tournament = tournaments.get(tournamentId);
    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    // Validate players exist
    if (!tournament.getPlayer(player1Id) || !tournament.getPlayer(player2Id)) {
      return res.status(400).json({ error: 'One or both players not found in tournament' });
    }

    // Check if game already exists
    if (tournament.getGame(gameId)) {
      return res.status(400).json({ error: 'Game already exists' });
    }

    const game = new Game(gameId, player1Id, player2Id, tournamentId);
    tournament.addGame(game);

    res.status(201).json({
      message: 'Game created successfully',
      game: {
        id: game.id,
        player1Id: game.player1Id,
        player2Id: game.player2Id,
        result: game.result
      }
    });
  }

  static updateGameResult(req, res) {
    const { id: tournamentId } = req.params;
    const { gameId, result } = req.body;

    if (!gameId || result === undefined || result === null) {
      return res.status(400).json({ error: 'gameId and result are required' });
    }

    if (result !== 1 && result !== 0.5 && result !== 0) {
      return res.status(400).json({ error: 'result must be 1 (win), 0.5 (draw), or 0 (loss)' });
    }

    const tournament = tournaments.get(tournamentId);
    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    const game = tournament.getGame(gameId);
    if (!game) {
      return res.status(404).json({ error: 'Game not found' });
    }

    game.setResult(result);

    res.json({
      message: 'Game result updated successfully',
      game: {
        id: game.id,
        player1Id: game.player1Id,
        player2Id: game.player2Id,
        result: game.result
      }
    });
  }

  static getGames(req, res) {
    const { id: tournamentId } = req.params;
    const tournament = tournaments.get(tournamentId);

    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    res.json({
      games: tournament.games.map(g => ({
        id: g.id,
        player1Id: g.player1Id,
        player2Id: g.player2Id,
        result: g.result
      }))
    });
  }

  static addWinner(req, res) {
    const { id: tournamentId } = req.params;
    const { playerId } = req.body;

    if (!playerId) {
      return res.status(400).json({ error: 'playerId is required' });
    }

    const tournament = tournaments.get(tournamentId);
    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    if (!tournament.getPlayer(playerId)) {
      return res.status(400).json({ error: 'Player not found in tournament' });
    }

    tournament.addWinner(playerId);

    res.status(201).json({
      message: 'Winner added successfully',
      winners: tournament.winners
    });
  }

  static getWinners(req, res) {
    const { id: tournamentId } = req.params;
    const tournament = tournaments.get(tournamentId);

    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    const winners = tournament.winners.map(winnerId => {
      const player = tournament.getPlayer(winnerId);
      return player ? { id: player.id, name: player.name } : null;
    }).filter(w => w !== null);

    res.json({ winners });
  }

  static getStandings(req, res) {
    const { id: tournamentId } = req.params;
    const tournament = tournaments.get(tournamentId);

    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    const standings = tournament.calculateStandings();
    res.json({ standings });
  }

  static completeTournament(req, res) {
    const { id: tournamentId } = req.params;
    const tournament = tournaments.get(tournamentId);

    if (!tournament) {
      return res.status(404).json({ error: 'Tournament not found' });
    }

    tournament.complete();

    res.json({
      message: 'Tournament completed successfully',
      isCompleted: tournament.isCompleted
    });
  }
}

module.exports = TournamentController;
