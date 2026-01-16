package com.chessai.tournament.controller;

import com.chessai.tournament.dto.TournamentPlayerRequest;
import com.chessai.tournament.dto.TournamentPlayerResponse;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.service.TournamentPlayerService;
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
 * Контроллер для управления участниками турнира
 */
@Slf4j
@RestController
@RequestMapping("/api/tournaments/{tournamentId}/players")
@RequiredArgsConstructor
@Tag(name = "Tournament Players API", description = "Управление участниками турнира")
public class TournamentPlayerController {

    private final TournamentPlayerService playerService;

    /**
     * Извлекает ID пользователя из SecurityContext
     */
    private Long extractUserId(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return user.getId();
    }

    @PostMapping
    @Operation(
            summary = "Зарегистрировать участника",
            description = "Регистрирует пользователя в качестве участника турнира"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Участник зарегистрирован",
                    content = @Content(schema = @Schema(implementation = TournamentPlayerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Невалидные данные"),
            @ApiResponse(responseCode = "404", description = "Турнир не найден"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже зарегистрирован")
    })
    public ResponseEntity<TournamentPlayerResponse> registerPlayer(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long tournamentId,
            @Valid @RequestBody TournamentPlayerRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments/{}/players - регистрация участника", tournamentId);
        TournamentPlayerResponse response = playerService.registerPlayer(tournamentId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить список участников",
            description = "Возвращает список участников турнира, отсортированных по очкам"
    )
    @ApiResponse(responseCode = "200", description = "Список участников")
    public ResponseEntity<List<TournamentPlayerResponse>> getPlayers(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long tournamentId
    ) {
        log.debug("GET /api/tournaments/{}/players", tournamentId);
        List<TournamentPlayerResponse> response = playerService.getPlayers(tournamentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    @Operation(
            summary = "Получить список участников с пагинацией",
            description = "Возвращает список участников турнира с пагинацией"
    )
    @ApiResponse(responseCode = "200", description = "Страница участников")
    public ResponseEntity<Page<TournamentPlayerResponse>> getPlayersPaged(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long tournamentId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/tournaments/{}/players/paged", tournamentId);
        Page<TournamentPlayerResponse> response = playerService.getPlayers(tournamentId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{playerId}")
    @Operation(
            summary = "Получить информацию об участнике",
            description = "Возвращает детальную информацию об участнике турнира"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация об участнике",
                    content = @Content(schema = @Schema(implementation = TournamentPlayerResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Участник не найден")
    })
    public ResponseEntity<TournamentPlayerResponse> getPlayer(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long tournamentId,
            @Parameter(description = "ID участника", example = "1")
            @PathVariable Long playerId
    ) {
        log.debug("GET /api/tournaments/{}/players/{}", tournamentId, playerId);
        TournamentPlayerResponse response = playerService.getPlayer(tournamentId, playerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{playerId}")
    @Operation(
            summary = "Снять участника с турнира",
            description = "Отменяет регистрацию участника или меняет статус на WITHDRAWN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Участник снят с турнира"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Участник не найден")
    })
    public ResponseEntity<Void> withdrawPlayer(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long tournamentId,
            @Parameter(description = "ID участника", example = "1")
            @PathVariable Long playerId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("DELETE /api/tournaments/{}/players/{}", tournamentId, playerId);
        playerService.withdrawPlayer(tournamentId, playerId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{playerId}/disqualify")
    @Operation(
            summary = "Дисквалифицировать участника",
            description = "Дисквалифицирует участника турнира (только организатор)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Участник дисквалифицирован",
                    content = @Content(schema = @Schema(implementation = TournamentPlayerResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Только организатор может дисквалифицировать"),
            @ApiResponse(responseCode = "404", description = "Участник не найден")
    })
    public ResponseEntity<TournamentPlayerResponse> disqualifyPlayer(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long tournamentId,
            @Parameter(description = "ID участника", example = "1")
            @PathVariable Long playerId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("PATCH /api/tournaments/{}/players/{}/disqualify", tournamentId, playerId);
        TournamentPlayerResponse response = playerService.disqualifyPlayer(tournamentId, playerId, userId);
        return ResponseEntity.ok(response);
    }
}
