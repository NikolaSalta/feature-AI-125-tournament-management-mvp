package com.chessai.tournament.service;

import com.chessai.tournament.dto.TournamentPlayerRequest;
import com.chessai.tournament.dto.TournamentPlayerResponse;
import com.chessai.tournament.entity.Tournament;
import com.chessai.tournament.entity.TournamentPlayer;
import com.chessai.tournament.entity.TournamentStatus;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.repository.TournamentPlayerRepository;
import com.chessai.tournament.repository.TournamentRepository;
import com.chessai.tournament.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Сервис для управления участниками турнира
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentPlayerService {

    private final TournamentPlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    /**
     * Регистрирует участника в турнире
     */
    @Transactional
    public TournamentPlayerResponse registerPlayer(Long tournamentId, TournamentPlayerRequest request, Long currentUserId) {
        log.info("Регистрация участника {} в турнире {}", request.getUserId(), tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        // Проверка: регистрация открыта
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new IllegalStateException("Registration is not open for this tournament");
        }

        // Проверка: есть ли место
        if (!tournament.canRegisterParticipant()) {
            throw new IllegalStateException("Tournament is full");
        }

        // Проверка: пользователь существует
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

        // Проверка: пользователь ещё не зарегистрирован
        if (playerRepository.existsByTournamentIdAndUserId(tournamentId, request.getUserId())) {
            throw new IllegalStateException("User is already registered for this tournament");
        }

        // Проверка рейтинга (если указаны ограничения)
        Integer rating = request.getRatingAtRegistration();
        if (rating != null && !tournament.isRatingEligible(rating)) {
            throw new IllegalStateException("Player rating " + rating + " does not meet tournament requirements");
        }

        // Создаём участника
        TournamentPlayer player = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user)
                .ratingAtRegistration(rating)
                .build();

        TournamentPlayer saved = playerRepository.save(player);

        // Обновляем счётчик участников
        tournament.setCurrentParticipants(tournament.getCurrentParticipants() + 1);
        tournamentRepository.save(tournament);

        log.info("Участник {} зарегистрирован в турнире {} с ID {}", 
                user.getUsername(), tournamentId, saved.getId());

        return mapToResponse(saved, 0);
    }

    /**
     * Отменяет регистрацию участника
     */
    @Transactional
    public void withdrawPlayer(Long tournamentId, Long playerId, Long currentUserId) {
        log.info("Снятие участника {} с турнира {}", playerId, tournamentId);

        TournamentPlayer player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        if (!player.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player does not belong to this tournament");
        }

        Tournament tournament = player.getTournament();

        // Проверка прав: организатор или сам участник
        boolean isOrganizer = tournament.isOrganizer(currentUserId);
        boolean isSelf = player.getUser().getId().equals(currentUserId);
        if (!isOrganizer && !isSelf) {
            throw new AccessDeniedException("Only organizer or the player can withdraw from tournament");
        }

        // Проверка: можно снять только если турнир не начался
        if (tournament.hasStarted()) {
            // Меняем статус на WITHDRAWN вместо удаления
            player.setStatus(TournamentPlayer.PlayerStatus.WITHDRAWN);
            playerRepository.save(player);
        } else {
            // До начала турнира - полностью удаляем
            playerRepository.delete(player);
            tournament.setCurrentParticipants(tournament.getCurrentParticipants() - 1);
            tournamentRepository.save(tournament);
        }

        log.info("Участник {} снят с турнира {}", playerId, tournamentId);
    }

    /**
     * Получает список участников турнира
     */
    @Transactional(readOnly = true)
    public List<TournamentPlayerResponse> getPlayers(Long tournamentId) {
        log.debug("Получение участников турнира {}", tournamentId);

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        AtomicInteger rank = new AtomicInteger(1);
        return playerRepository.findStandingsByTournamentId(tournamentId).stream()
                .map(p -> mapToResponse(p, rank.getAndIncrement()))
                .collect(Collectors.toList());
    }

    /**
     * Получает список участников турнира с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<TournamentPlayerResponse> getPlayers(Long tournamentId, Pageable pageable) {
        log.debug("Получение участников турнира {} с пагинацией", tournamentId);

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        AtomicInteger rank = new AtomicInteger(pageable.getPageNumber() * pageable.getPageSize() + 1);
        return playerRepository.findStandingsByTournamentId(tournamentId, pageable)
                .map(p -> mapToResponse(p, rank.getAndIncrement()));
    }

    /**
     * Получает информацию об участнике
     */
    @Transactional(readOnly = true)
    public TournamentPlayerResponse getPlayer(Long tournamentId, Long playerId) {
        log.debug("Получение участника {} турнира {}", playerId, tournamentId);

        TournamentPlayer player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        if (!player.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player does not belong to this tournament");
        }

        // Вычисляем место
        List<TournamentPlayer> standings = playerRepository.findStandingsByTournamentId(tournamentId);
        int rank = 1;
        for (TournamentPlayer p : standings) {
            if (p.getId().equals(playerId)) break;
            rank++;
        }

        return mapToResponse(player, rank);
    }

    /**
     * Дисквалифицирует участника (только организатор)
     */
    @Transactional
    public TournamentPlayerResponse disqualifyPlayer(Long tournamentId, Long playerId, Long currentUserId) {
        log.info("Дисквалификация участника {} в турнире {} организатором {}", 
                playerId, tournamentId, currentUserId);

        TournamentPlayer player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        Tournament tournament = player.getTournament();

        if (!tournament.getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player does not belong to this tournament");
        }

        // Только организатор может дисквалифицировать
        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can disqualify players");
        }

        player.setStatus(TournamentPlayer.PlayerStatus.DISQUALIFIED);
        TournamentPlayer saved = playerRepository.save(player);

        log.info("Участник {} дисквалифицирован в турнире {}", playerId, tournamentId);

        return mapToResponse(saved, 0);
    }

    /**
     * Проверяет, зарегистрирован ли пользователь в турнире
     */
    @Transactional(readOnly = true)
    public boolean isRegistered(Long tournamentId, Long userId) {
        return playerRepository.existsByTournamentIdAndUserId(tournamentId, userId);
    }

    /**
     * Маппинг entity в DTO
     */
    private TournamentPlayerResponse mapToResponse(TournamentPlayer player, int rank) {
        User user = player.getUser();
        Tournament tournament = player.getTournament();

        return TournamentPlayerResponse.builder()
                .id(player.getId())
                .tournamentId(tournament.getId())
                .tournamentName(tournament.getName())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .ratingAtRegistration(player.getRatingAtRegistration())
                .score(player.getScore())
                .gamesPlayed(player.getGamesPlayed())
                .wins(player.getWins())
                .draws(player.getDraws())
                .losses(player.getLosses())
                .status(player.getStatus())
                .rank(rank > 0 ? rank : null)
                .finalRank(player.getFinalRank())
                .registeredAt(player.getRegisteredAt())
                .build();
    }
}
