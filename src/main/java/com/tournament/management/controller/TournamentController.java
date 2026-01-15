package com.tournament.management.controller;

import com.tournament.management.dto.TournamentRequest;
import com.tournament.management.dto.TournamentResponse;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody TournamentRequest request) {
        TournamentResponse response = tournamentService.createTournament(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournament(@PathVariable Long id) {
        TournamentResponse response = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAllTournaments(
            @RequestParam(required = false) TournamentStatus status) {
        List<TournamentResponse> tournaments;
        if (status != null) {
            tournaments = tournamentService.getTournamentsByStatus(status);
        } else {
            tournaments = tournamentService.getAllTournaments();
        }
        return ResponseEntity.ok(tournaments);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentResponse> updateTournament(
            @PathVariable Long id,
            @Valid @RequestBody TournamentRequest request) {
        TournamentResponse response = tournamentService.updateTournament(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> updateTournamentStatus(
            @PathVariable Long id,
            @RequestParam TournamentStatus status) {
        TournamentResponse response = tournamentService.updateTournamentStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }
}
