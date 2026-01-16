package com.chessai.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Победитель турнира.
 * Поддерживает множественных победителей (детские турниры, несколько призовых мест).
 * 
 * Примеры использования:
 * - Детский турнир: все участники — победители
 * - Классический турнир: 1-3 место
 * - Специальные призы: "Лучший дебют", "Самый юный участник"
 */
@Entity
@Table(name = "tournament_winners",
    indexes = {
        @Index(name = "idx_tw_tournament", columnList = "tournament_id"),
        @Index(name = "idx_tw_player", columnList = "player_id"),
        @Index(name = "idx_tw_place", columnList = "tournament_id, place")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Турнир
     */
    @NotNull(message = "Tournament is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    /**
     * Участник-победитель
     */
    @NotNull(message = "Player is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private TournamentPlayer player;

    /**
     * Место (1, 2, 3... или null для специальных призов)
     */
    @Min(value = 1, message = "Place must be at least 1")
    @Column(name = "place")
    private Integer place;

    /**
     * Тип награды/приза
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "award_type", nullable = false, length = 30)
    @Builder.Default
    private AwardType awardType = AwardType.PLACE;

    /**
     * Название награды (для специальных призов)
     * Например: "Лучший дебют", "Самый юный участник", "За волю к победе"
     */
    @Column(name = "award_title", length = 200)
    private String awardTitle;

    /**
     * Описание/комментарий
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Призовая сумма (если есть)
     */
    @Min(value = 0, message = "Prize amount cannot be negative")
    @Column(name = "prize_amount")
    private Integer prizeAmount;

    /**
     * Дата присвоения награды
     */
    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    // =====================
    // JPA Callbacks
    // =====================

    @PrePersist
    protected void onCreate() {
        if (awardedAt == null) {
            awardedAt = LocalDateTime.now();
        }
        if (awardType == null) {
            awardType = AwardType.PLACE;
        }
    }

    // =====================
    // Business Methods
    // =====================

    /**
     * Проверяет, является ли это призовым местом (1, 2, 3...)
     */
    public boolean isPlaceAward() {
        return awardType == AwardType.PLACE && place != null;
    }

    /**
     * Проверяет, является ли это специальным призом
     */
    public boolean isSpecialAward() {
        return awardType == AwardType.SPECIAL;
    }

    /**
     * Проверяет, является ли это наградой участника (все победители)
     */
    public boolean isParticipationAward() {
        return awardType == AwardType.PARTICIPATION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TournamentWinner that = (TournamentWinner) o;
        if (id != null) return id.equals(that.id);
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TournamentWinner{" +
                "id=" + id +
                ", tournamentId=" + (tournament != null ? tournament.getId() : null) +
                ", playerId=" + (player != null ? player.getId() : null) +
                ", place=" + place +
                ", awardType=" + awardType +
                ", awardTitle='" + awardTitle + '\'' +
                '}';
    }

    /**
     * Тип награды
     */
    public enum AwardType {
        /**
         * Призовое место (1, 2, 3...)
         */
        PLACE,
        
        /**
         * Специальный приз ("Лучший дебют", "За волю к победе")
         */
        SPECIAL,
        
        /**
         * Награда участника (все — победители, детские турниры)
         */
        PARTICIPATION
    }
}
