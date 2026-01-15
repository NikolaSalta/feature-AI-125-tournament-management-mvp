package com.tournament.management.service;

import com.tournament.management.dto.TournamentRequest;
import com.tournament.management.dto.TournamentResponse;
import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public TournamentResponse createTournament(TournamentRequest request) {
        Tournament tournament = new Tournament(
            request.getName(),
            request.getDescription(),
            request.getMaxPlayers()
        );
        tournament.setStatus(TournamentStatus.DRAFT);
        Tournament saved = tournamentRepository.save(tournament);
        return TournamentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with id: " + id));
        return TournamentResponse.fromEntity(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getAllTournaments() {
        return tournamentRepository.findAll().stream()
            .map(TournamentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getTournamentsByStatus(TournamentStatus status) {
        return tournamentRepository.findByStatus(status).stream()
            .map(TournamentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public TournamentResponse updateTournament(Long id, TournamentRequest request) {
        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with id: " + id));
        
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new IllegalStateException("Cannot update tournament that is not in DRAFT status");
        }

        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setMaxPlayers(request.getMaxPlayers());
        
        Tournament saved = tournamentRepository.save(tournament);
        return TournamentResponse.fromEntity(saved);
    }

    public TournamentResponse updateTournamentStatus(Long id, TournamentStatus newStatus) {
        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with id: " + id));
        
        validateStatusTransition(tournament.getStatus(), newStatus);
        
        // Additional validation based on target status
        if (newStatus == TournamentStatus.ONGOING && tournament.getPlayers().size() < 2) {
            throw new IllegalStateException("Cannot start tournament with less than 2 players");
        }
        
        tournament.setStatus(newStatus);
        Tournament saved = tournamentRepository.save(tournament);
        return TournamentResponse.fromEntity(saved);
    }

    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with id: " + id));
        
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new IllegalStateException("Cannot delete tournament that is not in DRAFT status");
        }
        
        tournamentRepository.delete(tournament);
    }

    @Transactional(readOnly = true)
    public Tournament getTournamentEntity(Long id) {
        return tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with id: " + id));
    }

    private void validateStatusTransition(TournamentStatus currentStatus, TournamentStatus newStatus) {
        boolean valid = switch (currentStatus) {
            case DRAFT -> newStatus == TournamentStatus.REGISTRATION;
            case REGISTRATION -> newStatus == TournamentStatus.ONGOING || newStatus == TournamentStatus.DRAFT;
            case ONGOING -> newStatus == TournamentStatus.FINISHED;
            case FINISHED -> false;
        };
        
        if (!valid) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }
}
