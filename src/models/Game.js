class Game {
  constructor(id, player1Id, player2Id, tournamentId) {
    this.id = id;
    this.player1Id = player1Id;
    this.player2Id = player2Id;
    this.tournamentId = tournamentId;
    this.result = null; // null = not played, 1 = player1 wins, 0.5 = draw, 0 = player2 wins
  }

  setResult(result) {
    if (result === 1 || result === 0.5 || result === 0) {
      this.result = result;
      return true;
    }
    return false;
  }
}

module.exports = Game;
