package com.chessai.tournament.dto;

import com.chessai.tournament.entity.TournamentWinner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией о победителе турнира
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response с данными победителя турнира")
public class WinnerResponse {

    @Schema(description = "ID записи победителя", example = "1")
    private Long id;

    @Schema(description = "ID турнира", example = "1")
    private Long tournamentId;

    @Schema(description = "Название турнира", example = "Детский турнир 2026")
    private String tournamentName;

    // Информация об игроке
    @Schema(description = "ID участника турнира", example = "1")
    private Long playerId;

    @Schema(description = "ID пользователя", example = "1")
    private Long userId;

    @Schema(description = "Имя пользователя", example = "young_champion")
    private String username;

    @Schema(description = "Полное имя", example = "Иванов Петя")
    private String fullName;

    @Schema(description = "Рейтинг игрока", example = "1200")
    private Integer rating;

    @Schema(description = "Набранные очки в турнире", example = "5.5")
    private BigDecimal score;

    // Информация о награде
    @Schema(description = "Место", example = "1")
    private Integer place;

    @Schema(description = "Тип награды", example = "PLACE")
    private TournamentWinner.AwardType awardType;

    @Schema(description = "Название награды", example = "Лучший дебют")
    private String awardTitle;

    @Schema(description = "Описание", example = "Отличная игра!")
    private String description;

    @Schema(description = "Призовая сумма", example = "1000")
    private Integer prizeAmount;

    @Schema(description = "Дата награждения", example = "2026-01-15T18:00:00")
    private LocalDateTime awardedAt;
}
