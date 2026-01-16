package com.chessai.tournament.dto;

import com.chessai.tournament.entity.Game;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ввода результата партии (мануальный ввод организатором)
 * 
 * MVP: Результаты вводятся вручную.
 * Система очков:
 * - WHITE_WINS: белые +1, чёрные +0
 * - BLACK_WINS: белые +0, чёрные +1  
 * - DRAW: обоим +0.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request для ввода результата партии")
public class GameResultRequest {

    @NotNull(message = "Результат партии обязателен")
    @Schema(description = "Результат партии", example = "WHITE_WINS",
            allowableValues = {"WHITE_WINS", "BLACK_WINS", "DRAW", "WHITE_FORFEIT", "BLACK_FORFEIT", "NOT_PLAYED"})
    private Game.GameResult result;

    @Schema(description = "Комментарий к партии", example = "Мат в 45 ходов")
    private String notes;

    @Schema(description = "PGN-нотация партии (опционально)", example = "1. e4 e5 2. Nf3 Nc6...")
    private String pgn;
}
