package com.chessai.tournament.dto;

import com.chessai.tournament.entity.TournamentFormat;
import com.chessai.tournament.entity.TournamentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response с данными турнира")
public class TournamentResponse {

    @Schema(description = "ID турнира", example = "1")
    private Long id;

    @Schema(description = "Название турнира", example = "Чемпионат мира по шахматам 2026")
    private String name;

    @Schema(description = "Описание турнира")
    private String description;

    @Schema(description = "Формат турнира", example = "SWISS")
    private TournamentFormat format;

    @Schema(description = "Статус турнира", example = "DRAFT")
    private TournamentStatus status;

    @Schema(description = "ID организатора", example = "123")
    private Long organizerId;

    @Schema(description = "Дата и время начала", example = "2026-06-01T10:00:00")
    private LocalDateTime startDate;

    @Schema(description = "Дата и время окончания", example = "2026-06-10T18:00:00")
    private LocalDateTime endDate;

    @Schema(description = "Максимальное количество участников", example = "64")
    private Integer maxParticipants;

    @Schema(description = "Текущее количество участников", example = "32")
    private Integer currentParticipants;

    @Schema(description = "Контроль времени в минутах", example = "15")
    private Integer timeControlMinutes;

    @Schema(description = "Инкремент времени в секундах", example = "10")
    private Integer timeIncrementSeconds;

    @Schema(description = "Минимальный рейтинг", example = "2000")
    private Integer minRating;

    @Schema(description = "Максимальный рейтинг", example = "2800")
    private Integer maxRating;

    @Schema(description = "Призовой фонд", example = "10000")
    private Integer prizePool;

    @Schema(description = "Вступительный взнос", example = "50")
    private Integer entryFee;

    @Schema(description = "Публичный турнир", example = "true")
    private Boolean isPublic;

    @Schema(description = "Дата создания", example = "2026-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата обновления", example = "2026-01-02T15:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Доступны ли места для регистрации", example = "true")
    private Boolean hasAvailableSlots;

    @Schema(description = "Начался ли турнир", example = "false")
    private Boolean hasStarted;

    @Schema(description = "Можно ли редактировать турнир", example = "true")
    private Boolean isEditable;

    @Schema(description = "ID победителя (TournamentPlayer ID)", example = "1")
    private Long winnerId;

    @Schema(description = "Дата завершения турнира", example = "2026-06-10T18:00:00")
    private LocalDateTime completedAt;
}

