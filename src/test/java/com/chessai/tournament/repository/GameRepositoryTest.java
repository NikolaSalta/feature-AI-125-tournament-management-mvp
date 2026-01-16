package com.chessai.tournament.repository;

import com.chessai.tournament.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-тесты для GameRepository.
 * 
 * Используется H2 in-memory база для изоляции тестов.
 * 
 * Покрытие:
 * - findByTournamentIdAndRoundNumber: 1 тест-кейс
 * - findByTournamentIdAndPlayerId: 1 тест-кейс
 * - existsByTournamentIdAndRoundAndPlayers: 1 тест-кейс
 * - findMaxRoundByTournamentId: 2 тест-кейса
 * - countFinishedByTournamentIdAndRound: 1 тест-кейс
 * - findUnfinishedByTournamentId: 1 тест-кейс
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("GameRepository Tests")
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentPlayerRepository playerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Tournament tournament;
    private User user1;
    private User user2;
    private User user3;
    private TournamentPlayer player1;
    private TournamentPlayer player2;
    private TournamentPlayer player3;
    private Game game1;
    private Game game2;
    private Game game3;

    @BeforeEach
    void setUp() {
        // Create users
        user1 = new User("player1", "player1@test.com", "password123");
        user1.setFirstName("Player");
        user1.setLastName("One");
        user1.addRole(Role.USER);
        user1 = entityManager.persistAndFlush(user1);

        user2 = new User("player2", "player2@test.com", "password123");
        user2.setFirstName("Player");
        user2.setLastName("Two");
        user2.addRole(Role.USER);
        user2 = entityManager.persistAndFlush(user2);

        user3 = new User("player3", "player3@test.com", "password123");
        user3.setFirstName("Player");
        user3.setLastName("Three");
        user3.addRole(Role.USER);
        user3 = entityManager.persistAndFlush(user3);

        // Create tournament
        tournament = Tournament.builder()
                .name("Test Tournament")
                .format(TournamentFormat.SWISS)
                .status(TournamentStatus.IN_PROGRESS)
                .organizerId(user1.getId())
                .maxParticipants(16)
                .currentParticipants(3)
                .startDate(LocalDateTime.now().plusDays(7))
                .build();
        tournament = entityManager.persistAndFlush(tournament);

        // Create players
        player1 = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user1)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.ACTIVE)
                .ratingAtRegistration(1500)
                .build();
        player1 = entityManager.persistAndFlush(player1);

        player2 = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user2)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.ACTIVE)
                .ratingAtRegistration(1600)
                .build();
        player2 = entityManager.persistAndFlush(player2);

        player3 = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user3)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.ACTIVE)
                .ratingAtRegistration(1400)
                .build();
        player3 = entityManager.persistAndFlush(player3);

        // Create games
        // Round 1: player1 vs player2 (FINISHED)
        game1 = Game.builder()
                .tournament(tournament)
                .whitePlayer(player1)
                .blackPlayer(player2)
                .roundNumber(1)
                .boardNumber(1)
                .status(Game.GameStatus.FINISHED)
                .result(Game.GameResult.WHITE_WINS)
                .whiteScore(BigDecimal.ONE)
                .blackScore(BigDecimal.ZERO)
                .build();
        game1 = entityManager.persistAndFlush(game1);

        // Round 2: player1 vs player3 (SCHEDULED)
        game2 = Game.builder()
                .tournament(tournament)
                .whitePlayer(player1)
                .blackPlayer(player3)
                .roundNumber(2)
                .boardNumber(1)
                .status(Game.GameStatus.SCHEDULED)
                .build();
        game2 = entityManager.persistAndFlush(game2);

        // Round 2: player2 vs player3 (IN_PROGRESS)
        game3 = Game.builder()
                .tournament(tournament)
                .whitePlayer(player2)
                .blackPlayer(player3)
                .roundNumber(2)
                .boardNumber(2)
                .status(Game.GameStatus.IN_PROGRESS)
                .build();
        game3 = entityManager.persistAndFlush(game3);

        entityManager.clear();
    }

    // =====================
    // findByTournamentIdAndRoundNumber tests
    // =====================

    @Nested
    @DisplayName("findByTournamentIdAndRoundNumber")
    class FindByTournamentIdAndRoundNumberTests {

        @Test
        @DisplayName("Should return games for specific round")
        void findByTournamentIdAndRoundNumber() {
            // Act
            List<Game> round1Games = gameRepository.findByTournamentIdAndRoundNumber(
                    tournament.getId(), 1);
            List<Game> round2Games = gameRepository.findByTournamentIdAndRoundNumber(
                    tournament.getId(), 2);

            // Assert
            assertThat(round1Games).hasSize(1);
            assertThat(round1Games.get(0).getWhitePlayer().getId()).isEqualTo(player1.getId());

            assertThat(round2Games).hasSize(2);
        }
    }

    // =====================
    // findByTournamentIdAndPlayerId tests
    // =====================

    @Nested
    @DisplayName("findByTournamentIdAndPlayerId")
    class FindByTournamentIdAndPlayerIdTests {

        @Test
        @DisplayName("Should return games where player is white OR black")
        void findByTournamentIdAndPlayerId() {
            // Act
            List<Game> player1Games = gameRepository.findByTournamentIdAndPlayerId(
                    tournament.getId(), player1.getId());
            List<Game> player3Games = gameRepository.findByTournamentIdAndPlayerId(
                    tournament.getId(), player3.getId());

            // Assert
            assertThat(player1Games).hasSize(2); // game1 (white) + game2 (white)
            assertThat(player3Games).hasSize(2); // game2 (black) + game3 (black)
        }
    }

    // =====================
    // existsByTournamentIdAndRoundAndPlayers tests
    // =====================

    @Nested
    @DisplayName("existsByTournamentIdAndRoundAndPlayers")
    class ExistsByTournamentIdAndRoundAndPlayersTests {

        @Test
        @DisplayName("Should return true when game exists between players in round")
        void existsByTournamentIdAndRoundAndPlayers_True() {
            // Act
            boolean exists = gameRepository.existsByTournamentIdAndRoundAndPlayers(
                    tournament.getId(), 1, player1.getId(), player2.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return true regardless of color order")
        void existsByTournamentIdAndRoundAndPlayers_ReverseOrder() {
            // Act - reverse order (black, white)
            boolean exists = gameRepository.existsByTournamentIdAndRoundAndPlayers(
                    tournament.getId(), 1, player2.getId(), player1.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when game does not exist")
        void existsByTournamentIdAndRoundAndPlayers_False() {
            // Act
            boolean exists = gameRepository.existsByTournamentIdAndRoundAndPlayers(
                    tournament.getId(), 1, player1.getId(), player3.getId());

            // Assert
            assertThat(exists).isFalse();
        }
    }

    // =====================
    // findMaxRoundByTournamentId tests
    // =====================

    @Nested
    @DisplayName("findMaxRoundByTournamentId")
    class FindMaxRoundByTournamentIdTests {

        @Test
        @DisplayName("Should return highest round number")
        void findMaxRoundByTournamentId() {
            // Act
            Optional<Integer> maxRound = gameRepository.findMaxRoundByTournamentId(tournament.getId());

            // Assert
            assertThat(maxRound).isPresent();
            assertThat(maxRound.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty Optional when no games")
        void findMaxRoundByTournamentId_NoGames() {
            // Arrange - create tournament without games
            Tournament emptyTournament = Tournament.builder()
                    .name("Empty Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.IN_PROGRESS)
                    .organizerId(user1.getId())
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();
            emptyTournament = entityManager.persistAndFlush(emptyTournament);

            // Act
            Optional<Integer> maxRound = gameRepository.findMaxRoundByTournamentId(emptyTournament.getId());

            // Assert
            assertThat(maxRound).isEmpty();
        }
    }

    // =====================
    // countFinishedByTournamentIdAndRound tests
    // =====================

    @Nested
    @DisplayName("countFinishedByTournamentIdAndRound")
    class CountFinishedByTournamentIdAndRoundTests {

        @Test
        @DisplayName("Should count only FINISHED status games")
        void countFinishedByTournamentIdAndRound() {
            // Act
            long round1Finished = gameRepository.countFinishedByTournamentIdAndRound(
                    tournament.getId(), 1);
            long round2Finished = gameRepository.countFinishedByTournamentIdAndRound(
                    tournament.getId(), 2);

            // Assert
            assertThat(round1Finished).isEqualTo(1); // game1 is FINISHED
            assertThat(round2Finished).isEqualTo(0); // game2=SCHEDULED, game3=IN_PROGRESS
        }
    }

    // =====================
    // findUnfinishedByTournamentId tests
    // =====================

    @Nested
    @DisplayName("findUnfinishedByTournamentId")
    class FindUnfinishedByTournamentIdTests {

        @Test
        @DisplayName("Should return SCHEDULED and IN_PROGRESS games")
        void findUnfinishedByTournamentId() {
            // Act
            List<Game> unfinished = gameRepository.findUnfinishedByTournamentId(tournament.getId());

            // Assert
            assertThat(unfinished).hasSize(2); // game2 (SCHEDULED) + game3 (IN_PROGRESS)
            assertThat(unfinished).extracting(Game::getStatus)
                    .containsExactlyInAnyOrder(Game.GameStatus.SCHEDULED, Game.GameStatus.IN_PROGRESS);
        }
    }

    // =====================
    // findByTournamentId tests
    // =====================

    @Nested
    @DisplayName("findByTournamentId")
    class FindByTournamentIdTests {

        @Test
        @DisplayName("Should return all games in tournament")
        void findByTournamentId() {
            // Act
            List<Game> games = gameRepository.findByTournamentId(tournament.getId());

            // Assert
            assertThat(games).hasSize(3);
        }
    }

    // =====================
    // findByTournamentIdAndRoundNumberOrdered tests
    // =====================

    @Nested
    @DisplayName("findByTournamentIdAndRoundNumberOrdered")
    class FindByTournamentIdAndRoundNumberOrderedTests {

        @Test
        @DisplayName("Should return games sorted by board number")
        void findByTournamentIdAndRoundNumberOrdered() {
            // Act
            List<Game> round2Games = gameRepository.findByTournamentIdAndRoundNumberOrdered(
                    tournament.getId(), 2);

            // Assert
            assertThat(round2Games).hasSize(2);
            assertThat(round2Games.get(0).getBoardNumber()).isEqualTo(1); // game2
            assertThat(round2Games.get(1).getBoardNumber()).isEqualTo(2); // game3
        }
    }

    // =====================
    // findByTournamentIdAndStatus tests
    // =====================

    @Nested
    @DisplayName("findByTournamentIdAndStatus")
    class FindByTournamentIdAndStatusTests {

        @Test
        @DisplayName("Should return games filtered by status")
        void findByTournamentIdAndStatus() {
            // Act
            List<Game> finishedGames = gameRepository.findByTournamentIdAndStatus(
                    tournament.getId(), Game.GameStatus.FINISHED);
            List<Game> scheduledGames = gameRepository.findByTournamentIdAndStatus(
                    tournament.getId(), Game.GameStatus.SCHEDULED);

            // Assert
            assertThat(finishedGames).hasSize(1);
            assertThat(scheduledGames).hasSize(1);
        }
    }
}
