package com.tournament.management.controller;

import com.tournament.management.dto.StandingResponse;
import com.tournament.management.service.StandingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/standings")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping
    public ResponseEntity<List<StandingResponse>> getStandings(@PathVariable Long tournamentId) {
        List<StandingResponse> standings = standingsService.getStandings(tournamentId);
        return ResponseEntity.ok(standings);
    }
}
