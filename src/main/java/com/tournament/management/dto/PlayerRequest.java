package com.tournament.management.dto;

import jakarta.validation.constraints.NotBlank;

public class PlayerRequest {

    @NotBlank(message = "Player name is required")
    private String name;

    private String email;

    public PlayerRequest() {
    }

    public PlayerRequest(String name, String email) {
        this.name = name;
        this.email = email;
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
}
