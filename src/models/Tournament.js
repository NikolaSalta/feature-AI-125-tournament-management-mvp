class Tournament {
  constructor(id, name) {
    this.id = id;
    this.name = name;
    this.players = [];
    this.games = [];
    this.winners = [];
    this.isCompleted = false;
  }

  addPlayer(player) {
    this.players.push(player);
  }

  getPlayer(playerId) {
    return this.players.find(p => p.id === playerId);
  }

  addGame(game) {
    this.games.push(game);
  }

  getGame(gameId) {
    return this.games.find(g => g.id === gameId);
  }

  addWinner(playerId) {
    if (!this.winners.includes(playerId)) {
      this.winners.push(playerId);
    }
  }

  calculateStandings() {
    const standings = {};
    
    this.players.forEach(player => {
      standings[player.id] = {
        playerId: player.id,
        playerName: player.name,
        wins: 0,
        draws: 0,
        losses: 0,
        points: 0
      };
    });

    this.games.forEach(game => {
      if (game.result !== null && game.result !== undefined) {
        const player1 = standings[game.player1Id];
        const player2 = standings[game.player2Id];

        if (player1 && player2) {
          if (game.result === 1) {
            player1.wins++;
            player1.points += 1;
            player2.losses++;
          } else if (game.result === 0.5) {
            player1.draws++;
            player1.points += 0.5;
            player2.draws++;
            player2.points += 0.5;
          } else if (game.result === 0) {
            player1.losses++;
            player2.wins++;
            player2.points += 1;
          }
        }
      }
    });

    return Object.values(standings).sort((a, b) => b.points - a.points);
  }

  complete() {
    this.isCompleted = true;
  }
}

module.exports = Tournament;
