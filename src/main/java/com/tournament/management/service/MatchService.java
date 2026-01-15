package com.tournament.management.service;

import com.tournament.management.dto.MatchRequest;
import com.tournament.management.dto.MatchResponse;
import com.tournament.management.entity.Match;
import com.tournament.management.entity.Player;
import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentService tournamentService;
    private final PlayerService playerService;

    public MatchService(MatchRepository matchRepository, 
                       TournamentService tournamentService,
                       PlayerService playerService) {
        this.matchRepository = matchRepository;
        this.tournamentService = tournamentService;
        this.playerService = playerService;
    }

    public MatchResponse createMatch(Long tournamentId, MatchRequest request) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        
        // Validate tournament status
        if (tournament.getStatus() != TournamentStatus.ONGOING) {
            throw new IllegalStateException("Matches can only be created when tournament is ONGOING");
        }
        
        // Validate players
        if (request.getPlayer1Id().equals(request.getPlayer2Id())) {
            throw new IllegalArgumentException("A player cannot play against themselves");
        }
        
        Player player1 = playerService.getPlayerEntity(request.getPlayer1Id());
        Player player2 = playerService.getPlayerEntity(request.getPlayer2Id());
        
        // Verify players belong to this tournament
        if (!player1.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player 1 does not belong to this tournament");
        }
        if (!player2.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player 2 does not belong to this tournament");
        }
        
        Match match = new Match(tournament, player1, player2, request.getRound());
        tournament.addMatch(match);
        
        Match saved = matchRepository.save(match);
        return MatchResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with id: " + id));
        return MatchResponse.fromEntity(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesByTournament(Long tournamentId) {
        // Verify tournament exists
        tournamentService.getTournamentEntity(tournamentId);
        
        return matchRepository.findByTournamentId(tournamentId).stream()
            .map(MatchResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesByTournamentAndRound(Long tournamentId, Integer round) {
        // Verify tournament exists
        tournamentService.getTournamentEntity(tournamentId);
        
        return matchRepository.findByTournamentIdAndRound(tournamentId, round).stream()
            .map(MatchResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Integer> getRoundsByTournament(Long tournamentId) {
        // Verify tournament exists
        tournamentService.getTournamentEntity(tournamentId);
        
        return matchRepository.findDistinctRoundsByTournamentId(tournamentId);
    }

    public void deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with id: " + matchId));
        
        if (match.hasResult()) {
            throw new IllegalStateException("Cannot delete match that already has a result");
        }
        
        if (match.getTournament().getStatus() == TournamentStatus.FINISHED) {
            throw new IllegalStateException("Cannot delete match from a finished tournament");
        }
        
        matchRepository.delete(match);
    }

    @Transactional(readOnly = true)
    public Match getMatchEntity(Long id) {
        return matchRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with id: " + id));
    }
}
