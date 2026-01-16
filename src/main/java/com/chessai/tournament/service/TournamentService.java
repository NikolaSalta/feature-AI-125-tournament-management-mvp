package com.chessai.tournament.service;

import com.chessai.tournament.dto.TournamentRequest;
import com.chessai.tournament.dto.TournamentResponse;
import com.chessai.tournament.entity.Tournament;
import com.chessai.tournament.entity.TournamentStatus;
import com.chessai.tournament.exception.TournamentNotEditableException;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    @Transactional
    public TournamentResponse createTournament(TournamentRequest request, Long organizerId) {
        log.info("Создание турнира: {} организатором: {}", request.getName(), organizerId);

        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .description(request.getDescription())
                .format(request.getFormat())
                .status(request.getStatus() != null ? request.getStatus() : TournamentStatus.DRAFT)
                .organizerId(organizerId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .maxParticipants(request.getMaxParticipants())
                .currentParticipants(0)
                .timeControlMinutes(request.getTimeControlMinutes())
                .timeIncrementSeconds(request.getTimeIncrementSeconds())
                .minRating(request.getMinRating())
                .maxRating(request.getMaxRating())
                .prizePool(request.getPrizePool())
                .entryFee(request.getEntryFee())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();

        Tournament saved = tournamentRepository.save(tournament);
        log.info("Турнир создан с ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(Long id) {
        log.debug("Получение турнира по ID: {}", id);
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
        return mapToResponse(tournament);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> getAllTournaments(Pageable pageable) {
        log.debug("Получение всех турниров, страница: {}", pageable.getPageNumber());
        return tournamentRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> getPublicTournaments(Pageable pageable) {
        log.debug("Получение публичных турниров");
        return tournamentRepository.findByIsPublicTrue(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> getTournamentsByStatus(TournamentStatus status, Pageable pageable) {
        log.debug("Получение турниров со статусом: {}", status);
        return tournamentRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> getUpcomingTournaments(Pageable pageable) {
        log.debug("Получение предстоящих турниров");
        return tournamentRepository.findUpcomingTournaments(LocalDateTime.now(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> getTournamentsByOrganizer(Long organizerId, Pageable pageable) {
        log.debug("Получение турниров организатора: {}", organizerId);
        return tournamentRepository.findByOrganizerId(organizerId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public TournamentResponse updateTournament(Long id, TournamentRequest request, Long userId) {
        log.info("Обновление турнира ID: {} пользователем: {}", id, userId);

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));

        // SECURITY: Проверяем что пользователь является организатором турнира
        if (!tournament.isOrganizer(userId)) {
            log.warn("Попытка несанкционированного обновления турнира ID: {} пользователем: {}", id, userId);
            throw new AccessDeniedException("Only tournament organizer can update tournament");
        }

        if (!tournament.isEditable()) {
            throw new TournamentNotEditableException(id);
        }

        // Обновляем только разрешенные поля
        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setFormat(request.getFormat());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setMaxParticipants(request.getMaxParticipants());
        tournament.setTimeControlMinutes(request.getTimeControlMinutes());
        tournament.setTimeIncrementSeconds(request.getTimeIncrementSeconds());
        tournament.setMinRating(request.getMinRating());
        tournament.setMaxRating(request.getMaxRating());
        tournament.setPrizePool(request.getPrizePool());
        tournament.setEntryFee(request.getEntryFee());
        // SECURITY: Проверяем null, чтобы избежать нарушения NOT NULL constraint
        if (request.getIsPublic() != null) {
            tournament.setIsPublic(request.getIsPublic());
        }

        Tournament updated = tournamentRepository.save(tournament);
        log.info("Турнир ID: {} обновлен", id);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTournament(Long id, Long userId) {
        log.info("Удаление турнира ID: {} пользователем: {}", id, userId);

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));

        // SECURITY: Проверяем что пользователь является организатором турнира
        if (!tournament.isOrganizer(userId)) {
            log.warn("Попытка несанкционированного удаления турнира ID: {} пользователем: {}", id, userId);
            throw new AccessDeniedException("Only tournament organizer can delete tournament");
        }

        if (!tournament.isEditable()) {
            throw new TournamentNotEditableException(id);
        }

        tournamentRepository.delete(tournament);
        log.info("Турнир ID: {} удален", id);
    }

    @Transactional
    public TournamentResponse updateTournamentStatus(Long id, TournamentStatus status, Long userId) {
        log.info("Изменение статуса турнира ID: {} на: {} пользователем: {}", id, status, userId);

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));

        // SECURITY: Проверяем что пользователь является организатором турнира
        if (!tournament.isOrganizer(userId)) {
            log.warn("Попытка несанкционированного изменения статуса турнира ID: {} пользователем: {}", id, userId);
            throw new AccessDeniedException("Only tournament organizer can change tournament status");
        }

        tournament.setStatus(status);
        Tournament updated = tournamentRepository.save(tournament);

        log.info("Статус турнира ID: {} изменен на: {}", id, status);
        return mapToResponse(updated);
    }

    private TournamentResponse mapToResponse(Tournament tournament) {
        return TournamentResponse.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .description(tournament.getDescription())
                .format(tournament.getFormat())
                .status(tournament.getStatus())
                .organizerId(tournament.getOrganizerId())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .maxParticipants(tournament.getMaxParticipants())
                .currentParticipants(tournament.getCurrentParticipants())
                .timeControlMinutes(tournament.getTimeControlMinutes())
                .timeIncrementSeconds(tournament.getTimeIncrementSeconds())
                .minRating(tournament.getMinRating())
                .maxRating(tournament.getMaxRating())
                .prizePool(tournament.getPrizePool())
                .entryFee(tournament.getEntryFee())
                .isPublic(tournament.getIsPublic())
                .createdAt(tournament.getCreatedAt())
                .updatedAt(tournament.getUpdatedAt())
                .hasAvailableSlots(tournament.canRegisterParticipant())
                .hasStarted(tournament.hasStarted())
                .isEditable(tournament.isEditable())
                .winnerId(tournament.getWinnerId())
                .completedAt(tournament.getCompletedAt())
                .build();
    }
}





