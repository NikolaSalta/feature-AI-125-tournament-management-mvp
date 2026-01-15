package com.tournament.management.service;

import com.tournament.management.dto.PlayerRequest;
import com.tournament.management.dto.PlayerResponse;
import com.tournament.management.entity.Player;
import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TournamentService tournamentService;

    public PlayerService(PlayerRepository playerRepository, TournamentService tournamentService) {
        this.playerRepository = playerRepository;
        this.tournamentService = tournamentService;
    }

    public PlayerResponse registerPlayer(Long tournamentId, PlayerRequest request) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        
        // Validate tournament status
        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new IllegalStateException("Player registration is only allowed during REGISTRATION phase");
        }
        
        // Check max players limit
        if (tournament.getMaxPlayers() != null && 
            tournament.getPlayers().size() >= tournament.getMaxPlayers()) {
            throw new IllegalStateException("Tournament has reached maximum number of players");
        }
        
        // Check for duplicate name
        if (playerRepository.existsByTournamentIdAndName(tournamentId, request.getName())) {
            throw new IllegalArgumentException("Player with this name already registered in the tournament");
        }
        
        Player player = new Player(request.getName(), request.getEmail());
        tournament.addPlayer(player);
        
        Player saved = playerRepository.save(player);
        return PlayerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PlayerResponse getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + id));
        return PlayerResponse.fromEntity(player);
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> getPlayersByTournament(Long tournamentId) {
        // Verify tournament exists
        tournamentService.getTournamentEntity(tournamentId);
        
        return playerRepository.findByTournamentId(tournamentId).stream()
            .map(PlayerResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public void removePlayer(Long tournamentId, Long playerId) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        
        if (tournament.getStatus() != TournamentStatus.REGISTRATION && 
            tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new IllegalStateException("Cannot remove player after registration phase has ended");
        }
        
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + playerId));
        
        if (!player.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player does not belong to this tournament");
        }
        
        tournament.removePlayer(player);
        playerRepository.delete(player);
    }

    @Transactional(readOnly = true)
    public Player getPlayerEntity(Long id) {
        return playerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + id));
    }
}
