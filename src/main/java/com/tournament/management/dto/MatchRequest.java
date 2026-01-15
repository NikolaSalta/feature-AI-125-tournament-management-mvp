package com.tournament.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MatchRequest {

    @NotNull(message = "Player 1 ID is required")
    private Long player1Id;

    @NotNull(message = "Player 2 ID is required")
    private Long player2Id;

    @Positive(message = "Round must be positive")
    private Integer round;

    public MatchRequest() {
    }

    public MatchRequest(Long player1Id, Long player2Id, Integer round) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.round = round;
    }

    public Long getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(Long player1Id) {
        this.player1Id = player1Id;
    }

    public Long getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(Long player2Id) {
        this.player2Id = player2Id;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }
}
