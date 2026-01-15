package com.tournament.management.controller;

import com.tournament.management.dto.PlayerRequest;
import com.tournament.management.dto.PlayerResponse;
import com.tournament.management.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> registerPlayer(
            @PathVariable Long tournamentId,
            @Valid @RequestBody PlayerRequest request) {
        PlayerResponse response = playerService.registerPlayer(tournamentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getPlayers(@PathVariable Long tournamentId) {
        List<PlayerResponse> players = playerService.getPlayersByTournament(tournamentId);
        return ResponseEntity.ok(players);
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable Long playerId) {
        PlayerResponse response = playerService.getPlayerById(playerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> removePlayer(
            @PathVariable Long tournamentId,
            @PathVariable Long playerId) {
        playerService.removePlayer(tournamentId, playerId);
        return ResponseEntity.noContent().build();
    }
}
