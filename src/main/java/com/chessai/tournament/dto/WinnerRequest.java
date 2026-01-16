package com.chessai.tournament.dto;

import com.chessai.tournament.entity.TournamentWinner;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для добавления победителя турнира
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request для добавления победителя турнира")
public class WinnerRequest {

    @NotNull(message = "ID участника обязателен")
    @Schema(description = "ID участника турнира (TournamentPlayer ID)", example = "1")
    private Long playerId;

    @Min(value = 1, message = "Место должно быть не менее 1")
    @Schema(description = "Место (1, 2, 3... или null для специальных призов)", example = "1")
    private Integer place;

    @Schema(description = "Тип награды", example = "PLACE",
            allowableValues = {"PLACE", "SPECIAL", "PARTICIPATION"})
    @Builder.Default
    private TournamentWinner.AwardType awardType = TournamentWinner.AwardType.PLACE;

    @Schema(description = "Название награды (для специальных призов)", 
            example = "Лучший дебют")
    private String awardTitle;

    @Schema(description = "Описание/комментарий", example = "Отличная игра!")
    private String description;

    @Min(value = 0, message = "Призовая сумма не может быть отрицательной")
    @Schema(description = "Призовая сумма", example = "1000")
    private Integer prizeAmount;
}
