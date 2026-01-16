package com.chessai.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Участник турнира.
 * Связывает пользователя с турниром и хранит его статистику в рамках турнира.
 */
@Entity
@Table(name = "tournament_players", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tournament_player", 
        columnNames = {"tournament_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_tp_tournament", columnList = "tournament_id"),
        @Index(name = "idx_tp_user", columnList = "user_id"),
        @Index(name = "idx_tp_score", columnList = "score DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Турнир, в котором участвует игрок
     */
    @NotNull(message = "Tournament is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    /**
     * Пользователь-участник
     */
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Рейтинг игрока на момент регистрации
     */
    @Min(value = 0, message = "Rating cannot be negative")
    @Column(name = "rating_at_registration")
    private Integer ratingAtRegistration;

    /**
     * Набранные очки в турнире.
     * Победа = 1.0, Ничья = 0.5, Поражение = 0.0
     */
    @Column(name = "score", precision = 4, scale = 1, nullable = false)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    /**
     * Количество сыгранных партий
     */
    @Column(name = "games_played", nullable = false)
    @Builder.Default
    private Integer gamesPlayed = 0;

    /**
     * Количество побед
     */
    @Column(name = "wins", nullable = false)
    @Builder.Default
    private Integer wins = 0;

    /**
     * Количество ничьих
     */
    @Column(name = "draws", nullable = false)
    @Builder.Default
    private Integer draws = 0;

    /**
     * Количество поражений
     */
    @Column(name = "losses", nullable = false)
    @Builder.Default
    private Integer losses = 0;

    /**
     * Статус участника
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PlayerStatus status = PlayerStatus.REGISTERED;

    /**
     * Место в итоговой таблице (заполняется после завершения турнира)
     */
    @Column(name = "final_rank")
    private Integer finalRank;

    /**
     * Дата регистрации
     */
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

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
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (score == null) score = BigDecimal.ZERO;
        if (gamesPlayed == null) gamesPlayed = 0;
        if (wins == null) wins = 0;
        if (draws == null) draws = 0;
        if (losses == null) losses = 0;
        if (status == null) status = PlayerStatus.REGISTERED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =====================
    // Business Methods
    // =====================

    /**
     * Добавляет победу
     */
    public void addWin() {
        this.wins++;
        this.gamesPlayed++;
        this.score = this.score.add(BigDecimal.ONE);
    }

    /**
     * Добавляет ничью
     */
    public void addDraw() {
        this.draws++;
        this.gamesPlayed++;
        this.score = this.score.add(new BigDecimal("0.5"));
    }

    /**
     * Добавляет поражение
     */
    public void addLoss() {
        this.losses++;
        this.gamesPlayed++;
        // score не меняется при поражении
    }

    /**
     * Проверяет, активен ли участник
     */
    public boolean isActive() {
        return status == PlayerStatus.REGISTERED || status == PlayerStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TournamentPlayer that = (TournamentPlayer) o;
        if (id != null) return id.equals(that.id);
        return tournament != null && user != null &&
               tournament.equals(that.tournament) && user.equals(that.user);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        int result = tournament != null ? tournament.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TournamentPlayer{" +
                "id=" + id +
                ", tournamentId=" + (tournament != null ? tournament.getId() : null) +
                ", userId=" + (user != null ? user.getId() : null) +
                ", score=" + score +
                ", status=" + status +
                '}';
    }

    /**
     * Статус участника турнира
     */
    public enum PlayerStatus {
        REGISTERED,  // Зарегистрирован
        ACTIVE,      // Активно играет
        WITHDRAWN,   // Снялся с турнира
        DISQUALIFIED // Дисквалифицирован
    }
}
