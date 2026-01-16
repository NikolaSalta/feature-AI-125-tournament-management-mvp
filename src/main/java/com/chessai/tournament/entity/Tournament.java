package com.chessai.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность турнира.
 * Представляет шахматный турнир с его основными характеристиками.
 */
@Entity
@Table(name = "tournaments", indexes = {
        @Index(name = "idx_tournament_status", columnList = "status"),
        @Index(name = "idx_tournament_organizer", columnList = "organizer_id"),
        @Index(name = "idx_tournament_start_date", columnList = "start_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Версия для оптимистической блокировки.
     * Предотвращает lost updates при конкурентном доступе.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Название турнира
     */
    @NotBlank(message = "Tournament name is required")
    @Size(min = 3, max = 100, message = "Tournament name must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание турнира
     */
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(length = 2000)
    private String description;

    /**
     * Формат турнира
     */
    @NotNull(message = "Tournament format is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TournamentFormat format;

    /**
     * Статус турнира
     */
    @NotNull(message = "Tournament status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TournamentStatus status = TournamentStatus.DRAFT;

    /**
     * Организатор турнира (User ID)
     */
    @NotNull(message = "Organizer is required")
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    /**
     * Дата и время начала турнира.
     * 
     * NOTE: Используется @FutureOrPresent вместо @Future для поддержки обновления
     * турниров, которые уже начались или близки к началу.
     * При создании нового турнира проверка на будущую дату должна выполняться
     * на уровне сервиса.
     */
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * Дата и время окончания турнира
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * Максимальное количество участников
     */
    @Min(value = 2, message = "Tournament must allow at least 2 participants")
    @Max(value = 1000, message = "Tournament cannot exceed 1000 participants")
    @Column(name = "max_participants")
    private Integer maxParticipants;

    /**
     * Текущее количество зарегистрированных участников
     */
    @Min(value = 0, message = "Current participants cannot be negative")
    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;

    /**
     * Контроль времени (в минутах на партию)
     */
    @Min(value = 1, message = "Time control must be at least 1 minute")
    @Column(name = "time_control_minutes")
    private Integer timeControlMinutes;

    /**
     * Добавка времени (в секундах за ход)
     */
    @Min(value = 0, message = "Increment cannot be negative")
    @Column(name = "time_increment_seconds")
    private Integer timeIncrementSeconds;

    /**
     * Минимальный рейтинг для участия
     */
    @Min(value = 0, message = "Minimum rating cannot be negative")
    @Max(value = 3500, message = "Minimum rating cannot exceed 3500")
    @Column(name = "min_rating")
    private Integer minRating;

    /**
     * Максимальный рейтинг для участия
     */
    @Min(value = 0, message = "Maximum rating cannot be negative")
    @Max(value = 3500, message = "Maximum rating cannot exceed 3500")
    @Column(name = "max_rating")
    private Integer maxRating;

    /**
     * Призовой фонд (в условных единицах)
     */
    @Min(value = 0, message = "Prize pool cannot be negative")
    @Column(name = "prize_pool")
    private Integer prizePool;

    /**
     * Взнос за участие
     */
    @Min(value = 0, message = "Entry fee cannot be negative")
    @Column(name = "entry_fee")
    private Integer entryFee;

    /**
     * Публичный ли турнир
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    /**
     * ID победителя турнира (TournamentPlayer ID)
     * Заполняется при завершении турнира
     */
    @Column(name = "winner_id")
    private Long winnerId;

    /**
     * Дата завершения турнира
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Дата создания
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата последнего обновления
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =====================
    // JPA Callbacks
    // =====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TournamentStatus.DRAFT;
        }
        if (currentParticipants == null) {
            currentParticipants = 0;
        }
        if (isPublic == null) {
            isPublic = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =====================
    // Business Methods
    // =====================

    /**
     * Проверяет, можно ли зарегистрировать нового участника.
     * 
     * SECURITY: Этот метод используется вместе с @Version для оптимистической блокировки.
     * Механизм работает следующим образом:
     * 1. Оптимистическая блокировка (@Version) предотвращает lost updates
     * 2. Если произойдет конфликт версий, Spring выбросит OptimisticLockingFailureException
     * 3. Клиент должен повторить запрос после получения обновленных данных
     * 
     * Это безопаснее чем Pessimistic Locking, так как не блокирует другие операции.
     */
    public boolean canRegisterParticipant() {
        return status == TournamentStatus.REGISTRATION_OPEN
                && (maxParticipants == null || currentParticipants < maxParticipants);
    }

    /**
     * Проверяет, подходит ли рейтинг игрока для участия
     */
    public boolean isRatingEligible(int playerRating) {
        boolean minOk = minRating == null || playerRating >= minRating;
        boolean maxOk = maxRating == null || playerRating <= maxRating;
        return minOk && maxOk;
    }

    /**
     * Проверяет, является ли пользователь организатором турнира
     */
    public boolean isOrganizer(Long userId) {
        return organizerId != null && organizerId.equals(userId);
    }

    /**
     * Проверяет, можно ли редактировать турнир
     */
    public boolean isEditable() {
        return status == TournamentStatus.DRAFT || status == TournamentStatus.REGISTRATION_OPEN;
    }

    /**
     * Проверяет, начался ли турнир
     */
    public boolean hasStarted() {
        return status == TournamentStatus.IN_PROGRESS || status == TournamentStatus.COMPLETED;
    }

    @Override
    public String toString() {
        return "Tournament{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", format=" + format +
                ", status=" + status +
                ", startDate=" + startDate +
                ", participants=" + currentParticipants +
                (maxParticipants != null ? "/" + maxParticipants : "") +
                '}';
    }

    // =====================
    // equals and hashCode
    // =====================

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tournament that = (Tournament) o;
        if (id != null) {
            return id.equals(that.id);
        }
        // Для новых турниров сравниваем по name + organizerId
        return name != null && name.equals(that.name)
                && organizerId != null && organizerId.equals(that.organizerId);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (organizerId != null ? organizerId.hashCode() : 0);
        return result;
    }

    // =====================
    // Validation Helpers
    // =====================

    /**
     * Проверяет корректность диапазона рейтингов.
     * 
     * @return true если диапазон валиден (minRating <= maxRating или один из них
     *         null)
     */
    public boolean isRatingRangeValid() {
        if (minRating == null || maxRating == null) {
            return true;
        }
        return minRating <= maxRating;
    }

    /**
     * Проверяет корректность диапазона дат.
     * 
     * @return true если диапазон валиден (endDate >= startDate или endDate null)
     */
    public boolean isDateRangeValid() {
        if (endDate == null || startDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}





