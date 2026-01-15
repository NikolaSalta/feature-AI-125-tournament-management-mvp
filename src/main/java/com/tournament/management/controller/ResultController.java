package com.tournament.management.controller;

import com.tournament.management.dto.ResultRequest;
import com.tournament.management.dto.ResultResponse;
import com.tournament.management.service.ResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping("/matches/{matchId}/result")
    public ResponseEntity<ResultResponse> submitResult(
            @PathVariable Long matchId,
            @Valid @RequestBody ResultRequest request) {
        ResultResponse response = resultService.submitResult(matchId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/matches/{matchId}/result")
    public ResponseEntity<ResultResponse> updateResult(
            @PathVariable Long matchId,
            @Valid @RequestBody ResultRequest request) {
        ResultResponse response = resultService.updateResult(matchId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matches/{matchId}/result")
    public ResponseEntity<ResultResponse> getResult(@PathVariable Long matchId) {
        ResultResponse response = resultService.getResultByMatchId(matchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tournaments/{tournamentId}/results")
    public ResponseEntity<List<ResultResponse>> getResultsByTournament(@PathVariable Long tournamentId) {
        List<ResultResponse> results = resultService.getResultsByTournament(tournamentId);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/matches/{matchId}/result")
    public ResponseEntity<Void> deleteResult(@PathVariable Long matchId) {
        resultService.deleteResult(matchId);
        return ResponseEntity.noContent().build();
    }
}
