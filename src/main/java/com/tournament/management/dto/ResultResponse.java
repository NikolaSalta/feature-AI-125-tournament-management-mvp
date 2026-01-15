package com.tournament.management.dto;

import com.tournament.management.entity.Result;

import java.time.LocalDateTime;

public class ResultResponse {

    private Long id;
    private Long matchId;
    private Integer player1Score;
    private Integer player2Score;
    private Long winnerId;
    private String winnerName;
    private Boolean isDraw;
    private LocalDateTime submittedAt;

    public ResultResponse() {
    }

    public static ResultResponse fromEntity(Result result) {
        ResultResponse response = new ResultResponse();
        response.setId(result.getId());
        response.setMatchId(result.getMatch().getId());
        response.setPlayer1Score(result.getPlayer1Score());
        response.setPlayer2Score(result.getPlayer2Score());
        if (result.getWinner() != null) {
            response.setWinnerId(result.getWinner().getId());
            response.setWinnerName(result.getWinner().getName());
        }
        response.setIsDraw(result.getIsDraw());
        response.setSubmittedAt(result.getSubmittedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
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

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public Boolean getIsDraw() {
        return isDraw;
    }

    public void setIsDraw(Boolean isDraw) {
        this.isDraw = isDraw;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
