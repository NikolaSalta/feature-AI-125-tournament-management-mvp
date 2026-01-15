package com.tournament.management.service;

import com.tournament.management.dto.StandingResponse;
import com.tournament.management.entity.Player;
import com.tournament.management.entity.Result;
import com.tournament.management.entity.Tournament;
import com.tournament.management.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StandingsService {

    private final TournamentService tournamentService;
    private final ResultRepository resultRepository;

    public StandingsService(TournamentService tournamentService, ResultRepository resultRepository) {
        this.tournamentService = tournamentService;
        this.resultRepository = resultRepository;
    }

    public List<StandingResponse> getStandings(Long tournamentId) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        
        // Initialize standings for all players
        Map<Long, StandingResponse> standingsMap = new HashMap<>();
        for (Player player : tournament.getPlayers()) {
            standingsMap.put(player.getId(), new StandingResponse(player.getId(), player.getName()));
        }
        
        // Calculate standings based on results
        List<Result> results = resultRepository.findByTournamentId(tournamentId);
        for (Result result : results) {
            Long player1Id = result.getMatch().getPlayer1().getId();
            Long player2Id = result.getMatch().getPlayer2().getId();
            
            StandingResponse standing1 = standingsMap.get(player1Id);
            StandingResponse standing2 = standingsMap.get(player2Id);
            
            if (result.getIsDraw()) {
                if (standing1 != null) standing1.addDraw();
                if (standing2 != null) standing2.addDraw();
            } else if (result.getWinner() != null) {
                Long winnerId = result.getWinner().getId();
                if (winnerId.equals(player1Id)) {
                    if (standing1 != null) standing1.addWin();
                    if (standing2 != null) standing2.addLoss();
                } else {
                    if (standing1 != null) standing1.addLoss();
                    if (standing2 != null) standing2.addWin();
                }
            }
        }
        
        // Sort by points (descending), then by wins (descending)
        List<StandingResponse> standings = standingsMap.values().stream()
            .sorted(Comparator.comparing(StandingResponse::getPoints, Comparator.reverseOrder())
                .thenComparing(StandingResponse::getWins, Comparator.reverseOrder()))
            .collect(Collectors.toList());
        
        // Assign ranks
        int rank = 1;
        for (StandingResponse standing : standings) {
            standing.setRank(rank++);
        }
        
        return standings;
    }
}
