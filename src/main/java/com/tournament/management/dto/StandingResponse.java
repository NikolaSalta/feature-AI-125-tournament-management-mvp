package com.tournament.management.dto;

/**
 * DTO representing a player's standing in a tournament.
 */
public class StandingResponse {

    private int rank;
    private Long playerId;
    private String playerName;
    private int wins;
    private int draws;
    private int losses;
    private int points;
    private int matchesPlayed;

    public StandingResponse() {
    }

    public StandingResponse(Long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.points = 0;
        this.matchesPlayed = 0;
    }

    public void addWin() {
        this.wins++;
        this.points += 3; // 3 points for a win
        this.matchesPlayed++;
    }

    public void addDraw() {
        this.draws++;
        this.points += 1; // 1 point for a draw
        this.matchesPlayed++;
    }

    public void addLoss() {
        this.losses++;
        // 0 points for a loss
        this.matchesPlayed++;
    }

    // Getters and Setters
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }
}
