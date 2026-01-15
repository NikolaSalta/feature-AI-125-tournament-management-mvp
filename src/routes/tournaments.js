const express = require('express');
const router = express.Router();
const TournamentController = require('../controllers/tournamentController');

// Player endpoints
router.post('/:id/players', TournamentController.registerPlayer);
router.get('/:id/players', TournamentController.getPlayers);

// Game endpoints
router.post('/:id/games', TournamentController.createGame);
router.patch('/:id/games', TournamentController.updateGameResult);
router.get('/:id/games', TournamentController.getGames);

// Winners endpoints
router.post('/:id/games/winners', TournamentController.addWinner);
router.get('/:id/games/winners', TournamentController.getWinners);

// Standings and completion
router.get('/:id/standings', TournamentController.getStandings);
router.post('/:id/complete', TournamentController.completeTournament);

module.exports = router;
