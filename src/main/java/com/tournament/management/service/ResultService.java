package com.tournament.management.service;

import com.tournament.management.dto.ResultRequest;
import com.tournament.management.dto.ResultResponse;
import com.tournament.management.entity.Match;
import com.tournament.management.entity.Result;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResultService {

    private final ResultRepository resultRepository;
    private final MatchService matchService;

    public ResultService(ResultRepository resultRepository, MatchService matchService) {
        this.resultRepository = resultRepository;
        this.matchService = matchService;
    }

    public ResultResponse submitResult(Long matchId, ResultRequest request) {
        Match match = matchService.getMatchEntity(matchId);
        
        // Validate tournament status
        if (match.getTournament().getStatus() != TournamentStatus.ONGOING) {
            throw new IllegalStateException("Results can only be submitted when tournament is ONGOING");
        }
        
        // Check if result already exists
        if (match.hasResult()) {
            throw new IllegalStateException("Match already has a result. Use update endpoint instead.");
        }
        
        Result result = new Result(match, request.getPlayer1Score(), request.getPlayer2Score());
        match.setResult(result);
        
        Result saved = resultRepository.save(result);
        return ResultResponse.fromEntity(saved);
    }

    public ResultResponse updateResult(Long matchId, ResultRequest request) {
        Match match = matchService.getMatchEntity(matchId);
        
        // Validate tournament status
        if (match.getTournament().getStatus() == TournamentStatus.FINISHED) {
            throw new IllegalStateException("Cannot update results for a finished tournament");
        }
        
        // Check if result exists
        if (!match.hasResult()) {
            throw new IllegalStateException("Match does not have a result yet. Use submit endpoint instead.");
        }
        
        Result result = match.getResult();
        result.updateScores(request.getPlayer1Score(), request.getPlayer2Score());
        
        Result saved = resultRepository.save(result);
        return ResultResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ResultResponse getResultByMatchId(Long matchId) {
        Result result = resultRepository.findByMatchId(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Result not found for match id: " + matchId));
        return ResultResponse.fromEntity(result);
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> getResultsByTournament(Long tournamentId) {
        return resultRepository.findByTournamentId(tournamentId).stream()
            .map(ResultResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public void deleteResult(Long matchId) {
        Match match = matchService.getMatchEntity(matchId);
        
        if (match.getTournament().getStatus() == TournamentStatus.FINISHED) {
            throw new IllegalStateException("Cannot delete result from a finished tournament");
        }
        
        if (!match.hasResult()) {
            throw new IllegalArgumentException("Match does not have a result");
        }
        
        Result result = match.getResult();
        match.setResult(null);
        resultRepository.delete(result);
    }
}
