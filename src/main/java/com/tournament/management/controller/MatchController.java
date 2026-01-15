package com.tournament.management.controller;

import com.tournament.management.dto.MatchRequest;
import com.tournament.management.dto.MatchResponse;
import com.tournament.management.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(
            @PathVariable Long tournamentId,
            @Valid @RequestBody MatchRequest request) {
        MatchResponse response = matchService.createMatch(tournamentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MatchResponse>> getMatches(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Integer round) {
        List<MatchResponse> matches;
        if (round != null) {
            matches = matchService.getMatchesByTournamentAndRound(tournamentId, round);
        } else {
            matches = matchService.getMatchesByTournament(tournamentId);
        }
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long matchId) {
        MatchResponse response = matchService.getMatchById(matchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rounds")
    public ResponseEntity<List<Integer>> getRounds(@PathVariable Long tournamentId) {
        List<Integer> rounds = matchService.getRoundsByTournament(tournamentId);
        return ResponseEntity.ok(rounds);
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long matchId) {
        matchService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}
