package com.chessai.tournament.service;

import com.chessai.tournament.dto.TournamentPlayerRequest;
import com.chessai.tournament.dto.TournamentPlayerResponse;
import com.chessai.tournament.entity.Role;
import com.chessai.tournament.entity.Tournament;
import com.chessai.tournament.entity.TournamentFormat;
import com.chessai.tournament.entity.TournamentPlayer;
import com.chessai.tournament.entity.TournamentStatus;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.repository.TournamentPlayerRepository;
import com.chessai.tournament.repository.TournamentRepository;
import com.chessai.tournament.repository.UserRepository;
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
 * Unit-тесты для TournamentPlayerService.
 * 
 * Покрытие: 100% методов сервиса
 * - registerPlayer: 7 тест-кейсов
 * - withdrawPlayer: 4 тест-кейса
 * - getPlayers: 2 тест-кейса
 * - getPlayer: 2 тест-кейса
 * - disqualifyPlayer: 2 тест-кейса
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentPlayerService Unit Tests")
class TournamentPlayerServiceTest {

    @Mock
    private TournamentPlayerRepository playerRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TournamentPlayerService playerService;

    // Test data
    private Tournament tournament;
    private User user;
    private TournamentPlayer player;
    private TournamentPlayerRequest request;

    @BeforeEach
    void setUp() {
        // Tournament: id=1, status=REGISTRATION_OPEN, organizerId=1, maxParticipants=16
        tournament = Tournament.builder()
                .id(1L)
                .name("Test Tournament")
                .format(TournamentFormat.SWISS)
                .status(TournamentStatus.REGISTRATION_OPEN)
                .organizerId(1L)
                .maxParticipants(16)
                .currentParticipants(5)
                .minRating(1000)
                .maxRating(2000)
                .startDate(LocalDateTime.now().plusDays(7))
                .build();

        // User: id=2, username="player1"
        user = new User("player1", "player1@test.com", "password123");
        user.setId(2L);
        user.setFirstName("Test");
        user.setLastName("Player");
        user.addRole(Role.USER);

        // TournamentPlayer: id=1, tournament, user, score=0, status=REGISTERED
        player = TournamentPlayer.builder()
                .id(1L)
                .tournament(tournament)
                .user(user)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.REGISTERED)
                .ratingAtRegistration(1500)
                .build();

        // Request
        request = TournamentPlayerRequest.builder()
                .userId(2L)
                .ratingAtRegistration(1500)
                .build();
    }

    // =====================
    // registerPlayer tests
    // =====================

    @Nested
    @DisplayName("registerPlayer")
    class RegisterPlayerTests {

        @Test
        @DisplayName("Should register player successfully when valid input")
        void registerPlayer_Success() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(playerRepository.existsByTournamentIdAndUserId(1L, 2L)).thenReturn(false);
            when(playerRepository.save(any(TournamentPlayer.class))).thenAnswer(invocation -> {
                TournamentPlayer p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

            // Act
            TournamentPlayerResponse response = playerService.registerPlayer(1L, request, 1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(2L);
            assertThat(response.getTournamentId()).isEqualTo(1L);
            assertThat(response.getRatingAtRegistration()).isEqualTo(1500);
            verify(playerRepository).save(any(TournamentPlayer.class));
            verify(tournamentRepository).save(tournament);
        }

        @Test
        @DisplayName("Should throw TournamentNotFoundException when tournament not found")
        void registerPlayer_TournamentNotFound() {
            // Arrange
            when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(999L, request, 1L))
                    .isInstanceOf(TournamentNotFoundException.class);
            verify(playerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when registration is closed")
        void registerPlayer_RegistrationClosed() {
            // Arrange
            tournament.setStatus(TournamentStatus.IN_PROGRESS);
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(1L, request, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Registration is not open");
            verify(playerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when tournament is full")
        void registerPlayer_TournamentFull() {
            // Arrange
            tournament.setCurrentParticipants(16); // maxParticipants = 16
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(1L, request, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Tournament is full");
            verify(playerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when user already registered")
        void registerPlayer_AlreadyRegistered() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(playerRepository.existsByTournamentIdAndUserId(1L, 2L)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(1L, request, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already registered");
            verify(playerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when rating below minimum")
        void registerPlayer_RatingBelowMinimum() {
            // Arrange
            request.setRatingAtRegistration(500); // minRating = 1000
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(playerRepository.existsByTournamentIdAndUserId(1L, 2L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(1L, request, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("rating")
                    .hasMessageContaining("does not meet tournament requirements");
            verify(playerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when rating above maximum")
        void registerPlayer_RatingAboveMaximum() {
            // Arrange
            request.setRatingAtRegistration(2500); // maxRating = 2000
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(playerRepository.existsByTournamentIdAndUserId(1L, 2L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(1L, request, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("rating")
                    .hasMessageContaining("does not meet tournament requirements");
            verify(playerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user not found")
        void registerPlayer_UserNotFound() {
            // Arrange
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> playerService.registerPlayer(1L, request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
            verify(playerRepository, never()).save(any());
        }
    }

    // =====================
    // withdrawPlayer tests
    // =====================

    @Nested
    @DisplayName("withdrawPlayer")
    class WithdrawPlayerTests {

        @Test
        @DisplayName("Should allow organizer to withdraw player before tournament starts")
        void withdrawPlayer_OrganizerBeforeStart() {
            // Arrange
            tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act
            playerService.withdrawPlayer(1L, 1L, 1L); // organizerId = 1

            // Assert
            verify(playerRepository).delete(player);
            verify(tournamentRepository).save(tournament);
        }

        @Test
        @DisplayName("Should allow player to withdraw self")
        void withdrawPlayer_SelfWithdraw() {
            // Arrange
            tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act
            playerService.withdrawPlayer(1L, 1L, 2L); // userId = 2 (player's user)

            // Assert
            verify(playerRepository).delete(player);
        }

        @Test
        @DisplayName("Should set status WITHDRAWN after tournament starts")
        void withdrawPlayer_AfterTournamentStarts() {
            // Arrange
            tournament.setStatus(TournamentStatus.IN_PROGRESS);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act
            playerService.withdrawPlayer(1L, 1L, 1L);

            // Assert
            assertThat(player.getStatus()).isEqualTo(TournamentPlayer.PlayerStatus.WITHDRAWN);
            verify(playerRepository).save(player);
            verify(playerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer and not self")
        void withdrawPlayer_NotAuthorized() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act & Assert
            assertThatThrownBy(() -> playerService.withdrawPlayer(1L, 1L, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer or the player can withdraw");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player not found")
        void withdrawPlayer_PlayerNotFound() {
            // Arrange
            when(playerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> playerService.withdrawPlayer(1L, 999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Player not found");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player from different tournament")
        void withdrawPlayer_DifferentTournament() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act & Assert
            assertThatThrownBy(() -> playerService.withdrawPlayer(999L, 1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Player does not belong to this tournament");
        }
    }

    // =====================
    // getPlayers tests
    // =====================

    @Nested
    @DisplayName("getPlayers")
    class GetPlayersTests {

        @Test
        @DisplayName("Should return sorted list with correct ranks")
        void getPlayers_ReturnsSortedList() {
            // Arrange
            User user2 = new User("player2", "player2@test.com", "password123");
            user2.setId(3L);
            user2.setFirstName("Player");
            user2.setLastName("Two");
            
            TournamentPlayer player2 = TournamentPlayer.builder()
                    .id(2L)
                    .tournament(tournament)
                    .user(user2)
                    .score(new BigDecimal("2.5"))
                    .gamesPlayed(3)
                    .wins(2)
                    .draws(1)
                    .losses(0)
                    .status(TournamentPlayer.PlayerStatus.ACTIVE)
                    .build();

            when(tournamentRepository.existsById(1L)).thenReturn(true);
            when(playerRepository.findStandingsByTournamentId(1L))
                    .thenReturn(List.of(player2, player)); // player2 has higher score

            // Act
            List<TournamentPlayerResponse> result = playerService.getPlayers(1L);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(0).getScore()).isEqualTo(new BigDecimal("2.5"));
            assertThat(result.get(1).getRank()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw TournamentNotFoundException when tournament not found")
        void getPlayers_TournamentNotFound() {
            // Arrange
            when(tournamentRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> playerService.getPlayers(999L))
                    .isInstanceOf(TournamentNotFoundException.class);
        }
    }

    // =====================
    // getPlayer tests
    // =====================

    @Nested
    @DisplayName("getPlayer")
    class GetPlayerTests {

        @Test
        @DisplayName("Should return player with correct rank")
        void getPlayer_ReturnsWithRank() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            when(playerRepository.findStandingsByTournamentId(1L)).thenReturn(List.of(player));

            // Act
            TournamentPlayerResponse result = playerService.getPlayer(1L, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRank()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player not found")
        void getPlayer_NotFound() {
            // Arrange
            when(playerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> playerService.getPlayer(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Player not found");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player from different tournament")
        void getPlayer_DifferentTournament() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act & Assert
            assertThatThrownBy(() -> playerService.getPlayer(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Player does not belong to this tournament");
        }
    }

    // =====================
    // disqualifyPlayer tests
    // =====================

    @Nested
    @DisplayName("disqualifyPlayer")
    class DisqualifyPlayerTests {

        @Test
        @DisplayName("Should disqualify player when organizer")
        void disqualifyPlayer_Success() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            when(playerRepository.save(any(TournamentPlayer.class))).thenReturn(player);

            // Act
            TournamentPlayerResponse result = playerService.disqualifyPlayer(1L, 1L, 1L);

            // Assert
            assertThat(player.getStatus()).isEqualTo(TournamentPlayer.PlayerStatus.DISQUALIFIED);
            verify(playerRepository).save(player);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when not organizer")
        void disqualifyPlayer_NotOrganizer() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            // Act & Assert
            assertThatThrownBy(() -> playerService.disqualifyPlayer(1L, 1L, 999L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only organizer can disqualify");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when player not found")
        void disqualifyPlayer_PlayerNotFound() {
            // Arrange
            when(playerRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> playerService.disqualifyPlayer(1L, 999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Player not found");
        }
    }

    // =====================
    // isRegistered tests
    // =====================

    @Nested
    @DisplayName("isRegistered")
    class IsRegisteredTests {

        @Test
        @DisplayName("Should return true when user is registered")
        void isRegistered_True() {
            // Arrange
            when(playerRepository.existsByTournamentIdAndUserId(1L, 2L)).thenReturn(true);

            // Act
            boolean result = playerService.isRegistered(1L, 2L);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not registered")
        void isRegistered_False() {
            // Arrange
            when(playerRepository.existsByTournamentIdAndUserId(1L, 999L)).thenReturn(false);

            // Act
            boolean result = playerService.isRegistered(1L, 999L);

            // Assert
            assertThat(result).isFalse();
        }
    }
}
