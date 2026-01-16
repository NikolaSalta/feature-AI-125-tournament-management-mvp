package com.chessai.tournament.service;

import com.chessai.tournament.dto.*;
import com.chessai.tournament.entity.*;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.repository.GameRepository;
import com.chessai.tournament.repository.TournamentPlayerRepository;
import com.chessai.tournament.repository.TournamentRepository;
import com.chessai.tournament.repository.TournamentWinnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Сервис для управления партиями турнира.
 * 
 * MVP: Все операции выполняются вручную организатором:
 * - Создание пар (партий)
 * - Ввод результатов
 * - Определение победителей
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository playerRepository;
    private final TournamentWinnerRepository winnerRepository;

    // =====================
    // Создание партий (мануально)
    // =====================

    /**
     * Создаёт партию в турнире (мануальное создание организатором)
     */
    @Transactional
    public GameResponse createGame(Long tournamentId, GameRequest request, Long currentUserId) {
        log.info("Создание партии в турнире {} организатором {}", tournamentId, currentUserId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        // Только организатор может создавать партии
        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can create games");
        }

        // Проверка: турнир должен быть в процессе или с закрытой регистрацией
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS &&
            tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED) {
            throw new IllegalStateException("Cannot create games in tournament with status: " + tournament.getStatus());
        }

        // Получаем участников
        TournamentPlayer whitePlayer = playerRepository.findById(request.getWhitePlayerId())
                .orElseThrow(() -> new IllegalArgumentException("White player not found: " + request.getWhitePlayerId()));
        TournamentPlayer blackPlayer = playerRepository.findById(request.getBlackPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Black player not found: " + request.getBlackPlayerId()));

        // Проверка: участники из этого турнира
        if (!whitePlayer.getTournament().getId().equals(tournamentId) ||
            !blackPlayer.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Players must belong to the same tournament");
        }

        // Проверка: не играют сами с собой
        if (whitePlayer.getId().equals(blackPlayer.getId())) {
            throw new IllegalArgumentException("A player cannot play against themselves");
        }

        // Проверка: нет дубликата в этом раунде
        if (gameRepository.existsByTournamentIdAndRoundAndPlayers(
                tournamentId, request.getRoundNumber(), 
                request.getWhitePlayerId(), request.getBlackPlayerId())) {
            throw new IllegalStateException("Game between these players already exists in round " + request.getRoundNumber());
        }

        // Создаём партию
        Game game = Game.builder()
                .tournament(tournament)
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .roundNumber(request.getRoundNumber())
                .boardNumber(request.getBoardNumber())
                .scheduledAt(request.getScheduledAt())
                .status(Game.GameStatus.SCHEDULED)
                .build();

        Game saved = gameRepository.save(game);

        log.info("Партия {} создана: {} vs {} (раунд {})", 
                saved.getId(), whitePlayer.getUser().getUsername(), 
                blackPlayer.getUser().getUsername(), request.getRoundNumber());

        return mapToResponse(saved);
    }

    /**
     * Создаёт несколько партий сразу (для удобства организатора)
     */
    @Transactional
    public List<GameResponse> createGames(Long tournamentId, List<GameRequest> requests, Long currentUserId) {
        log.info("Создание {} партий в турнире {}", requests.size(), tournamentId);

        return requests.stream()
                .map(request -> createGame(tournamentId, request, currentUserId))
                .collect(Collectors.toList());
    }

    // =====================
    // Ввод результатов (мануально)
    // =====================

    /**
     * Вводит результат партии (мануально организатором)
     * 
     * Система очков:
     * - Победа: +1.0
     * - Ничья: +0.5
     * - Поражение: +0.0
     */
    @Transactional
    public GameResponse setGameResult(Long tournamentId, Long gameId, GameResultRequest request, Long currentUserId) {
        log.info("Ввод результата партии {} в турнире {} организатором {}", gameId, tournamentId, currentUserId);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        Tournament tournament = game.getTournament();

        // Проверка: партия из этого турнира
        if (!tournament.getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Game does not belong to this tournament");
        }

        // Только организатор может вводить результаты
        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can set game results");
        }

        // Проверка: партию можно редактировать
        if (!game.isResultEditable()) {
            throw new IllegalStateException("Game result cannot be modified");
        }

        // Если партия уже имеет результат - откатываем старые очки
        if (game.getResult() != null && game.getStatus() == Game.GameStatus.FINISHED) {
            rollbackScores(game);
        }

        // Устанавливаем результат
        switch (request.getResult()) {
            case WHITE_WINS -> game.setWhiteWins();
            case BLACK_WINS -> game.setBlackWins();
            case DRAW -> game.setDraw();
            case WHITE_FORFEIT -> game.setWhiteForfeit();
            case BLACK_FORFEIT -> game.setBlackForfeit();
            case NOT_PLAYED -> {
                game.setResult(Game.GameResult.NOT_PLAYED);
                game.setWhiteScore(BigDecimal.ZERO);
                game.setBlackScore(BigDecimal.ZERO);
                game.setStatus(Game.GameStatus.CANCELLED);
            }
        }

        // Дополнительная информация
        if (request.getNotes() != null) {
            game.setNotes(request.getNotes());
        }
        if (request.getPgn() != null) {
            game.setPgn(request.getPgn());
        }

        Game saved = gameRepository.save(game);

        // Обновляем статистику игроков (если партия не отменена)
        if (saved.getStatus() == Game.GameStatus.FINISHED) {
            updatePlayerScores(saved);
        }

        log.info("Результат партии {}: {} (белые: {}, чёрные: {})", 
                gameId, saved.getResultString(), saved.getWhiteScore(), saved.getBlackScore());

        return mapToResponse(saved);
    }

    /**
     * Откатывает очки при изменении результата
     */
    private void rollbackScores(Game game) {
        TournamentPlayer white = game.getWhitePlayer();
        TournamentPlayer black = game.getBlackPlayer();

        // Убираем очки и статистику
        white.setScore(white.getScore().subtract(game.getWhiteScore()));
        black.setScore(black.getScore().subtract(game.getBlackScore()));

        white.setGamesPlayed(white.getGamesPlayed() - 1);
        black.setGamesPlayed(black.getGamesPlayed() - 1);

        // Убираем из статистики побед/ничьих/поражений
        if (game.getWhiteScore().compareTo(BigDecimal.ONE) == 0) {
            white.setWins(white.getWins() - 1);
            black.setLosses(black.getLosses() - 1);
        } else if (game.getBlackScore().compareTo(BigDecimal.ONE) == 0) {
            black.setWins(black.getWins() - 1);
            white.setLosses(white.getLosses() - 1);
        } else {
            white.setDraws(white.getDraws() - 1);
            black.setDraws(black.getDraws() - 1);
        }

        playerRepository.save(white);
        playerRepository.save(black);
    }

    /**
     * Обновляет очки и статистику игроков после ввода результата
     */
    private void updatePlayerScores(Game game) {
        TournamentPlayer white = game.getWhitePlayer();
        TournamentPlayer black = game.getBlackPlayer();

        // Добавляем очки
        white.setScore(white.getScore().add(game.getWhiteScore()));
        black.setScore(black.getScore().add(game.getBlackScore()));

        white.setGamesPlayed(white.getGamesPlayed() + 1);
        black.setGamesPlayed(black.getGamesPlayed() + 1);

        // Обновляем статистику побед/ничьих/поражений
        if (game.getWhiteScore().compareTo(BigDecimal.ONE) == 0) {
            white.setWins(white.getWins() + 1);
            black.setLosses(black.getLosses() + 1);
        } else if (game.getBlackScore().compareTo(BigDecimal.ONE) == 0) {
            black.setWins(black.getWins() + 1);
            white.setLosses(white.getLosses() + 1);
        } else {
            white.setDraws(white.getDraws() + 1);
            black.setDraws(black.getDraws() + 1);
        }

        playerRepository.save(white);
        playerRepository.save(black);
    }

    // =====================
    // Получение данных
    // =====================

    /**
     * Получает партию по ID
     */
    @Transactional(readOnly = true)
    public GameResponse getGame(Long tournamentId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        if (!game.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Game does not belong to this tournament");
        }

        return mapToResponse(game);
    }

    /**
     * Получает все партии турнира
     */
    @Transactional(readOnly = true)
    public List<GameResponse> getGames(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return gameRepository.findByTournamentId(tournamentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получает партии турнира с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<GameResponse> getGames(Long tournamentId, Pageable pageable) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return gameRepository.findByTournamentId(tournamentId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Получает партии раунда
     */
    @Transactional(readOnly = true)
    public List<GameResponse> getGamesByRound(Long tournamentId, Integer roundNumber) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return gameRepository.findByTournamentIdAndRoundNumberOrdered(tournamentId, roundNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получает турнирную таблицу (standings)
     */
    @Transactional(readOnly = true)
    public StandingsResponse getStandings(Long tournamentId) {
        log.debug("Получение турнирной таблицы для турнира {}", tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        List<TournamentPlayer> players = playerRepository.findStandingsByTournamentId(tournamentId);

        // Определяем текущий и максимальный раунд
        Integer maxRound = gameRepository.findMaxRoundByTournamentId(tournamentId).orElse(0);

        AtomicInteger rank = new AtomicInteger(1);
        List<StandingsResponse.StandingsEntry> entries = players.stream()
                .map(p -> mapToStandingsEntry(p, rank.getAndIncrement()))
                .collect(Collectors.toList());

        return StandingsResponse.builder()
                .tournamentId(tournamentId)
                .tournamentName(tournament.getName())
                .tournamentStatus(tournament.getStatus().name())
                .currentRound(maxRound)
                .totalRounds(null) // MVP: не вычисляем
                .standings(entries)
                .build();
    }

    // =====================
    // Завершение турнира
    // =====================

    /**
     * Завершает турнир и определяет победителя
     * 
     * Победитель - игрок с максимальным количеством очков.
     * При равенстве очков - по количеству побед.
     */
    @Transactional
    public StandingsResponse completeTournament(Long tournamentId, Long currentUserId) {
        log.info("Завершение турнира {} организатором {}", tournamentId, currentUserId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        // Только организатор может завершить турнир
        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can complete tournament");
        }

        // Проверка статуса
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress tournaments can be completed");
        }

        // Получаем таблицу и присваиваем места
        List<TournamentPlayer> standings = playerRepository.findStandingsByTournamentId(tournamentId);

        if (standings.isEmpty()) {
            throw new IllegalStateException("Cannot complete tournament with no players");
        }

        // Присваиваем итоговые места
        int rank = 1;
        for (TournamentPlayer player : standings) {
            player.setFinalRank(rank++);
            playerRepository.save(player);
        }

        // Устанавливаем победителя (первый в standings)
        TournamentPlayer winner = standings.get(0);
        tournament.setWinnerId(winner.getId());
        tournament.setCompletedAt(LocalDateTime.now());
        tournament.setStatus(TournamentStatus.COMPLETED);
        tournamentRepository.save(tournament);

        log.info("Турнир {} завершён. Победитель: {} (ID: {}) с {} очков", 
                tournamentId, 
                winner.getUser().getUsername(),
                winner.getId(),
                winner.getScore());

        return getStandings(tournamentId);
    }

    /**
     * Удаляет партию (только если результат ещё не введён)
     */
    @Transactional
    public void deleteGame(Long tournamentId, Long gameId, Long currentUserId) {
        log.info("Удаление партии {} в турнире {} организатором {}", gameId, tournamentId, currentUserId);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        if (!game.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Game does not belong to this tournament");
        }

        // Только организатор может удалять
        if (!game.getTournament().isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can delete games");
        }

        // Нельзя удалять завершённые партии
        if (game.getStatus() == Game.GameStatus.FINISHED) {
            throw new IllegalStateException("Cannot delete finished game. Set result to NOT_PLAYED instead.");
        }

        gameRepository.delete(game);

        log.info("Партия {} удалена", gameId);
    }

    // =====================
    // Маппинг
    // =====================

    private GameResponse mapToResponse(Game game) {
        TournamentPlayer white = game.getWhitePlayer();
        TournamentPlayer black = game.getBlackPlayer();

        return GameResponse.builder()
                .id(game.getId())
                .tournamentId(game.getTournament().getId())
                .tournamentName(game.getTournament().getName())
                .roundNumber(game.getRoundNumber())
                .boardNumber(game.getBoardNumber())
                // White
                .whitePlayerId(white.getId())
                .whiteUserId(white.getUser().getId())
                .whiteUsername(white.getUser().getUsername())
                .whiteFullName(white.getUser().getFullName())
                .whiteRating(white.getRatingAtRegistration())
                // Black
                .blackPlayerId(black.getId())
                .blackUserId(black.getUser().getId())
                .blackUsername(black.getUser().getUsername())
                .blackFullName(black.getUser().getFullName())
                .blackRating(black.getRatingAtRegistration())
                // Result
                .status(game.getStatus())
                .result(game.getResult())
                .resultString(game.getResultString())
                .whiteScore(game.getWhiteScore())
                .blackScore(game.getBlackScore())
                // Time
                .scheduledAt(game.getScheduledAt())
                .startedAt(game.getStartedAt())
                .finishedAt(game.getFinishedAt())
                // Additional
                .pgn(game.getPgn())
                .notes(game.getNotes())
                .createdAt(game.getCreatedAt())
                .build();
    }

    private StandingsResponse.StandingsEntry mapToStandingsEntry(TournamentPlayer player, int rank) {
        Double scorePercentage = null;
        if (player.getGamesPlayed() > 0) {
            BigDecimal maxPossible = new BigDecimal(player.getGamesPlayed());
            scorePercentage = player.getScore()
                    .divide(maxPossible, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return StandingsResponse.StandingsEntry.builder()
                .rank(rank)
                .playerId(player.getId())
                .userId(player.getUser().getId())
                .username(player.getUser().getUsername())
                .fullName(player.getUser().getFullName())
                .rating(player.getRatingAtRegistration())
                .score(player.getScore())
                .gamesPlayed(player.getGamesPlayed())
                .wins(player.getWins())
                .draws(player.getDraws())
                .losses(player.getLosses())
                .scorePercentage(scorePercentage)
                .build();
    }

    // =====================
    // Победители турнира (множественные)
    // =====================

    /**
     * Добавляет победителя турнира (мануально организатором)
     * Поддерживает множественных победителей для детских турниров
     */
    @Transactional
    public WinnerResponse addWinner(Long tournamentId, WinnerRequest request, Long currentUserId) {
        log.info("Добавление победителя в турнир {} организатором {}", tournamentId, currentUserId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        // Только организатор
        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can add winners");
        }

        // Турнир должен быть завершён или в процессе
        if (tournament.getStatus() != TournamentStatus.COMPLETED &&
            tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot add winners to tournament with status: " + tournament.getStatus());
        }

        TournamentPlayer player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + request.getPlayerId()));

        if (!player.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Player does not belong to this tournament");
        }

        TournamentWinner winner = TournamentWinner.builder()
                .tournament(tournament)
                .player(player)
                .place(request.getPlace())
                .awardType(request.getAwardType() != null ? request.getAwardType() : TournamentWinner.AwardType.PLACE)
                .awardTitle(request.getAwardTitle())
                .description(request.getDescription())
                .prizeAmount(request.getPrizeAmount())
                .build();

        TournamentWinner saved = winnerRepository.save(winner);

        log.info("Победитель добавлен: {} (место: {}, тип: {})", 
                player.getUser().getUsername(), request.getPlace(), request.getAwardType());

        return mapToWinnerResponse(saved);
    }

    /**
     * Добавляет всех участников как победителей (для детских турниров)
     */
    @Transactional
    public List<WinnerResponse> addAllAsWinners(Long tournamentId, String awardTitle, Long currentUserId) {
        log.info("Добавление всех участников как победителей в турнире {}", tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can add winners");
        }

        List<TournamentPlayer> players = playerRepository.findStandingsByTournamentId(tournamentId);

        return players.stream()
                .filter(TournamentPlayer::isActive)
                .map(player -> {
                    TournamentWinner winner = TournamentWinner.builder()
                            .tournament(tournament)
                            .player(player)
                            .awardType(TournamentWinner.AwardType.PARTICIPATION)
                            .awardTitle(awardTitle != null ? awardTitle : "Участник турнира")
                            .build();
                    return mapToWinnerResponse(winnerRepository.save(winner));
                })
                .collect(Collectors.toList());
    }

    /**
     * Получает список победителей турнира
     */
    @Transactional(readOnly = true)
    public List<WinnerResponse> getWinners(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return winnerRepository.findByTournamentIdOrdered(tournamentId).stream()
                .map(this::mapToWinnerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Удаляет победителя
     */
    @Transactional
    public void removeWinner(Long tournamentId, Long winnerId, Long currentUserId) {
        log.info("Удаление победителя {} из турнира {}", winnerId, tournamentId);

        TournamentWinner winner = winnerRepository.findById(winnerId)
                .orElseThrow(() -> new IllegalArgumentException("Winner not found: " + winnerId));

        if (!winner.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Winner does not belong to this tournament");
        }

        if (!winner.getTournament().isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can remove winners");
        }

        winnerRepository.delete(winner);
        log.info("Победитель {} удалён", winnerId);
    }

    /**
     * Удаляет всех победителей турнира
     */
    @Transactional
    public void clearWinners(Long tournamentId, Long currentUserId) {
        log.info("Удаление всех победителей турнира {}", tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (!tournament.isOrganizer(currentUserId)) {
            throw new AccessDeniedException("Only organizer can clear winners");
        }

        winnerRepository.deleteByTournamentId(tournamentId);
        log.info("Все победители турнира {} удалены", tournamentId);
    }

    private WinnerResponse mapToWinnerResponse(TournamentWinner winner) {
        TournamentPlayer player = winner.getPlayer();
        User user = player.getUser();

        return WinnerResponse.builder()
                .id(winner.getId())
                .tournamentId(winner.getTournament().getId())
                .tournamentName(winner.getTournament().getName())
                .playerId(player.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .rating(player.getRatingAtRegistration())
                .score(player.getScore())
                .place(winner.getPlace())
                .awardType(winner.getAwardType())
                .awardTitle(winner.getAwardTitle())
                .description(winner.getDescription())
                .prizeAmount(winner.getPrizeAmount())
                .awardedAt(winner.getAwardedAt())
                .build();
    }
}
