package com.chessai.tournament.service;

import com.chessai.tournament.dto.GameRequest;
import com.chessai.tournament.dto.GameResultRequest;
import com.chessai.tournament.entity.*;
import com.chessai.tournament.repository.GameRepository;
import com.chessai.tournament.repository.TournamentPlayerRepository;
import com.chessai.tournament.repository.TournamentRepository;
import com.chessai.tournament.repository.TournamentWinnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Proof of Concept Tests for Manual MVP Risks.
 * These tests demonstrate current behavior which allows potential logical errors.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Manual MVP Risk Verification")
class ManualRiskTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentPlayerRepository playerRepository;

    @Mock
    private TournamentWinnerRepository winnerRepository;

    @InjectMocks
    private GameService gameService;

    private Tournament tournament;
    private TournamentPlayer playerA;
    private TournamentPlayer playerB;
    private TournamentPlayer playerC;
    private Game gameAB;

    @BeforeEach
    void setUp() {
        tournament = Tournament.builder()
                .id(1L)
                .name("Risk Tournament")
                .status(TournamentStatus.IN_PROGRESS)
                .organizerId(1L)
                .build();

        User userA = new User(); userA.setId(10L); userA.setUsername("A");
        User userB = new User(); userB.setId(11L); userB.setUsername("B");
        User userC = new User(); userC.setId(12L); userC.setUsername("C");

        playerA = TournamentPlayer.builder().id(1L).tournament(tournament).user(userA).build();
        playerB = TournamentPlayer.builder().id(2L).tournament(tournament).user(userB).build();
        playerC = TournamentPlayer.builder().id(3L).tournament(tournament).user(userC).build();

        gameAB = Game.builder()
                .id(100L)
                .tournament(tournament)
                .whitePlayer(playerA)
                .blackPlayer(playerB)
                .roundNumber(1)
                .status(Game.GameStatus.FINISHED)
                .result(Game.GameResult.WHITE_WINS)
                .whiteScore(BigDecimal.ONE)
                .blackScore(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("RISK: Player can be double booked in the same round")
    void verify_DoubleBookingIsPossible() {
        // Arrange
        // Player A is already playing Player B in Round 1 (implied by setup, but we are creating A vs C)
        // The check in service only checks existsByTournamentIdAndRoundAndPlayers(A, C), not A vs ANYONE.

        GameRequest requestAC = GameRequest.builder()
                .whitePlayerId(playerA.getId())
                .blackPlayerId(playerC.getId())
                .roundNumber(1)
                .build();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findById(playerA.getId())).thenReturn(Optional.of(playerA));
        when(playerRepository.findById(playerC.getId())).thenReturn(Optional.of(playerC));

        // Mocking that A vs C does NOT exist specifically
        when(gameRepository.existsByTournamentIdAndRoundAndPlayers(1L, 1, playerA.getId(), playerC.getId()))
                .thenReturn(false);

        when(gameRepository.save(any(Game.class))).thenAnswer(i -> {
            Game g = i.getArgument(0);
            g.setId(200L);
            return g;
        });

        // Act & Assert
        // This SHOULD fail in a robust system, but in MVP it passes.
        assertThatCode(() -> gameService.createGame(1L, requestAC, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RISK: Result can be modified after tournament is COMPLETED")
    void verify_PostCompletionModification() {
        // Arrange
        tournament.setStatus(TournamentStatus.COMPLETED); // Tournament is Over

        GameResultRequest request = GameResultRequest.builder()
                .result(Game.GameResult.DRAW)
                .build();

        when(gameRepository.findById(100L)).thenReturn(Optional.of(gameAB));
        when(gameRepository.save(any(Game.class))).thenReturn(gameAB);
        // Mocks for player updates
        when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));

        // Act & Assert
        // This SHOULD fail (tournament is closed), but currently passes.
        assertThatCode(() -> gameService.setGameResult(1L, 100L, request, 1L))
                .doesNotThrowAnyException();

        assertThat(gameAB.getResult()).isEqualTo(Game.GameResult.DRAW);
    }
}
