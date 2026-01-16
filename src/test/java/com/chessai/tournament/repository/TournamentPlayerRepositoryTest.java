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
 * Repository-тесты для TournamentPlayerRepository.
 * 
 * Используется H2 in-memory база для изоляции тестов.
 * 
 * Покрытие:
 * - findByTournamentIdAndUserId: 2 тест-кейса
 * - findStandingsByTournamentId: 2 тест-кейса
 * - existsByTournamentIdAndUserId: 2 тест-кейса
 * - countActiveByTournamentId: 1 тест-кейс
 * - findByTournamentIdAndStatus: 1 тест-кейс
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("TournamentPlayerRepository Tests")
class TournamentPlayerRepositoryTest {

    @Autowired
    private TournamentPlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

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

        // Create players with different scores
        player1 = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user1)
                .score(new BigDecimal("2.5"))
                .gamesPlayed(3)
                .wins(2)
                .draws(1)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.ACTIVE)
                .ratingAtRegistration(1500)
                .build();
        player1 = entityManager.persistAndFlush(player1);

        player2 = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user2)
                .score(new BigDecimal("1.5"))
                .gamesPlayed(3)
                .wins(1)
                .draws(1)
                .losses(1)
                .status(TournamentPlayer.PlayerStatus.REGISTERED)
                .ratingAtRegistration(1600)
                .build();
        player2 = entityManager.persistAndFlush(player2);

        player3 = TournamentPlayer.builder()
                .tournament(tournament)
                .user(user3)
                .score(new BigDecimal("1.0"))
                .gamesPlayed(3)
                .wins(0)
                .draws(2)
                .losses(1)
                .status(TournamentPlayer.PlayerStatus.WITHDRAWN)
                .ratingAtRegistration(1400)
                .build();
        player3 = entityManager.persistAndFlush(player3);

        entityManager.clear();
    }

    // =====================
    // findByTournamentIdAndUserId tests
    // =====================

    @Nested
    @DisplayName("findByTournamentIdAndUserId")
    class FindByTournamentIdAndUserIdTests {

        @Test
        @DisplayName("Should return player when found")
        void findByTournamentIdAndUserId_Found() {
            // Act
            Optional<TournamentPlayer> result = playerRepository.findByTournamentIdAndUserId(
                    tournament.getId(), user1.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getUser().getId()).isEqualTo(user1.getId());
            assertThat(result.get().getTournament().getId()).isEqualTo(tournament.getId());
        }

        @Test
        @DisplayName("Should return empty Optional when not found")
        void findByTournamentIdAndUserId_NotFound() {
            // Act
            Optional<TournamentPlayer> result = playerRepository.findByTournamentIdAndUserId(
                    tournament.getId(), 999L);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // =====================
    // findStandingsByTournamentId tests
    // =====================

    @Nested
    @DisplayName("findStandingsByTournamentId")
    class FindStandingsByTournamentIdTests {

        @Test
        @DisplayName("Should return players sorted by score DESC")
        void findStandingsByTournamentId_SortedByScoreDesc() {
            // Act
            List<TournamentPlayer> standings = playerRepository.findStandingsByTournamentId(tournament.getId());

            // Assert
            assertThat(standings).hasSize(2); // player3 is WITHDRAWN, excluded
            assertThat(standings.get(0).getScore()).isEqualTo(new BigDecimal("2.5")); // player1
            assertThat(standings.get(1).getScore()).isEqualTo(new BigDecimal("1.5")); // player2
        }

        @Test
        @DisplayName("Should sort by wins when scores are equal")
        void findStandingsByTournamentId_TiebreakByWins() {
            // Arrange - make scores equal
            TournamentPlayer loadedPlayer2 = entityManager.find(TournamentPlayer.class, player2.getId());
            loadedPlayer2.setScore(new BigDecimal("2.5"));
            loadedPlayer2.setWins(1); // less wins than player1
            entityManager.flush();
            entityManager.clear();

            // Act
            List<TournamentPlayer> standings = playerRepository.findStandingsByTournamentId(tournament.getId());

            // Assert
            assertThat(standings).hasSize(2);
            // player1 has more wins (2 vs 1), should be first
            assertThat(standings.get(0).getWins()).isEqualTo(2);
            assertThat(standings.get(1).getWins()).isEqualTo(1);
        }
    }

    // =====================
    // existsByTournamentIdAndUserId tests
    // =====================

    @Nested
    @DisplayName("existsByTournamentIdAndUserId")
    class ExistsByTournamentIdAndUserIdTests {

        @Test
        @DisplayName("Should return true when player exists")
        void existsByTournamentIdAndUserId_True() {
            // Act
            boolean exists = playerRepository.existsByTournamentIdAndUserId(
                    tournament.getId(), user1.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when player does not exist")
        void existsByTournamentIdAndUserId_False() {
            // Act
            boolean exists = playerRepository.existsByTournamentIdAndUserId(
                    tournament.getId(), 999L);

            // Assert
            assertThat(exists).isFalse();
        }
    }

    // =====================
    // countActiveByTournamentId tests
    // =====================

    @Nested
    @DisplayName("countActiveByTournamentId")
    class CountActiveByTournamentIdTests {

        @Test
        @DisplayName("Should count only REGISTERED and ACTIVE status players")
        void countActiveByTournamentId() {
            // Act
            long count = playerRepository.countActiveByTournamentId(tournament.getId());

            // Assert
            // player1 = ACTIVE, player2 = REGISTERED, player3 = WITHDRAWN
            assertThat(count).isEqualTo(2);
        }
    }

    // =====================
    // findByTournamentIdAndStatus tests
    // =====================

    @Nested
    @DisplayName("findByTournamentIdAndStatus")
    class FindByTournamentIdAndStatusTests {

        @Test
        @DisplayName("Should return players filtered by status")
        void findByTournamentIdAndStatus() {
            // Act
            List<TournamentPlayer> activePlayers = playerRepository.findByTournamentIdAndStatus(
                    tournament.getId(), TournamentPlayer.PlayerStatus.ACTIVE);
            List<TournamentPlayer> withdrawnPlayers = playerRepository.findByTournamentIdAndStatus(
                    tournament.getId(), TournamentPlayer.PlayerStatus.WITHDRAWN);

            // Assert
            assertThat(activePlayers).hasSize(1);
            assertThat(activePlayers.get(0).getUser().getId()).isEqualTo(user1.getId());

            assertThat(withdrawnPlayers).hasSize(1);
            assertThat(withdrawnPlayers.get(0).getUser().getId()).isEqualTo(user3.getId());
        }
    }

    // =====================
    // findByTournamentId tests
    // =====================

    @Nested
    @DisplayName("findByTournamentId")
    class FindByTournamentIdTests {

        @Test
        @DisplayName("Should return all players in tournament")
        void findByTournamentId() {
            // Act
            List<TournamentPlayer> players = playerRepository.findByTournamentId(tournament.getId());

            // Assert
            assertThat(players).hasSize(3); // includes all statuses
        }
    }

    // =====================
    // findByUserId tests
    // =====================

    @Nested
    @DisplayName("findByUserId")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should return all tournaments for user")
        void findByUserId() {
            // Act
            List<TournamentPlayer> userTournaments = playerRepository.findByUserId(user1.getId());

            // Assert
            assertThat(userTournaments).hasSize(1);
            assertThat(userTournaments.get(0).getTournament().getId()).isEqualTo(tournament.getId());
        }
    }
}
