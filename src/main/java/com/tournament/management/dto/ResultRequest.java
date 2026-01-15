package com.tournament.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ResultRequest {

    @NotNull(message = "Player 1 score is required")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer player1Score;

    @NotNull(message = "Player 2 score is required")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer player2Score;

    public ResultRequest() {
    }

    public ResultRequest(Integer player1Score, Integer player2Score) {
        this.player1Score = player1Score;
        this.player2Score = player2Score;
    }

    public Integer getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(Integer player1Score) {
        this.player1Score = player1Score;
    }

    public Integer getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(Integer player2Score) {
        this.player2Score = player2Score;
    }
}
