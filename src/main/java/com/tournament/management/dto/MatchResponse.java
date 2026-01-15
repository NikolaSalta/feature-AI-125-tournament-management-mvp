package com.tournament.management.dto;

import com.tournament.management.entity.Match;

import java.time.LocalDateTime;

public class MatchResponse {

    private Long id;
    private Long tournamentId;
    private Long player1Id;
    private String player1Name;
    private Long player2Id;
    private String player2Name;
    private Integer round;
    private boolean hasResult;
    private ResultResponse result;
    private LocalDateTime createdAt;

    public MatchResponse() {
    }

    public static MatchResponse fromEntity(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setTournamentId(match.getTournament().getId());
        response.setPlayer1Id(match.getPlayer1().getId());
        response.setPlayer1Name(match.getPlayer1().getName());
        response.setPlayer2Id(match.getPlayer2().getId());
        response.setPlayer2Name(match.getPlayer2().getName());
        response.setRound(match.getRound());
        response.setHasResult(match.hasResult());
        if (match.hasResult()) {
            response.setResult(ResultResponse.fromEntity(match.getResult()));
        }
        response.setCreatedAt(match.getCreatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public Long getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(Long player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }

    public Long getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(Long player2Id) {
        this.player2Id = player2Id;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public boolean isHasResult() {
        return hasResult;
    }

    public void setHasResult(boolean hasResult) {
        this.hasResult = hasResult;
    }

    public ResultResponse getResult() {
        return result;
    }

    public void setResult(ResultResponse result) {
        this.result = result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
