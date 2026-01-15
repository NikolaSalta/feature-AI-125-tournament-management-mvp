package com.tournament.management.dto;

import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;

import java.time.LocalDateTime;

public class TournamentResponse {

    private Long id;
    private String name;
    private String description;
    private TournamentStatus status;
    private Integer maxPlayers;
    private int playerCount;
    private int matchCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TournamentResponse() {
    }

    public static TournamentResponse fromEntity(Tournament tournament) {
        TournamentResponse response = new TournamentResponse();
        response.setId(tournament.getId());
        response.setName(tournament.getName());
        response.setDescription(tournament.getDescription());
        response.setStatus(tournament.getStatus());
        response.setMaxPlayers(tournament.getMaxPlayers());
        response.setPlayerCount(tournament.getPlayers().size());
        response.setMatchCount(tournament.getMatches().size());
        response.setCreatedAt(tournament.getCreatedAt());
        response.setUpdatedAt(tournament.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
