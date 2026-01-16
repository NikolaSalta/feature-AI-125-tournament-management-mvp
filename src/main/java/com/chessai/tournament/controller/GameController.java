package com.chessai.tournament.controller;

import com.chessai.tournament.dto.*;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления партиями турнира.
 * 
 * MVP: Все операции выполняются вручную организатором:
 * - Создание пар (партий)
 * - Ввод результатов  
 * - Определение победителей
 */
@Slf4j
@RestController
@RequestMapping("/api/tournaments/{tournamentId}/games")
@RequiredArgsConstructor
@Tag(name = "Tournament Games API", description = "Управление партиями турнира (MVP: мануальный ввод)")
public class GameController {

    private final GameService gameService;

    private Long extractUserId(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return user.getId();
    }

    @PostMapping
    @Operation(summary = "Создать партию", description = "Создаёт новую партию в турнире (мануально)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Партия создана",
                    content = @Content(schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "400", description = "Невалидные данные"),
            @ApiResponse(responseCode = "403", description = "Только организатор"),
            @ApiResponse(responseCode = "409", description = "Партия уже существует")
    })
    public ResponseEntity<GameResponse> createGame(
            @PathVariable Long tournamentId,
            @Valid @RequestBody GameRequest request,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments/{}/games", tournamentId);
        GameResponse response = gameService.createGame(tournamentId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch")
    @Operation(summary = "Создать несколько партий", description = "Batch создание партий")
    @ApiResponse(responseCode = "201", description = "Партии созданы")
    public ResponseEntity<List<GameResponse>> createGames(
            @PathVariable Long tournamentId,
            @Valid @RequestBody List<GameRequest> requests,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments/{}/games/batch - {} партий", tournamentId, requests.size());
        List<GameResponse> response = gameService.createGames(tournamentId, requests, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{gameId}/result")
    @Operation(summary = "Ввести результат партии",
            description = "MVP: мануальный ввод результата. WHITE_WINS=1-0, BLACK_WINS=0-1, DRAW=0.5-0.5")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Результат записан",
                    content = @Content(schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "403", description = "Только организатор"),
            @ApiResponse(responseCode = "404", description = "Партия не найдена")
    })
    public ResponseEntity<GameResponse> setGameResult(
            @PathVariable Long tournamentId,
            @PathVariable Long gameId,
            @Valid @RequestBody GameResultRequest request,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("PATCH /api/tournaments/{}/games/{}/result", tournamentId, gameId);
        GameResponse response = gameService.setGameResult(tournamentId, gameId, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить все партии турнира")
    @ApiResponse(responseCode = "200", description = "Список партий")
    public ResponseEntity<List<GameResponse>> getGames(@PathVariable Long tournamentId) {
        log.debug("GET /api/tournaments/{}/games", tournamentId);
        List<GameResponse> response = gameService.getGames(tournamentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    @Operation(summary = "Получить партии с пагинацией")
    @ApiResponse(responseCode = "200", description = "Страница партий")
    public ResponseEntity<Page<GameResponse>> getGamesPaged(
            @PathVariable Long tournamentId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/tournaments/{}/games/paged", tournamentId);
        Page<GameResponse> response = gameService.getGames(tournamentId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/round/{roundNumber}")
    @Operation(summary = "Получить партии раунда")
    @ApiResponse(responseCode = "200", description = "Партии раунда")
    public ResponseEntity<List<GameResponse>> getGamesByRound(
            @PathVariable Long tournamentId,
            @PathVariable Integer roundNumber) {
        log.debug("GET /api/tournaments/{}/games/round/{}", tournamentId, roundNumber);
        List<GameResponse> response = gameService.getGamesByRound(tournamentId, roundNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Получить партию по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о партии",
                    content = @Content(schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "404", description = "Партия не найдена")
    })
    public ResponseEntity<GameResponse> getGame(
            @PathVariable Long tournamentId,
            @PathVariable Long gameId) {
        log.debug("GET /api/tournaments/{}/games/{}", tournamentId, gameId);
        GameResponse response = gameService.getGame(tournamentId, gameId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/standings")
    @Operation(summary = "Получить турнирную таблицу",
            description = "Возвращает standings: участники отсортированы по очкам")
    @ApiResponse(responseCode = "200", description = "Турнирная таблица",
            content = @Content(schema = @Schema(implementation = StandingsResponse.class)))
    public ResponseEntity<StandingsResponse> getStandings(@PathVariable Long tournamentId) {
        log.debug("GET /api/tournaments/{}/games/standings", tournamentId);
        StandingsResponse response = gameService.getStandings(tournamentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    @Operation(summary = "Завершить турнир",
            description = "Завершает турнир, присваивает итоговые места. Победитель - игрок с макс. очков.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Турнир завершён",
                    content = @Content(schema = @Schema(implementation = StandingsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Только организатор"),
            @ApiResponse(responseCode = "409", description = "Турнир не в статусе IN_PROGRESS")
    })
    public ResponseEntity<StandingsResponse> completeTournament(
            @PathVariable Long tournamentId,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments/{}/games/complete", tournamentId);
        StandingsResponse response = gameService.completeTournament(tournamentId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{gameId}")
    @Operation(summary = "Удалить партию", description = "Только если результат ещё не введён")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Партия удалена"),
            @ApiResponse(responseCode = "403", description = "Только организатор"),
            @ApiResponse(responseCode = "409", description = "Нельзя удалить завершённую партию")
    })
    public ResponseEntity<Void> deleteGame(
            @PathVariable Long tournamentId,
            @PathVariable Long gameId,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("DELETE /api/tournaments/{}/games/{}", tournamentId, gameId);
        gameService.deleteGame(tournamentId, gameId, userId);
        return ResponseEntity.noContent().build();
    }

    // =====================
    // Победители турнира (множественные)
    // =====================

    @PostMapping("/winners")
    @Operation(summary = "Добавить победителя",
            description = "Добавляет победителя турнира. Поддерживает множественных победителей (детские турниры)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Победитель добавлен",
                    content = @Content(schema = @Schema(implementation = WinnerResponse.class))),
            @ApiResponse(responseCode = "403", description = "Только организатор")
    })
    public ResponseEntity<WinnerResponse> addWinner(
            @PathVariable Long tournamentId,
            @Valid @RequestBody WinnerRequest request,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments/{}/games/winners", tournamentId);
        WinnerResponse response = gameService.addWinner(tournamentId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/winners/all")
    @Operation(summary = "Сделать всех победителями",
            description = "Добавляет всех участников как победителей (для детских турниров)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Все участники добавлены как победители"),
            @ApiResponse(responseCode = "403", description = "Только организатор")
    })
    public ResponseEntity<List<WinnerResponse>> addAllAsWinners(
            @PathVariable Long tournamentId,
            @RequestParam(required = false, defaultValue = "Участник турнира") String awardTitle,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments/{}/games/winners/all", tournamentId);
        List<WinnerResponse> response = gameService.addAllAsWinners(tournamentId, awardTitle, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/winners")
    @Operation(summary = "Получить список победителей",
            description = "Возвращает всех победителей турнира")
    @ApiResponse(responseCode = "200", description = "Список победителей")
    public ResponseEntity<List<WinnerResponse>> getWinners(@PathVariable Long tournamentId) {
        log.debug("GET /api/tournaments/{}/games/winners", tournamentId);
        List<WinnerResponse> response = gameService.getWinners(tournamentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/winners/{winnerId}")
    @Operation(summary = "Удалить победителя")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Победитель удалён"),
            @ApiResponse(responseCode = "403", description = "Только организатор")
    })
    public ResponseEntity<Void> removeWinner(
            @PathVariable Long tournamentId,
            @PathVariable Long winnerId,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("DELETE /api/tournaments/{}/games/winners/{}", tournamentId, winnerId);
        gameService.removeWinner(tournamentId, winnerId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/winners")
    @Operation(summary = "Удалить всех победителей",
            description = "Удаляет всех победителей турнира")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Все победители удалены"),
            @ApiResponse(responseCode = "403", description = "Только организатор")
    })
    public ResponseEntity<Void> clearWinners(
            @PathVariable Long tournamentId,
            @AuthenticationPrincipal User currentUser) {
        Long userId = extractUserId(currentUser);
        log.info("DELETE /api/tournaments/{}/games/winners", tournamentId);
        gameService.clearWinners(tournamentId, userId);
        return ResponseEntity.noContent().build();
    }
}
