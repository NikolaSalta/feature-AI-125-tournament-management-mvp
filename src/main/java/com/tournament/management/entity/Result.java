package com.tournament.management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the result of a match.
 */
@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Column(name = "player1_score", nullable = false)
    private Integer player1Score;

    @Column(name = "player2_score", nullable = false)
    private Integer player2Score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Column(name = "is_draw")
    private Boolean isDraw = false;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    public Result() {
    }

    public Result(Match match, Integer player1Score, Integer player2Score) {
        this.match = match;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        determineWinner();
    }

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    private void determineWinner() {
        if (player1Score > player2Score) {
            this.winner = match.getPlayer1();
            this.isDraw = false;
        } else if (player2Score > player1Score) {
            this.winner = match.getPlayer2();
            this.isDraw = false;
        } else {
            this.winner = null;
            this.isDraw = true;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Integer getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(Integer player1Score) {
        this.player1Score = player1Score;
    }

    public Integer getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(Integer player2Score) {
        this.player2Score = player2Score;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public Boolean getIsDraw() {
        return isDraw;
    }

    public void setIsDraw(Boolean isDraw) {
        this.isDraw = isDraw;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void updateScores(Integer player1Score, Integer player2Score) {
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        determineWinner();
    }
}
