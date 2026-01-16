package com.chessai.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для создания партии (мануальный ввод организатором)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request для создания партии в турнире")
public class GameRequest {

    @NotNull(message = "ID игрока белыми обязателен")
    @Schema(description = "ID участника турнира (белые фигуры)", example = "1")
    private Long whitePlayerId;

    @NotNull(message = "ID игрока чёрными обязателен")
    @Schema(description = "ID участника турнира (чёрные фигуры)", example = "2")
    private Long blackPlayerId;

    @NotNull(message = "Номер раунда обязателен")
    @Min(value = 1, message = "Номер раунда должен быть не менее 1")
    @Schema(description = "Номер раунда/тура", example = "1")
    private Integer roundNumber;

    @Min(value = 1, message = "Номер доски должен быть не менее 1")
    @Schema(description = "Номер доски/столика", example = "1")
    private Integer boardNumber;

    @Schema(description = "Запланированное время начала", example = "2026-01-20T14:00:00")
    private LocalDateTime scheduledAt;
}
