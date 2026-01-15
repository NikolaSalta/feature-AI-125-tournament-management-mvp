package com.tournament.management.dto;

import com.tournament.management.entity.Player;

import java.time.LocalDateTime;

public class PlayerResponse {

    private Long id;
    private String name;
    private String email;
    private Long tournamentId;
    private LocalDateTime registeredAt;

    public PlayerResponse() {
    }

    public static PlayerResponse fromEntity(Player player) {
        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setName(player.getName());
        response.setEmail(player.getEmail());
        response.setTournamentId(player.getTournament().getId());
        response.setRegisteredAt(player.getRegisteredAt());
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
