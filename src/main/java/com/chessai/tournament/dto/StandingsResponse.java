package com.chessai.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для турнирной таблицы (standings)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Турнирная таблица")
public class StandingsResponse {

    @Schema(description = "ID турнира", example = "1")
    private Long tournamentId;

    @Schema(description = "Название турнира", example = "Чемпионат города 2026")
    private String tournamentName;

    @Schema(description = "Статус турнира", example = "IN_PROGRESS")
    private String tournamentStatus;

    @Schema(description = "Текущий раунд", example = "3")
    private Integer currentRound;

    @Schema(description = "Всего раундов", example = "7")
    private Integer totalRounds;

    @Schema(description = "Участники с их результатами")
    private List<StandingsEntry> standings;

    /**
     * Запись в турнирной таблице
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Запись в турнирной таблице")
    public static class StandingsEntry {

        @Schema(description = "Место в таблице", example = "1")
        private Integer rank;

        @Schema(description = "ID участника турнира", example = "1")
        private Long playerId;

        @Schema(description = "ID пользователя", example = "1")
        private Long userId;

        @Schema(description = "Имя пользователя", example = "grandmaster")
        private String username;

        @Schema(description = "Полное имя", example = "Магнус Карлсен")
        private String fullName;

        @Schema(description = "Рейтинг", example = "2850")
        private Integer rating;

        @Schema(description = "Набранные очки", example = "5.5")
        private BigDecimal score;

        @Schema(description = "Сыграно партий", example = "7")
        private Integer gamesPlayed;

        @Schema(description = "Побед", example = "5")
        private Integer wins;

        @Schema(description = "Ничьих", example = "1")
        private Integer draws;

        @Schema(description = "Поражений", example = "1")
        private Integer losses;

        @Schema(description = "Процент набранных очков", example = "78.6")
        private Double scorePercentage;
    }
}
