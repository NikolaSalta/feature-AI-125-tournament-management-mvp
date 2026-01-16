package com.chessai.tournament.controller;

import com.chessai.tournament.dto.TournamentRequest;
import com.chessai.tournament.dto.TournamentResponse;
import com.chessai.tournament.entity.TournamentStatus;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.service.TournamentService;
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

@Slf4j
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournament API", description = "Управление турнирами")
public class TournamentController {

    private final TournamentService tournamentService;

    /**
     * Извлекает ID пользователя из SecurityContext.
     * SECURITY: Использует аутентифицированного пользователя вместо клиентского заголовка.
     */
    private Long extractUserId(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return user.getId();
    }

    @PostMapping
    @Operation(
            summary = "Создать турнир",
            description = "Создает новый турнир. Организатором становится текущий пользователь."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Турнир успешно создан",
                    content = @Content(schema = @Schema(implementation = TournamentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Невалидные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody TournamentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("POST /api/tournaments - создание турнира пользователем: {}", userId);
        TournamentResponse response = tournamentService.createTournament(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить турнир по ID",
            description = "Возвращает информацию о турнире по его ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Турнир найден",
                    content = @Content(schema = @Schema(implementation = TournamentResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Турнир не найден")
    })
    public ResponseEntity<TournamentResponse> getTournamentById(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/tournaments/{} - получение турнира", id);
        TournamentResponse response = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить все турниры",
            description = "Возвращает список всех турниров с пагинацией"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список турниров",
            content = @Content(schema = @Schema(implementation = Page.class))
    )
    public ResponseEntity<Page<TournamentResponse>> getAllTournaments(
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        log.debug("GET /api/tournaments - получение всех турниров");
        Page<TournamentResponse> response = tournamentService.getAllTournaments(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    @Operation(
            summary = "Получить публичные турниры",
            description = "Возвращает список публичных турниров"
    )
    @ApiResponse(responseCode = "200", description = "Список публичных турниров")
    public ResponseEntity<Page<TournamentResponse>> getPublicTournaments(
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        log.debug("GET /api/tournaments/public - получение публичных турниров");
        Page<TournamentResponse> response = tournamentService.getPublicTournaments(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Получить предстоящие турниры",
            description = "Возвращает список предстоящих турниров (дата начала в будущем)"
    )
    @ApiResponse(responseCode = "200", description = "Список предстоящих турниров")
    public ResponseEntity<Page<TournamentResponse>> getUpcomingTournaments(
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        log.debug("GET /api/tournaments/upcoming - получение предстоящих турниров");
        Page<TournamentResponse> response = tournamentService.getUpcomingTournaments(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Получить турниры по статусу",
            description = "Возвращает список турниров с указанным статусом"
    )
    @ApiResponse(responseCode = "200", description = "Список турниров")
    public ResponseEntity<Page<TournamentResponse>> getTournamentsByStatus(
            @Parameter(description = "Статус турнира", example = "REGISTRATION_OPEN")
            @PathVariable TournamentStatus status,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        log.debug("GET /api/tournaments/status/{} - получение турниров по статусу", status);
        Page<TournamentResponse> response = tournamentService.getTournamentsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizer/{organizerId}")
    @Operation(
            summary = "Получить турниры организатора",
            description = "Возвращает список турниров конкретного организатора"
    )
    @ApiResponse(responseCode = "200", description = "Список турниров организатора")
    public ResponseEntity<Page<TournamentResponse>> getTournamentsByOrganizer(
            @Parameter(description = "ID организатора", example = "1")
            @PathVariable Long organizerId,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        log.debug("GET /api/tournaments/organizer/{} - получение турниров организатора", organizerId);
        Page<TournamentResponse> response = tournamentService.getTournamentsByOrganizer(organizerId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Обновить турнир",
            description = "Обновляет данные турнира. Можно обновлять только турниры в статусе DRAFT или REGISTRATION_OPEN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Турнир обновлен",
                    content = @Content(schema = @Schema(implementation = TournamentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Невалидные данные"),
            @ApiResponse(responseCode = "404", description = "Турнир не найден"),
            @ApiResponse(responseCode = "409", description = "Турнир нельзя редактировать")
    })
    public ResponseEntity<TournamentResponse> updateTournament(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody TournamentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("PUT /api/tournaments/{} - обновление турнира пользователем: {}", id, userId);
        TournamentResponse response = tournamentService.updateTournament(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Изменить статус турнира",
            description = "Изменяет статус турнира"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус изменен",
                    content = @Content(schema = @Schema(implementation = TournamentResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Турнир не найден")
    })
    public ResponseEntity<TournamentResponse> updateTournamentStatus(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Новый статус", example = "REGISTRATION_OPEN")
            @RequestParam TournamentStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("PATCH /api/tournaments/{}/status - изменение статуса на: {}", id, status);
        TournamentResponse response = tournamentService.updateTournamentStatus(id, status, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить турнир",
            description = "Удаляет турнир. Можно удалять только турниры в статусе DRAFT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Турнир удален"),
            @ApiResponse(responseCode = "404", description = "Турнир не найден"),
            @ApiResponse(responseCode = "409", description = "Турнир нельзя удалить")
    })
    public ResponseEntity<Void> deleteTournament(
            @Parameter(description = "ID турнира", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        Long userId = extractUserId(currentUser);
        log.info("DELETE /api/tournaments/{} - удаление турнира пользователем: {}", id, userId);
        tournamentService.deleteTournament(id, userId);
        return ResponseEntity.noContent().build();
    }
}





