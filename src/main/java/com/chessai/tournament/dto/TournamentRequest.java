package com.chessai.tournament.dto;

import com.chessai.tournament.entity.TournamentFormat;
import com.chessai.tournament.entity.TournamentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request для создания/обновления турнира")
public class TournamentRequest {

    @NotBlank(message = "Название турнира обязательно")
    @Size(min = 3, max = 100, message = "Название должно быть от 3 до 100 символов")
    @Schema(description = "Название турнира", example = "Чемпионат мира по шахматам 2026")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    @Schema(description = "Описание турнира", example = "Международный турнир для игроков с рейтингом 2000+")
    private String description;

    @NotNull(message = "Формат турнира обязателен")
    @Schema(description = "Формат турнира", example = "SWISS")
    private TournamentFormat format;

    @Schema(description = "Статус турнира (по умолчанию DRAFT)", example = "DRAFT")
    private TournamentStatus status;

    @NotNull(message = "Дата начала обязательна")
    @FutureOrPresent(message = "Дата начала должна быть в настоящем или будущем")
    @Schema(description = "Дата и время начала турнира", example = "2026-06-01T10:00:00")
    private LocalDateTime startDate;

    @NotNull(message = "Дата окончания обязательна")
    @Schema(description = "Дата и время окончания турнира", example = "2026-06-10T18:00:00")
    private LocalDateTime endDate;

    @NotNull(message = "Максимальное количество участников обязательно")
    @Min(value = 2, message = "Минимум 2 участника")
    @Max(value = 1000, message = "Максимум 1000 участников")
    @Schema(description = "Максимальное количество участников", example = "64")
    private Integer maxParticipants;

    @NotNull(message = "Контроль времени обязателен")
    @Min(value = 1, message = "Минимум 1 минута")
    @Max(value = 180, message = "Максимум 180 минут")
    @Schema(description = "Контроль времени в минутах", example = "15")
    private Integer timeControlMinutes;

    @Min(value = 0, message = "Инкремент не может быть отрицательным")
    @Max(value = 60, message = "Максимум 60 секунд")
    @Schema(description = "Инкремент времени в секундах", example = "10")
    private Integer timeIncrementSeconds;

    @Min(value = 0, message = "Минимальный рейтинг не может быть отрицательным")
    @Max(value = 3000, message = "Максимальный рейтинг 3000")
    @Schema(description = "Минимальный рейтинг для участия", example = "2000")
    private Integer minRating;

    @Min(value = 0, message = "Максимальный рейтинг не может быть отрицательным")
    @Max(value = 3000, message = "Максимальный рейтинг 3000")
    @Schema(description = "Максимальный рейтинг для участия", example = "2800")
    private Integer maxRating;

    @Min(value = 0, message = "Призовой фонд не может быть отрицательным")
    @Schema(description = "Призовой фонд (в условных единицах)", example = "10000")
    private Integer prizePool;

    @Min(value = 0, message = "Взнос не может быть отрицательным")
    @Schema(description = "Вступительный взнос (в условных единицах)", example = "50")
    private Integer entryFee;

    @Schema(description = "Публичный турнир", example = "true")
    private Boolean isPublic;

    @AssertTrue(message = "Дата окончания должна быть после даты начала")
    private boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Максимальный рейтинг должен быть не меньше минимального")
    private boolean isMaxRatingGreaterOrEqualMin() {
        if (minRating == null || maxRating == null) {
            return true;
        }
        // SECURITY: Разрешаем равные значения (например, турнир для игроков с рейтингом ровно 2000)
        return maxRating >= minRating;
    }
}

