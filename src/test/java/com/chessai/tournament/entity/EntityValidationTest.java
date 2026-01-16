package com.chessai.tournament.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation-тесты для Entity классов.
 * 
 * Проверяет Jakarta Validation аннотации (@NotNull, @Min, @Max и т.д.)
 * 
 * Покрытие:
 * - TournamentPlayer: 4 тест-кейса
 * - Game: 3 тест-кейса
 * - TournamentWinner: 3 тест-кейса
 */
@DisplayName("Entity Validation Tests")
class EntityValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // =====================
    // TournamentPlayer validation tests
    // =====================

    @Nested
    @DisplayName("TournamentPlayer Validation")
    class TournamentPlayerValidationTests {

        private Tournament tournament;
        private User user;

        @BeforeEach
        void setUp() {
            tournament = Tournament.builder()
                    .id(1L)
                    .name("Test Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.IN_PROGRESS)
                    .organizerId(1L)
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();

            user = new User("testuser", "test@test.com", "password");
            user.setId(1L);
            user.setFirstName("Test");
            user.setLastName("User");
            user.addRole(Role.USER);
        }

        @Test
        @DisplayName("Should have no violations when valid entity")
        void tournamentPlayer_ValidEntity() {
            // Arrange
            TournamentPlayer player = TournamentPlayer.builder()
                    .tournament(tournament)
                    .user(user)
                    .score(BigDecimal.ZERO)
                    .gamesPlayed(0)
                    .wins(0)
                    .draws(0)
                    .losses(0)
                    .ratingAtRegistration(1500)
                    .status(TournamentPlayer.PlayerStatus.REGISTERED)
                    .build();

            // Act
            Set<ConstraintViolation<TournamentPlayer>> violations = validator.validate(player);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should have violation when rating is negative")
        void tournamentPlayer_NegativeRating() {
            // Arrange
            TournamentPlayer player = TournamentPlayer.builder()
                    .tournament(tournament)
                    .user(user)
                    .ratingAtRegistration(-100) // Invalid: negative rating
                    .build();

            // Act
            Set<ConstraintViolation<TournamentPlayer>> violations = validator.validate(player);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("ratingAtRegistration") &&
                    v.getMessage().contains("cannot be negative"));
        }

        @Test
        @DisplayName("Should have violation when tournament is null")
        void tournamentPlayer_NullTournament() {
            // Arrange
            TournamentPlayer player = TournamentPlayer.builder()
                    .tournament(null) // Invalid: null tournament
                    .user(user)
                    .build();

            // Act
            Set<ConstraintViolation<TournamentPlayer>> violations = validator.validate(player);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("tournament"));
        }

        @Test
        @DisplayName("Should have violation when user is null")
        void tournamentPlayer_NullUser() {
            // Arrange
            TournamentPlayer player = TournamentPlayer.builder()
                    .tournament(tournament)
                    .user(null) // Invalid: null user
                    .build();

            // Act
            Set<ConstraintViolation<TournamentPlayer>> violations = validator.validate(player);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("user"));
        }
    }

    // =====================
    // Game validation tests
    // =====================

    @Nested
    @DisplayName("Game Validation")
    class GameValidationTests {

        private Tournament tournament;
        private TournamentPlayer whitePlayer;
        private TournamentPlayer blackPlayer;

        @BeforeEach
        void setUp() {
            User user1 = new User("white", "white@test.com", "password");
            user1.setId(1L);
            user1.setFirstName("White");
            user1.setLastName("Player");
            user1.addRole(Role.USER);

            User user2 = new User("black", "black@test.com", "password");
            user2.setId(2L);
            user2.setFirstName("Black");
            user2.setLastName("Player");
            user2.addRole(Role.USER);

            tournament = Tournament.builder()
                    .id(1L)
                    .name("Test Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.IN_PROGRESS)
                    .organizerId(1L)
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();

            whitePlayer = TournamentPlayer.builder()
                    .id(1L)
                    .tournament(tournament)
                    .user(user1)
                    .build();

            blackPlayer = TournamentPlayer.builder()
                    .id(2L)
                    .tournament(tournament)
                    .user(user2)
                    .build();
        }

        @Test
        @DisplayName("Should have no violations when valid entity")
        void game_ValidEntity() {
            // Arrange
            Game game = Game.builder()
                    .tournament(tournament)
                    .whitePlayer(whitePlayer)
                    .blackPlayer(blackPlayer)
                    .roundNumber(1)
                    .boardNumber(1)
                    .status(Game.GameStatus.SCHEDULED)
                    .build();

            // Act
            Set<ConstraintViolation<Game>> violations = validator.validate(game);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should have violation when roundNumber is zero")
        void game_RoundNumberZero() {
            // Arrange
            Game game = Game.builder()
                    .tournament(tournament)
                    .whitePlayer(whitePlayer)
                    .blackPlayer(blackPlayer)
                    .roundNumber(0) // Invalid: must be at least 1
                    .build();

            // Act
            Set<ConstraintViolation<Game>> violations = validator.validate(game);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("roundNumber") &&
                    v.getMessage().contains("at least 1"));
        }

        @Test
        @DisplayName("Should have violation when tournament is null")
        void game_NullTournament() {
            // Arrange
            Game game = Game.builder()
                    .tournament(null) // Invalid: null tournament
                    .whitePlayer(whitePlayer)
                    .blackPlayer(blackPlayer)
                    .roundNumber(1)
                    .build();

            // Act
            Set<ConstraintViolation<Game>> violations = validator.validate(game);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("tournament"));
        }

        @Test
        @DisplayName("Should have violation when whitePlayer is null")
        void game_NullWhitePlayer() {
            // Arrange
            Game game = Game.builder()
                    .tournament(tournament)
                    .whitePlayer(null) // Invalid: null white player
                    .blackPlayer(blackPlayer)
                    .roundNumber(1)
                    .build();

            // Act
            Set<ConstraintViolation<Game>> violations = validator.validate(game);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("whitePlayer"));
        }

        @Test
        @DisplayName("Should have violation when blackPlayer is null")
        void game_NullBlackPlayer() {
            // Arrange
            Game game = Game.builder()
                    .tournament(tournament)
                    .whitePlayer(whitePlayer)
                    .blackPlayer(null) // Invalid: null black player
                    .roundNumber(1)
                    .build();

            // Act
            Set<ConstraintViolation<Game>> violations = validator.validate(game);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("blackPlayer"));
        }
    }

    // =====================
    // TournamentWinner validation tests
    // =====================

    @Nested
    @DisplayName("TournamentWinner Validation")
    class TournamentWinnerValidationTests {

        private Tournament tournament;
        private TournamentPlayer player;

        @BeforeEach
        void setUp() {
            User user = new User("winner", "winner@test.com", "password");
            user.setId(1L);
            user.setFirstName("Winner");
            user.setLastName("Player");
            user.addRole(Role.USER);

            tournament = Tournament.builder()
                    .id(1L)
                    .name("Test Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.COMPLETED)
                    .organizerId(1L)
                    .startDate(LocalDateTime.now().minusDays(7))
                    .build();

            player = TournamentPlayer.builder()
                    .id(1L)
                    .tournament(tournament)
                    .user(user)
                    .build();
        }

        @Test
        @DisplayName("Should have no violations when valid entity")
        void tournamentWinner_ValidEntity() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .tournament(tournament)
                    .player(player)
                    .place(1)
                    .awardType(TournamentWinner.AwardType.PLACE)
                    .awardTitle("1st Place")
                    .prizeAmount(1000)
                    .awardedAt(LocalDateTime.now())
                    .build();

            // Act
            Set<ConstraintViolation<TournamentWinner>> violations = validator.validate(winner);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should have violation when place is zero")
        void tournamentWinner_PlaceZero() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .tournament(tournament)
                    .player(player)
                    .place(0) // Invalid: must be at least 1
                    .awardType(TournamentWinner.AwardType.PLACE)
                    .build();

            // Act
            Set<ConstraintViolation<TournamentWinner>> violations = validator.validate(winner);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("place") &&
                    v.getMessage().contains("at least 1"));
        }

        @Test
        @DisplayName("Should have violation when tournament is null")
        void tournamentWinner_NullTournament() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .tournament(null) // Invalid: null tournament
                    .player(player)
                    .place(1)
                    .build();

            // Act
            Set<ConstraintViolation<TournamentWinner>> violations = validator.validate(winner);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("tournament"));
        }

        @Test
        @DisplayName("Should have violation when player is null")
        void tournamentWinner_NullPlayer() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .tournament(tournament)
                    .player(null) // Invalid: null player
                    .place(1)
                    .build();

            // Act
            Set<ConstraintViolation<TournamentWinner>> violations = validator.validate(winner);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("player"));
        }

        @Test
        @DisplayName("Should have violation when prizeAmount is negative")
        void tournamentWinner_NegativePrizeAmount() {
            // Arrange
            TournamentWinner winner = TournamentWinner.builder()
                    .tournament(tournament)
                    .player(player)
                    .place(1)
                    .prizeAmount(-100) // Invalid: negative prize
                    .build();

            // Act
            Set<ConstraintViolation<TournamentWinner>> violations = validator.validate(winner);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("prizeAmount") &&
                    v.getMessage().contains("cannot be negative"));
        }
    }

    // =====================
    // Tournament validation tests (bonus)
    // =====================

    @Nested
    @DisplayName("Tournament Validation")
    class TournamentValidationTests {

        @Test
        @DisplayName("Should have no violations when valid entity")
        void tournament_ValidEntity() {
            // Arrange
            Tournament tournament = Tournament.builder()
                    .name("Valid Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(1L)
                    .maxParticipants(16)
                    .minRating(1000)
                    .maxRating(2000)
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();

            // Act
            Set<ConstraintViolation<Tournament>> violations = validator.validate(tournament);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should have violation when name is blank")
        void tournament_BlankName() {
            // Arrange
            Tournament tournament = Tournament.builder()
                    .name("") // Invalid: blank name
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(1L)
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();

            // Act
            Set<ConstraintViolation<Tournament>> violations = validator.validate(tournament);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("Should have violation when maxParticipants exceeds 1000")
        void tournament_MaxParticipantsExceedsLimit() {
            // Arrange
            Tournament tournament = Tournament.builder()
                    .name("Big Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(1L)
                    .maxParticipants(1500) // Invalid: max is 1000
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();

            // Act
            Set<ConstraintViolation<Tournament>> violations = validator.validate(tournament);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("maxParticipants") &&
                    v.getMessage().contains("1000"));
        }

        @Test
        @DisplayName("Should have violation when minRating is negative")
        void tournament_NegativeMinRating() {
            // Arrange
            Tournament tournament = Tournament.builder()
                    .name("Test Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(1L)
                    .minRating(-100) // Invalid: negative rating
                    .startDate(LocalDateTime.now().plusDays(7))
                    .build();

            // Act
            Set<ConstraintViolation<Tournament>> violations = validator.validate(tournament);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> 
                    v.getPropertyPath().toString().equals("minRating") &&
                    v.getMessage().contains("cannot be negative"));
        }
    }
}
