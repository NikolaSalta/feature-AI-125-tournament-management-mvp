package com.chessai.tournament.service;

import com.chessai.tournament.dto.*;
import com.chessai.tournament.entity.*;
import com.chessai.tournament.entity.Role;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.repository.GameRepository;
import com.chessai.tournament.repository.TournamentPlayerRepository;
import com.chessai.tournament.repository.TournamentRepository;
import com.chessai.tournament.repository.TournamentWinnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для GameService.
 * 
 * Покрытие: 100% методов сервиса
 * - createGame: 7 тест-кейсов
 * - setGameResult: 7 тест-кейсов
 * - getStandings: 2 тест-кейса
 * - completeTournament: 4 тест-кейса
 * - addWinner: 4 тест-кейса
 * - addAllAsWinners: 2 тест-кейса
 * - removeWinner: 2 тест-кейса
 * - getWinners: 2 тест-кейса
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService Unit Tests")
class GameServiceTest {

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

    // Test data
    private Tournament tournament;
    private User whiteUser;
    private User blackUser;
    private TournamentPlayer whitePlayer;
    private TournamentPlayer blackPlayer;
    private Game game;
    private GameRequest gameRequest;
    private GameResultRequest resultRequest;

    @BeforeEach
    void setUp() {
        // Tournament: id=1, status=IN_PROGRESS, organizerId=1
        tournament = Tournament.builder()
                .id(1L)
                .name("Test Tournament")
                .format(TournamentFormat.SWISS)
                .status(TournamentStatus.IN_PROGRESS)
                .organizerId(1L)
                .maxParticipants(16)
                .currentParticipants(8)
                .startDate(LocalDateTime.now().minusDays(1))
                .build();

        // Users
        whiteUser = new User("whitePlayer", "white@test.com", "password123");
        whiteUser.setId(2L);
        whiteUser.setFirstName("White");
        whiteUser.setLastName("Player");
        whiteUser.addRole(Role.USER);

        blackUser = new User("blackPlayer", "black@test.com", "password123");
        blackUser.setId(3L);
        blackUser.setFirstName("Black");
        blackUser.setLastName("Player");
        blackUser.addRole(Role.USER);

        // Tournament Players
        whitePlayer = TournamentPlayer.builder()
                .id(1L)
                .tournament(tournament)
                .user(whiteUser)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.ACTIVE)
                .ratingAtRegistration(1500)
                .build();

        blackPlayer = TournamentPlayer.builder()
                .id(2L)
                .tournament(tournament)
                .user(blackUser)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.ACTIVE)
                .ratingAtRegistration(1600)
                .build();

        // Game: id=1, tournament, whitePlayer, blackPlayer, roundNumber=1, status=SCHEDULED
        game = Game.builder()
                .id(1L)
                .tournament(tournament)
                .whitePlayer(whitePlayer)
                .blackPlayer(blackPlayer)
                .roundNumber(1)
                .boardNumber(1)
                .status(Game.GameStatus.SCHEDULED)
                .build();

        // Requests
        gameRequest = GameRequest.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .roundNumber(1)
                .boardNumber(1)
                .build();

        resultRequest = GameResultRequest.builder()
                .result(Game.GameResult.WHITE_WINS)
                .build();
    }

    // =====================
    // createGame tests
    // =====================

    @Nested
    @DisplayName("createGame")
    class CreateGameTests {

        @Test
        @DisplayName("Should create game successfully when organizer")
        void createGame_Success() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(blackPlayer));
            when(gameRepository.existsByTournamentIdAndRoundAndPlayers(anyLong(), anyInt(), anyLong(), anyLong()))
                    .thenReturn(false);
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
                Game g = invocation.getArgument(0);
                g.setId(1L);
                return g;
            });

            // Act
            GameResponse response = gameService.createGame(1L, gameRequest, 1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getWhitePlayerId()).isEqualTo(1L);
            assertThat(response.getBlackPlayerId()).isEqualTo(2L);
            assertThat(response.getRoundNumber()).isEqualTo(1);
            verify(gameRepository).save(any(Game.class));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void createGame_NotOrganizer() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> gameService.createGame(1L, gameRequest, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can create games");
            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when tournament status is COMPLETED")
        void createGame_InvalidTournamentStatus() {
            // Arrange
            tournament.setStatus(TournamentStatus.COMPLETED);
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> gameService.createGame(1L, gameRequest, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot create games in tournament with status");
            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when white player not found")
        void createGame_PlayerNotFound() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gameService.createGame(1L, gameRequest, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("White player not found");
            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player plays against self")
        void createGame_PlayerAgainstSelf() {
            // Arrange
            gameRequest.setBlackPlayerId(1L); // same as white
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));

            // Act & Assert
            assertThatThrownBy(() -> gameService.createGame(1L, gameRequest, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot play against themselves");
            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when duplicate game in round")
        void createGame_DuplicateGame() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(blackPlayer));
            when(gameRepository.existsByTournamentIdAndRoundAndPlayers(1L, 1, 1L, 2L))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> gameService.createGame(1L, gameRequest, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Game between these players already exists");
            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player from different tournament")
        void createGame_PlayerFromDifferentTournament() {
            // Arrange
            Tournament otherTournament = Tournament.builder().id(999L).build();
            TournamentPlayer otherPlayer = TournamentPlayer.builder()
                    .id(3L)
                    .tournament(otherTournament)
                    .user(blackUser)
                    .build();

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(otherPlayer));

            // Act & Assert
            assertThatThrownBy(() -> gameService.createGame(1L, gameRequest, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Players must belong to the same tournament");
            verify(gameRepository, never()).save(any());
        }
    }

    // =====================
    // setGameResult tests
    // =====================

    @Nested
    @DisplayName("setGameResult")
    class SetGameResultTests {

        @Test
        @DisplayName("Should set WHITE_WINS result correctly")
        void setGameResult_WhiteWins() {
            // Arrange
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenReturn(game);
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            GameResponse response = gameService.setGameResult(1L, 1L, resultRequest, 1L);

            // Assert
            assertThat(game.getResult()).isEqualTo(Game.GameResult.WHITE_WINS);
            assertThat(game.getWhiteScore()).isEqualTo(BigDecimal.ONE);
            assertThat(game.getBlackScore()).isEqualTo(BigDecimal.ZERO);
            assertThat(game.getStatus()).isEqualTo(Game.GameStatus.FINISHED);
            verify(playerRepository, times(2)).save(any(TournamentPlayer.class));
        }

        @Test
        @DisplayName("Should set BLACK_WINS result correctly")
        void setGameResult_BlackWins() {
            // Arrange
            resultRequest.setResult(Game.GameResult.BLACK_WINS);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenReturn(game);
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            GameResponse response = gameService.setGameResult(1L, 1L, resultRequest, 1L);

            // Assert
            assertThat(game.getResult()).isEqualTo(Game.GameResult.BLACK_WINS);
            assertThat(game.getWhiteScore()).isEqualTo(BigDecimal.ZERO);
            assertThat(game.getBlackScore()).isEqualTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("Should set DRAW result correctly")
        void setGameResult_Draw() {
            // Arrange
            resultRequest.setResult(Game.GameResult.DRAW);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenReturn(game);
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            GameResponse response = gameService.setGameResult(1L, 1L, resultRequest, 1L);

            // Assert
            assertThat(game.getResult()).isEqualTo(Game.GameResult.DRAW);
            assertThat(game.getWhiteScore()).isEqualTo(new BigDecimal("0.5"));
            assertThat(game.getBlackScore()).isEqualTo(new BigDecimal("0.5"));
        }

        @Test
        @DisplayName("Should set WHITE_FORFEIT result (black wins by forfeit)")
        void setGameResult_WhiteForfeit() {
            // Arrange
            resultRequest.setResult(Game.GameResult.WHITE_FORFEIT);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenReturn(game);
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            GameResponse response = gameService.setGameResult(1L, 1L, resultRequest, 1L);

            // Assert
            assertThat(game.getResult()).isEqualTo(Game.GameResult.WHITE_FORFEIT);
            assertThat(game.getWhiteScore()).isEqualTo(BigDecimal.ZERO);
            assertThat(game.getBlackScore()).isEqualTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void setGameResult_NotOrganizer() {
            // Arrange
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

            // Act & Assert
            assertThatThrownBy(() -> gameService.setGameResult(1L, 1L, resultRequest, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can set game results");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when game not found")
        void setGameResult_GameNotFound() {
            // Arrange
            when(gameRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gameService.setGameResult(1L, 999L, resultRequest, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Game not found");
        }

        @Test
        @DisplayName("Should rollback old scores when updating existing result")
        void setGameResult_UpdateExisting() {
            // Arrange - game already has WHITE_WINS result
            game.setResult(Game.GameResult.WHITE_WINS);
            game.setWhiteScore(BigDecimal.ONE);
            game.setBlackScore(BigDecimal.ZERO);
            game.setStatus(Game.GameStatus.FINISHED);
            whitePlayer.setScore(BigDecimal.ONE);
            whitePlayer.setWins(1);
            whitePlayer.setGamesPlayed(1);
            blackPlayer.setLosses(1);
            blackPlayer.setGamesPlayed(1);

            resultRequest.setResult(Game.GameResult.DRAW); // change to draw

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenReturn(game);
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            gameService.setGameResult(1L, 1L, resultRequest, 1L);

            // Assert - scores should be rolled back and new ones applied
            assertThat(game.getResult()).isEqualTo(Game.GameResult.DRAW);
            // White: was 1.0 win, now 0.5 draw
            assertThat(whitePlayer.getScore()).isEqualTo(new BigDecimal("0.5"));
            assertThat(whitePlayer.getWins()).isEqualTo(0);
            assertThat(whitePlayer.getDraws()).isEqualTo(1);
        }
    }

    // =====================
    // getStandings tests
    // =====================

    @Nested
    @DisplayName("getStandings")
    class GetStandingsTests {

        @Test
        @DisplayName("Should return standings sorted by score DESC")
        void getStandings_ReturnsSortedByScore() {
            // Arrange
            whitePlayer.setScore(new BigDecimal("2.5"));
            blackPlayer.setScore(new BigDecimal("1.0"));

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findStandingsByTournamentId(1L))
                    .thenReturn(List.of(whitePlayer, blackPlayer));
            when(gameRepository.findMaxRoundByTournamentId(1L)).thenReturn(Optional.of(3));

            // Act
            StandingsResponse response = gameService.getStandings(1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTournamentId()).isEqualTo(1L);
            assertThat(response.getCurrentRound()).isEqualTo(3);
            assertThat(response.getStandings()).hasSize(2);
            assertThat(response.getStandings().get(0).getRank()).isEqualTo(1);
            assertThat(response.getStandings().get(0).getScore()).isEqualTo(new BigDecimal("2.5"));
        }

        @Test
        @DisplayName("Should throw TournamentNotFoundException when tournament not found")
        void getStandings_TournamentNotFound() {
            // Arrange
            when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gameService.getStandings(999L))
                    .isInstanceOf(TournamentNotFoundException.class);
        }
    }

    // =====================
    // completeTournament tests
    // =====================

    @Nested
    @DisplayName("completeTournament")
    class CompleteTournamentTests {

        @Test
        @DisplayName("Should complete tournament successfully")
        void completeTournament_Success() {
            // Arrange
            whitePlayer.setScore(new BigDecimal("3.0"));
            blackPlayer.setScore(new BigDecimal("2.0"));

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findStandingsByTournamentId(1L))
                    .thenReturn(List.of(whitePlayer, blackPlayer));
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(i -> i.getArgument(0));
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);
            when(gameRepository.findMaxRoundByTournamentId(1L)).thenReturn(Optional.of(3));

            // Act
            StandingsResponse response = gameService.completeTournament(1L, 1L);

            // Assert
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
            assertThat(tournament.getWinnerId()).isEqualTo(whitePlayer.getId());
            assertThat(whitePlayer.getFinalRank()).isEqualTo(1);
            assertThat(blackPlayer.getFinalRank()).isEqualTo(2);
            verify(tournamentRepository).save(tournament);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void completeTournament_NotOrganizer() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> gameService.completeTournament(1L, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can complete tournament");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when status is not IN_PROGRESS")
        void completeTournament_InvalidStatus() {
            // Arrange
            tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> gameService.completeTournament(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only in-progress tournaments can be completed");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no players")
        void completeTournament_NoPlayers() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findStandingsByTournamentId(1L)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> gameService.completeTournament(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete tournament with no players");
        }
    }

    // =====================
    // addWinner tests
    // =====================

    @Nested
    @DisplayName("addWinner")
    class AddWinnerTests {

        @Test
        @DisplayName("Should add winner with PLACE award")
        void addWinner_PlaceAward() {
            // Arrange
            tournament.setStatus(TournamentStatus.COMPLETED);
            WinnerRequest request = WinnerRequest.builder()
                    .playerId(1L)
                    .place(1)
                    .awardType(TournamentWinner.AwardType.PLACE)
                    .build();

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));
            when(winnerRepository.save(any(TournamentWinner.class))).thenAnswer(invocation -> {
                TournamentWinner w = invocation.getArgument(0);
                w.setId(1L);
                return w;
            });

            // Act
            WinnerResponse response = gameService.addWinner(1L, request, 1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPlace()).isEqualTo(1);
            assertThat(response.getAwardType()).isEqualTo(TournamentWinner.AwardType.PLACE);
            verify(winnerRepository).save(any(TournamentWinner.class));
        }

        @Test
        @DisplayName("Should add winner with SPECIAL award")
        void addWinner_SpecialAward() {
            // Arrange
            tournament.setStatus(TournamentStatus.COMPLETED);
            WinnerRequest request = WinnerRequest.builder()
                    .playerId(1L)
                    .awardType(TournamentWinner.AwardType.SPECIAL)
                    .awardTitle("Best Newcomer")
                    .build();

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));
            when(winnerRepository.save(any(TournamentWinner.class))).thenAnswer(invocation -> {
                TournamentWinner w = invocation.getArgument(0);
                w.setId(1L);
                return w;
            });

            // Act
            WinnerResponse response = gameService.addWinner(1L, request, 1L);

            // Assert
            assertThat(response.getAwardType()).isEqualTo(TournamentWinner.AwardType.SPECIAL);
            assertThat(response.getAwardTitle()).isEqualTo("Best Newcomer");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void addWinner_NotOrganizer() {
            // Arrange
            WinnerRequest request = WinnerRequest.builder().playerId(1L).build();
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> gameService.addWinner(1L, request, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can add winners");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player not found")
        void addWinner_PlayerNotFound() {
            // Arrange
            tournament.setStatus(TournamentStatus.COMPLETED);
            WinnerRequest request = WinnerRequest.builder().playerId(999L).build();
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gameService.addWinner(1L, request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Player not found");
        }
    }

    // =====================
    // addAllAsWinners tests
    // =====================

    @Nested
    @DisplayName("addAllAsWinners")
    class AddAllAsWinnersTests {

        @Test
        @DisplayName("Should add all active players as winners with PARTICIPATION award")
        void addAllAsWinners_Success() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findStandingsByTournamentId(1L))
                    .thenReturn(List.of(whitePlayer, blackPlayer));
            when(winnerRepository.save(any(TournamentWinner.class))).thenAnswer(invocation -> {
                TournamentWinner w = invocation.getArgument(0);
                w.setId(System.currentTimeMillis());
                return w;
            });

            // Act
            List<WinnerResponse> response = gameService.addAllAsWinners(1L, null, 1L);

            // Assert
            assertThat(response).hasSize(2);
            assertThat(response.get(0).getAwardType()).isEqualTo(TournamentWinner.AwardType.PARTICIPATION);
            assertThat(response.get(0).getAwardTitle()).isEqualTo("Участник турнира");
            verify(winnerRepository, times(2)).save(any(TournamentWinner.class));
        }

        @Test
        @DisplayName("Should use custom award title")
        void addAllAsWinners_CustomTitle() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(playerRepository.findStandingsByTournamentId(1L))
                    .thenReturn(List.of(whitePlayer));
            when(winnerRepository.save(any(TournamentWinner.class))).thenAnswer(invocation -> {
                TournamentWinner w = invocation.getArgument(0);
                w.setId(1L);
                return w;
            });

            // Act
            List<WinnerResponse> response = gameService.addAllAsWinners(1L, "Young Champion", 1L);

            // Assert
            assertThat(response.get(0).getAwardTitle()).isEqualTo("Young Champion");
        }
    }

    // =====================
    // removeWinner tests
    // =====================

    @Nested
    @DisplayName("removeWinner")
    class RemoveWinnerTests {

        @Test
        @DisplayName("Should remove winner successfully")
        void removeWinner_Success() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .id(1L)
                    .tournament(tournament)
                    .player(whitePlayer)
                    .awardType(TournamentWinner.AwardType.PLACE)
                    .place(1)
                    .build();

            when(winnerRepository.findById(1L)).thenReturn(Optional.of(winner));

            // Act
            gameService.removeWinner(1L, 1L, 1L);

            // Assert
            verify(winnerRepository).delete(winner);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void removeWinner_NotOrganizer() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .id(1L)
                    .tournament(tournament)
                    .player(whitePlayer)
                    .build();

            when(winnerRepository.findById(1L)).thenReturn(Optional.of(winner));

            // Act & Assert
            assertThatThrownBy(() -> gameService.removeWinner(1L, 1L, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can remove winners");
        }
    }

    // =====================
    // getWinners tests
    // =====================

    @Nested
    @DisplayName("getWinners")
    class GetWinnersTests {

        @Test
        @DisplayName("Should return ordered list of winners")
        void getWinners_ReturnsOrderedList() {
            // Arrange
            TournamentWinner winner1 = TournamentWinner.builder()
                    .id(1L)
                    .tournament(tournament)
                    .player(whitePlayer)
                    .place(1)
                    .awardType(TournamentWinner.AwardType.PLACE)
                    .awardedAt(LocalDateTime.now())
                    .build();

            TournamentWinner winner2 = TournamentWinner.builder()
                    .id(2L)
                    .tournament(tournament)
                    .player(blackPlayer)
                    .place(2)
                    .awardType(TournamentWinner.AwardType.PLACE)
                    .awardedAt(LocalDateTime.now())
                    .build();

            when(tournamentRepository.existsById(1L)).thenReturn(true);
            when(winnerRepository.findByTournamentIdOrdered(1L))
                    .thenReturn(List.of(winner1, winner2));

            // Act
            List<WinnerResponse> response = gameService.getWinners(1L);

            // Assert
            assertThat(response).hasSize(2);
            assertThat(response.get(0).getPlace()).isEqualTo(1);
            assertThat(response.get(1).getPlace()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw TournamentNotFoundException when tournament not found")
        void getWinners_TournamentNotFound() {
            // Arrange
            when(tournamentRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> gameService.getWinners(999L))
                    .isInstanceOf(TournamentNotFoundException.class);
        }
    }

    // =====================
    // deleteGame tests
    // =====================

    @Nested
    @DisplayName("deleteGame")
    class DeleteGameTests {

        @Test
        @DisplayName("Should delete scheduled game successfully")
        void deleteGame_Success() {
            // Arrange
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

            // Act
            gameService.deleteGame(1L, 1L, 1L);

            // Assert
            verify(gameRepository).delete(game);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when trying to delete finished game")
        void deleteGame_FinishedGame() {
            // Arrange
            game.setStatus(Game.GameStatus.FINISHED);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

            // Act & Assert
            assertThatThrownBy(() -> gameService.deleteGame(1L, 1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete finished game");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void deleteGame_NotOrganizer() {
            // Arrange
            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

            // Act & Assert
            assertThatThrownBy(() -> gameService.deleteGame(1L, 1L, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can delete games");
        }
    }
}
