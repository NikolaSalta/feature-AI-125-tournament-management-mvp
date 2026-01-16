package com.chessai.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Шахматная партия в турнире.
 * MVP: результаты вносятся вручную организатором.
 * 
 * Система очков:
 * - Победа белых: whiteScore=1.0, blackScore=0.0
 * - Победа чёрных: whiteScore=0.0, blackScore=1.0
 * - Ничья: whiteScore=0.5, blackScore=0.5
 */
@Entity
@Table(name = "games",
    indexes = {
        @Index(name = "idx_game_tournament", columnList = "tournament_id"),
        @Index(name = "idx_game_white", columnList = "white_player_id"),
        @Index(name = "idx_game_black", columnList = "black_player_id"),
        @Index(name = "idx_game_round", columnList = "tournament_id, round_number"),
        @Index(name = "idx_game_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Турнир, в рамках которого проходит партия
     */
    @NotNull(message = "Tournament is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    /**
     * Игрок белыми фигурами
     */
    @NotNull(message = "White player is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id", nullable = false)
    private TournamentPlayer whitePlayer;

    /**
     * Игрок чёрными фигурами
     */
    @NotNull(message = "Black player is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id", nullable = false)
    private TournamentPlayer blackPlayer;

    /**
     * Номер раунда/тура в турнире
     */
    @Min(value = 1, message = "Round number must be at least 1")
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    /**
     * Номер доски/столика (опционально)
     */
    @Column(name = "board_number")
    private Integer boardNumber;

    /**
     * Статус партии
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GameStatus status = GameStatus.SCHEDULED;

    /**
     * Очки белых (1.0 = победа, 0.5 = ничья, 0.0 = поражение)
     */
    @Column(name = "white_score", precision = 2, scale = 1)
    private BigDecimal whiteScore;

    /**
     * Очки чёрных (1.0 = победа, 0.5 = ничья, 0.0 = поражение)
     */
    @Column(name = "black_score", precision = 2, scale = 1)
    private BigDecimal blackScore;

    /**
     * Результат партии (для отображения)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 20)
    private GameResult result;

    /**
     * Запланированное время начала
     */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * Фактическое время начала
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Время завершения
     */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /**
     * PGN-нотация партии (опционально, для будущего расширения)
     */
    @Column(name = "pgn", columnDefinition = "TEXT")
    private String pgn;

    /**
     * Комментарий/заметки к партии
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Дата создания записи
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
        if (status == null) status = GameStatus.SCHEDULED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =====================
    // Business Methods
    // =====================

    /**
     * Устанавливает результат: победа белых (1-0)
     */
    public void setWhiteWins() {
        this.result = GameResult.WHITE_WINS;
        this.whiteScore = BigDecimal.ONE;
        this.blackScore = BigDecimal.ZERO;
        this.status = GameStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * Устанавливает результат: победа чёрных (0-1)
     */
    public void setBlackWins() {
        this.result = GameResult.BLACK_WINS;
        this.whiteScore = BigDecimal.ZERO;
        this.blackScore = BigDecimal.ONE;
        this.status = GameStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * Устанавливает результат: ничья (½-½)
     */
    public void setDraw() {
        this.result = GameResult.DRAW;
        this.whiteScore = new BigDecimal("0.5");
        this.blackScore = new BigDecimal("0.5");
        this.status = GameStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * Устанавливает техническое поражение белых (0-1 forfeit)
     */
    public void setWhiteForfeit() {
        this.result = GameResult.WHITE_FORFEIT;
        this.whiteScore = BigDecimal.ZERO;
        this.blackScore = BigDecimal.ONE;
        this.status = GameStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * Устанавливает техническое поражение чёрных (1-0 forfeit)
     */
    public void setBlackForfeit() {
        this.result = GameResult.BLACK_FORFEIT;
        this.whiteScore = BigDecimal.ONE;
        this.blackScore = BigDecimal.ZERO;
        this.status = GameStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * Проверяет, завершена ли партия
     */
    public boolean isFinished() {
        return status == GameStatus.FINISHED || status == GameStatus.CANCELLED;
    }

    /**
     * Проверяет, можно ли редактировать результат
     */
    public boolean isResultEditable() {
        return status != GameStatus.CANCELLED;
    }

    /**
     * Возвращает строковое представление результата (для отображения)
     */
    public String getResultString() {
        if (result == null) return "-";
        return switch (result) {
            case WHITE_WINS, BLACK_FORFEIT -> "1-0";
            case BLACK_WINS, WHITE_FORFEIT -> "0-1";
            case DRAW -> "½-½";
            case NOT_PLAYED -> "-";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        if (id != null) return id.equals(game.id);
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", round=" + roundNumber +
                ", white=" + (whitePlayer != null ? whitePlayer.getId() : null) +
                ", black=" + (blackPlayer != null ? blackPlayer.getId() : null) +
                ", result=" + getResultString() +
                ", status=" + status +
                '}';
    }

    /**
     * Статус партии
     */
    public enum GameStatus {
        SCHEDULED,   // Запланирована
        IN_PROGRESS, // В процессе
        FINISHED,    // Завершена
        CANCELLED    // Отменена
    }

    /**
     * Результат партии
     */
    public enum GameResult {
        WHITE_WINS,    // Победа белых (1-0)
        BLACK_WINS,    // Победа чёрных (0-1)
        DRAW,          // Ничья (½-½)
        WHITE_FORFEIT, // Техническое поражение белых
        BLACK_FORFEIT, // Техническое поражение чёрных
        NOT_PLAYED     // Партия не состоялась
    }
}
