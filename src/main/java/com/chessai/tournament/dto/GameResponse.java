package com.chessai.tournament.dto;

import com.chessai.tournament.entity.Game;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией о партии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response с данными партии")
public class GameResponse {

    @Schema(description = "ID партии", example = "1")
    private Long id;

    @Schema(description = "ID турнира", example = "1")
    private Long tournamentId;

    @Schema(description = "Название турнира", example = "Чемпионат города 2026")
    private String tournamentName;

    @Schema(description = "Номер раунда", example = "1")
    private Integer roundNumber;

    @Schema(description = "Номер доски", example = "1")
    private Integer boardNumber;

    // Информация о белых
    @Schema(description = "ID участника (белые)")
    private Long whitePlayerId;

    @Schema(description = "ID пользователя (белые)")
    private Long whiteUserId;

    @Schema(description = "Имя пользователя (белые)", example = "player1")
    private String whiteUsername;

    @Schema(description = "Полное имя (белые)", example = "Иван Петров")
    private String whiteFullName;

    @Schema(description = "Рейтинг белых", example = "1600")
    private Integer whiteRating;

    // Информация о чёрных
    @Schema(description = "ID участника (чёрные)")
    private Long blackPlayerId;

    @Schema(description = "ID пользователя (чёрные)")
    private Long blackUserId;

    @Schema(description = "Имя пользователя (чёрные)", example = "player2")
    private String blackUsername;

    @Schema(description = "Полное имя (чёрные)", example = "Сергей Иванов")
    private String blackFullName;

    @Schema(description = "Рейтинг чёрных", example = "1550")
    private Integer blackRating;

    // Результат
    @Schema(description = "Статус партии", example = "FINISHED")
    private Game.GameStatus status;

    @Schema(description = "Результат партии", example = "WHITE_WINS")
    private Game.GameResult result;

    @Schema(description = "Строковое представление результата", example = "1-0")
    private String resultString;

    @Schema(description = "Очки белых", example = "1.0")
    private BigDecimal whiteScore;

    @Schema(description = "Очки чёрных", example = "0.0")
    private BigDecimal blackScore;

    // Время
    @Schema(description = "Запланированное время", example = "2026-01-20T14:00:00")
    private LocalDateTime scheduledAt;

    @Schema(description = "Время начала", example = "2026-01-20T14:05:00")
    private LocalDateTime startedAt;

    @Schema(description = "Время завершения", example = "2026-01-20T16:30:00")
    private LocalDateTime finishedAt;

    // Дополнительно
    @Schema(description = "PGN-нотация")
    private String pgn;

    @Schema(description = "Комментарий")
    private String notes;

    @Schema(description = "Дата создания")
    private LocalDateTime createdAt;
}
