package com.chessai.tournament.controller;

import com.chessai.tournament.config.WebMvcTestSecurityConfig;
import com.chessai.tournament.dto.TournamentPlayerRequest;
import com.chessai.tournament.dto.TournamentPlayerResponse;
import com.chessai.tournament.entity.Role;
import com.chessai.tournament.entity.TournamentPlayer;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.service.TournamentPlayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration-тесты для TournamentPlayerController.
 * 
 * Покрытие: 100% endpoints
 * - POST /api/tournaments/{id}/players: 4 тест-кейса
 * - GET /api/tournaments/{id}/players: 1 тест-кейс
 * - GET /api/tournaments/{id}/players/{playerId}: 2 тест-кейса
 * - DELETE /api/tournaments/{id}/players/{playerId}: 2 тест-кейса
 * - PATCH /api/tournaments/{id}/players/{playerId}/disqualify: 2 тест-кейса
 */
@WebMvcTest(TournamentPlayerController.class)
@Import(WebMvcTestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("TournamentPlayerController Integration Tests")
class TournamentPlayerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TournamentPlayerService playerService;

    private TournamentPlayerRequest validRequest;
    private TournamentPlayerResponse playerResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Создаём тестового пользователя
        testUser = new User("organizer", "organizer@test.com", "password");
        testUser.setId(1L);
        testUser.addRole(Role.USER);

        validRequest = TournamentPlayerRequest.builder()
                .userId(2L)
                .ratingAtRegistration(1500)
                .build();

        playerResponse = TournamentPlayerResponse.builder()
                .id(1L)
                .tournamentId(1L)
                .tournamentName("Test Tournament")
                .userId(2L)
                .username("player1")
                .fullName("Test Player")
                .ratingAtRegistration(1500)
                .score(BigDecimal.ZERO)
                .gamesPlayed(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .status(TournamentPlayer.PlayerStatus.REGISTERED)
                .rank(1)
                .registeredAt(LocalDateTime.now())
                .build();
    }

    // =====================
    // POST /api/tournaments/{id}/players
    // =====================

    @Nested
    @DisplayName("POST /api/tournaments/{id}/players")
    class RegisterPlayerTests {

        @Test
        @DisplayName("Should return 201 Created when valid request")
        void registerPlayer_Returns201() throws Exception {
            // Arrange
            when(playerService.registerPlayer(anyLong(), any(TournamentPlayerRequest.class), anyLong()))
                    .thenReturn(playerResponse);

            // Act & Assert
            mockMvc.perform(post("/api/tournaments/1/players")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.userId").value(2))
                    .andExpect(jsonPath("$.tournamentId").value(1))
                    .andExpect(jsonPath("$.status").value("REGISTERED"));

            verify(playerService).registerPlayer(eq(1L), any(TournamentPlayerRequest.class), eq(1L));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when missing userId")
        void registerPlayer_Returns400_MissingUserId() throws Exception {
            // Arrange
            TournamentPlayerRequest invalidRequest = TournamentPlayerRequest.builder()
                    .ratingAtRegistration(1500)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/tournaments/1/players")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(playerService, never()).registerPlayer(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("Should return 404 Not Found when tournament not found")
        void registerPlayer_Returns404_TournamentNotFound() throws Exception {
            // Arrange
            when(playerService.registerPlayer(anyLong(), any(TournamentPlayerRequest.class), anyLong()))
                    .thenThrow(new TournamentNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(post("/api/tournaments/999/players")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 500 when already registered (IllegalStateException)")
        void registerPlayer_Returns500_AlreadyRegistered() throws Exception {
            // Arrange - IllegalStateException is handled as 500 by GlobalExceptionHandler
            when(playerService.registerPlayer(anyLong(), any(TournamentPlayerRequest.class), anyLong()))
                    .thenThrow(new IllegalStateException("User is already registered"));

            // Act & Assert
            mockMvc.perform(post("/api/tournaments/1/players")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not authenticated")
        void registerPlayer_Returns403_Unauthenticated() throws Exception {
            // Act & Assert - Spring Security returns 403 by default when no authentication
            mockMvc.perform(post("/api/tournaments/1/players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());

            verify(playerService, never()).registerPlayer(anyLong(), any(), anyLong());
        }
    }

    // =====================
    // GET /api/tournaments/{id}/players
    // =====================

    @Nested
    @DisplayName("GET /api/tournaments/{id}/players")
    class GetPlayersTests {

        @Test
        @DisplayName("Should return 200 OK with list sorted by score")
        void getPlayers_Returns200() throws Exception {
            // Arrange
            TournamentPlayerResponse player2 = TournamentPlayerResponse.builder()
                    .id(2L)
                    .tournamentId(1L)
                    .userId(3L)
                    .username("player2")
                    .score(new BigDecimal("2.5"))
                    .rank(1)
                    .status(TournamentPlayer.PlayerStatus.ACTIVE)
                    .build();

            playerResponse.setRank(2);
            when(playerService.getPlayers(1L)).thenReturn(List.of(player2, playerResponse));

            // Act & Assert
            mockMvc.perform(get("/api/tournaments/1/players")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].score").value(2.5))
                    .andExpect(jsonPath("$[1].rank").value(2));
        }

        @Test
        @DisplayName("Should return 404 Not Found when tournament not found")
        void getPlayers_Returns404() throws Exception {
            // Arrange
            when(playerService.getPlayers(999L))
                    .thenThrow(new TournamentNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(get("/api/tournaments/999/players")
                            .with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    // =====================
    // GET /api/tournaments/{id}/players/{playerId}
    // =====================

    @Nested
    @DisplayName("GET /api/tournaments/{id}/players/{playerId}")
    class GetPlayerTests {

        @Test
        @DisplayName("Should return 200 OK with player and rank")
        void getPlayer_Returns200() throws Exception {
            // Arrange
            when(playerService.getPlayer(1L, 1L)).thenReturn(playerResponse);

            // Act & Assert
            mockMvc.perform(get("/api/tournaments/1/players/1")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.rank").value(1))
                    .andExpect(jsonPath("$.username").value("player1"));
        }

        @Test
        @DisplayName("Should return 500 when player not found (IllegalArgumentException)")
        void getPlayer_Returns500_NotFound() throws Exception {
            // Arrange - IllegalArgumentException is handled as 500 by GlobalExceptionHandler
            when(playerService.getPlayer(1L, 999L))
                    .thenThrow(new IllegalArgumentException("Player not found"));

            // Act & Assert
            mockMvc.perform(get("/api/tournaments/1/players/999")
                            .with(user(testUser)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =====================
    // DELETE /api/tournaments/{id}/players/{playerId}
    // =====================

    @Nested
    @DisplayName("DELETE /api/tournaments/{id}/players/{playerId}")
    class WithdrawPlayerTests {

        @Test
        @DisplayName("Should return 204 No Content when successful")
        void withdrawPlayer_Returns204() throws Exception {
            // Arrange
            doNothing().when(playerService).withdrawPlayer(anyLong(), anyLong(), anyLong());

            // Act & Assert
            mockMvc.perform(delete("/api/tournaments/1/players/1")
                            .with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(playerService).withdrawPlayer(eq(1L), eq(1L), eq(1L));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not authorized")
        void withdrawPlayer_Returns403() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("Only organizer or the player can withdraw"))
                    .when(playerService).withdrawPlayer(anyLong(), anyLong(), anyLong());

            // Act & Assert
            mockMvc.perform(delete("/api/tournaments/1/players/1")
                            .with(user(testUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 500 when player not found (IllegalArgumentException)")
        void withdrawPlayer_Returns500_NotFound() throws Exception {
            // Arrange - IllegalArgumentException is handled as 500 by GlobalExceptionHandler
            doThrow(new IllegalArgumentException("Player not found"))
                    .when(playerService).withdrawPlayer(anyLong(), anyLong(), anyLong());

            // Act & Assert
            mockMvc.perform(delete("/api/tournaments/1/players/999")
                            .with(user(testUser)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =====================
    // PATCH /api/tournaments/{id}/players/{playerId}/disqualify
    // =====================

    @Nested
    @DisplayName("PATCH /api/tournaments/{id}/players/{playerId}/disqualify")
    class DisqualifyPlayerTests {

        @Test
        @DisplayName("Should return 200 OK when organizer disqualifies")
        void disqualifyPlayer_Returns200() throws Exception {
            // Arrange
            playerResponse.setStatus(TournamentPlayer.PlayerStatus.DISQUALIFIED);
            when(playerService.disqualifyPlayer(anyLong(), anyLong(), anyLong()))
                    .thenReturn(playerResponse);

            // Act & Assert
            mockMvc.perform(patch("/api/tournaments/1/players/1/disqualify")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DISQUALIFIED"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not organizer")
        void disqualifyPlayer_Returns403() throws Exception {
            // Arrange
            when(playerService.disqualifyPlayer(anyLong(), anyLong(), anyLong()))
                    .thenThrow(new AccessDeniedException("Only organizer can disqualify"));

            // Act & Assert
            mockMvc.perform(patch("/api/tournaments/1/players/1/disqualify")
                            .with(user(testUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 500 when player not found (IllegalArgumentException)")
        void disqualifyPlayer_Returns500_NotFound() throws Exception {
            // Arrange - IllegalArgumentException is handled as 500 by GlobalExceptionHandler
            when(playerService.disqualifyPlayer(anyLong(), anyLong(), anyLong()))
                    .thenThrow(new IllegalArgumentException("Player not found"));

            // Act & Assert
            mockMvc.perform(patch("/api/tournaments/1/players/999/disqualify")
                            .with(user(testUser)))
                    .andExpect(status().isInternalServerError());
        }
    }
}
