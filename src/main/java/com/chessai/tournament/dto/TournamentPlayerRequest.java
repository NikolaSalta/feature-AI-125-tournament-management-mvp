package com.chessai.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для регистрации участника в турнире
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request для регистрации участника в турнире")
public class TournamentPlayerRequest {

    @NotNull(message = "ID пользователя обязателен")
    @Schema(description = "ID пользователя для регистрации", example = "1")
    private Long userId;

    @Min(value = 0, message = "Рейтинг не может быть отрицательным")
    @Schema(description = "Рейтинг игрока на момент регистрации", example = "1500")
    private Integer ratingAtRegistration;
}
