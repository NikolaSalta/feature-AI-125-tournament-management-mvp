package com.chessai.tournament.dto;

import com.chessai.tournament.entity.TournamentPlayer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией об участнике турнира
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response с данными участника турнира")
public class TournamentPlayerResponse {

    @Schema(description = "ID записи участника", example = "1")
    private Long id;

    @Schema(description = "ID турнира", example = "1")
    private Long tournamentId;

    @Schema(description = "Название турнира", example = "Чемпионат города 2026")
    private String tournamentName;

    @Schema(description = "ID пользователя", example = "1")
    private Long userId;

    @Schema(description = "Имя пользователя", example = "player123")
    private String username;

    @Schema(description = "Полное имя игрока", example = "Иван Петров")
    private String fullName;

    @Schema(description = "Рейтинг на момент регистрации", example = "1500")
    private Integer ratingAtRegistration;

    @Schema(description = "Набранные очки", example = "3.5")
    private BigDecimal score;

    @Schema(description = "Количество сыгранных партий", example = "5")
    private Integer gamesPlayed;

    @Schema(description = "Количество побед", example = "3")
    private Integer wins;

    @Schema(description = "Количество ничьих", example = "1")
    private Integer draws;

    @Schema(description = "Количество поражений", example = "1")
    private Integer losses;

    @Schema(description = "Статус участника", example = "ACTIVE")
    private TournamentPlayer.PlayerStatus status;

    @Schema(description = "Место в таблице", example = "1")
    private Integer rank;

    @Schema(description = "Итоговое место (после завершения)", example = "1")
    private Integer finalRank;

    @Schema(description = "Дата регистрации", example = "2026-01-15T10:30:00")
    private LocalDateTime registeredAt;
}
