package com.tournament.management.dto;

import com.tournament.management.entity.TournamentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class TournamentRequest {

    @NotBlank(message = "Tournament name is required")
    private String name;

    private String description;

    @Positive(message = "Max players must be positive")
    private Integer maxPlayers;

    public TournamentRequest() {
    }

    public TournamentRequest(String name, String description, Integer maxPlayers) {
        this.name = name;
        this.description = description;
        this.maxPlayers = maxPlayers;
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

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
