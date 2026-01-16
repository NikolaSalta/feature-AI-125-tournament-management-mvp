package com.chessai.tournament.controller;

import com.chessai.tournament.config.WebMvcTestSecurityConfig;
import com.chessai.tournament.dto.*;
import com.chessai.tournament.entity.Game;
import com.chessai.tournament.entity.Role;
import com.chessai.tournament.entity.TournamentWinner;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.service.GameService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration-тесты для GameController.
 */
@WebMvcTest(GameController.class)
@Import(WebMvcTestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("GameController Integration Tests")
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameService gameService;

    private GameRequest gameRequest;
    private GameResponse gameResponse;
    private GameResultRequest resultRequest;
    private StandingsResponse standingsResponse;
    private WinnerRequest winnerRequest;
    private WinnerResponse winnerResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("organizer", "organizer@test.com", "password");
        testUser.setId(1L);
        testUser.addRole(Role.USER);

        gameRequest = GameRequest.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .roundNumber(1)
                .boardNumber(1)
                .build();

        gameResponse = GameResponse.builder()
                .id(1L)
                .tournamentId(1L)
                .tournamentName("Test Tournament")
                .roundNumber(1)
                .boardNumber(1)
                .whitePlayerId(1L)
                .whiteUserId(2L)
                .whiteUsername("whitePlayer")
                .blackPlayerId(2L)
                .blackUserId(3L)
                .blackUsername("blackPlayer")
                .status(Game.GameStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .build();

        resultRequest = GameResultRequest.builder()
                .result(Game.GameResult.WHITE_WINS)
                .build();

        standingsResponse = StandingsResponse.builder()
                .tournamentId(1L)
                .tournamentName("Test Tournament")
                .tournamentStatus("IN_PROGRESS")
                .currentRound(3)
                .standings(List.of(
                        StandingsResponse.StandingsEntry.builder()
                                .rank(1)
                                .playerId(1L)
                                .userId(2L)
                                .username("whitePlayer")
                                .score(new BigDecimal("2.5"))
                                .gamesPlayed(3)
                                .wins(2)
                                .draws(1)
                                .losses(0)
                                .build()
                ))
                .build();

        winnerRequest = WinnerRequest.builder()
                .playerId(1L)
                .place(1)
                .awardType(TournamentWinner.AwardType.PLACE)
                .build();

        winnerResponse = WinnerResponse.builder()
                .id(1L)
                .tournamentId(1L)
                .tournamentName("Test Tournament")
                .playerId(1L)
                .userId(2L)
                .username("whitePlayer")
                .place(1)
                .awardType(TournamentWinner.AwardType.PLACE)
                .awardedAt(LocalDateTime.now())
                .build();
    }

    // =====================
    // POST /api/tournaments/{id}/games
    // =====================

    @Nested
    @DisplayName("POST /api/tournaments/{id}/games")
    class CreateGameTests {

        @Test
        @DisplayName("Should return 201 Created when valid request")
        void createGame_Returns201() throws Exception {
            when(gameService.createGame(anyLong(), any(GameRequest.class), anyLong()))
                    .thenReturn(gameResponse);

            mockMvc.perform(post("/api/tournaments/1/games")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(gameRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.whitePlayerId").value(1))
                    .andExpect(jsonPath("$.blackPlayerId").value(2))
                    .andExpect(jsonPath("$.roundNumber").value(1))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"));

            verify(gameService).createGame(eq(1L), any(GameRequest.class), eq(1L));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when invalid request")
        void createGame_Returns400() throws Exception {
            GameRequest invalidRequest = GameRequest.builder()
                    .whitePlayerId(1L)
                    .build();

            mockMvc.perform(post("/api/tournaments/1/games")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(gameService, never()).createGame(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not organizer")
        void createGame_Returns403() throws Exception {
            when(gameService.createGame(anyLong(), any(GameRequest.class), anyLong()))
                    .thenThrow(new AccessDeniedException("Only organizer can create games"));

            mockMvc.perform(post("/api/tournaments/1/games")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(gameRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not authenticated")
        void createGame_Returns403_Unauthenticated() throws Exception {
            mockMvc.perform(post("/api/tournaments/1/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(gameRequest)))
                    .andExpect(status().isForbidden());

            verify(gameService, never()).createGame(anyLong(), any(), anyLong());
        }
    }

    // =====================
    // PATCH /api/tournaments/{id}/games/{gameId}/result
    // =====================

    @Nested
    @DisplayName("PATCH /api/tournaments/{id}/games/{gameId}/result")
    class SetGameResultTests {

        @Test
        @DisplayName("Should return 200 OK when WHITE_WINS")
        void setGameResult_WhiteWins_Returns200() throws Exception {
            gameResponse.setResult(Game.GameResult.WHITE_WINS);
            gameResponse.setWhiteScore(BigDecimal.ONE);
            gameResponse.setBlackScore(BigDecimal.ZERO);
            gameResponse.setStatus(Game.GameStatus.FINISHED);
            when(gameService.setGameResult(anyLong(), anyLong(), any(GameResultRequest.class), anyLong()))
                    .thenReturn(gameResponse);

            mockMvc.perform(patch("/api/tournaments/1/games/1/result")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resultRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("WHITE_WINS"))
                    .andExpect(jsonPath("$.whiteScore").value(1))
                    .andExpect(jsonPath("$.blackScore").value(0))
                    .andExpect(jsonPath("$.status").value("FINISHED"));
        }

        @Test
        @DisplayName("Should return 200 OK when BLACK_WINS")
        void setGameResult_BlackWins_Returns200() throws Exception {
            resultRequest.setResult(Game.GameResult.BLACK_WINS);
            gameResponse.setResult(Game.GameResult.BLACK_WINS);
            gameResponse.setWhiteScore(BigDecimal.ZERO);
            gameResponse.setBlackScore(BigDecimal.ONE);
            gameResponse.setStatus(Game.GameStatus.FINISHED);
            when(gameService.setGameResult(anyLong(), anyLong(), any(GameResultRequest.class), anyLong()))
                    .thenReturn(gameResponse);

            mockMvc.perform(patch("/api/tournaments/1/games/1/result")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resultRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("BLACK_WINS"));
        }

        @Test
        @DisplayName("Should return 200 OK when DRAW")
        void setGameResult_Draw_Returns200() throws Exception {
            resultRequest.setResult(Game.GameResult.DRAW);
            gameResponse.setResult(Game.GameResult.DRAW);
            gameResponse.setWhiteScore(new BigDecimal("0.5"));
            gameResponse.setBlackScore(new BigDecimal("0.5"));
            gameResponse.setStatus(Game.GameStatus.FINISHED);
            when(gameService.setGameResult(anyLong(), anyLong(), any(GameResultRequest.class), anyLong()))
                    .thenReturn(gameResponse);

            mockMvc.perform(patch("/api/tournaments/1/games/1/result")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resultRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("DRAW"))
                    .andExpect(jsonPath("$.whiteScore").value(0.5))
                    .andExpect(jsonPath("$.blackScore").value(0.5));
        }

        @Test
        @DisplayName("Should return 500 when game not found (IllegalArgumentException)")
        void setGameResult_Returns500_NotFound() throws Exception {
            when(gameService.setGameResult(anyLong(), anyLong(), any(GameResultRequest.class), anyLong()))
                    .thenThrow(new IllegalArgumentException("Game not found"));

            mockMvc.perform(patch("/api/tournaments/1/games/999/result")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resultRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =====================
    // GET /api/tournaments/{id}/games
    // =====================

    @Nested
    @DisplayName("GET /api/tournaments/{id}/games")
    class GetGamesTests {

        @Test
        @DisplayName("Should return 200 OK with list of games")
        void getGames_Returns200() throws Exception {
            when(gameService.getGames(1L)).thenReturn(List.of(gameResponse));

            mockMvc.perform(get("/api/tournaments/1/games")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("Should return 404 Not Found when tournament not found")
        void getGames_Returns404() throws Exception {
            when(gameService.getGames(999L))
                    .thenThrow(new TournamentNotFoundException(999L));

            mockMvc.perform(get("/api/tournaments/999/games")
                            .with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    // =====================
    // GET /api/tournaments/{id}/games/standings
    // =====================

    @Nested
    @DisplayName("GET /api/tournaments/{id}/games/standings")
    class GetStandingsTests {

        @Test
        @DisplayName("Should return 200 OK with standings")
        void getStandings_Returns200() throws Exception {
            when(gameService.getStandings(1L)).thenReturn(standingsResponse);

            mockMvc.perform(get("/api/tournaments/1/games/standings")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tournamentId").value(1))
                    .andExpect(jsonPath("$.currentRound").value(3))
                    .andExpect(jsonPath("$.standings").isArray())
                    .andExpect(jsonPath("$.standings[0].rank").value(1))
                    .andExpect(jsonPath("$.standings[0].score").value(2.5));
        }
    }

    // =====================
    // POST /api/tournaments/{id}/games/complete
    // =====================

    @Nested
    @DisplayName("POST /api/tournaments/{id}/games/complete")
    class CompleteTournamentTests {

        @Test
        @DisplayName("Should return 200 OK when tournament completed")
        void completeTournament_Returns200() throws Exception {
            standingsResponse = StandingsResponse.builder()
                    .tournamentId(1L)
                    .tournamentStatus("COMPLETED")
                    .standings(List.of())
                    .build();
            when(gameService.completeTournament(anyLong(), anyLong()))
                    .thenReturn(standingsResponse);

            mockMvc.perform(post("/api/tournaments/1/games/complete")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tournamentStatus").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not organizer")
        void completeTournament_Returns403() throws Exception {
            when(gameService.completeTournament(anyLong(), anyLong()))
                    .thenThrow(new AccessDeniedException("Only organizer can complete tournament"));

            mockMvc.perform(post("/api/tournaments/1/games/complete")
                            .with(user(testUser)))
                    .andExpect(status().isForbidden());
        }
    }

    // =====================
    // POST /api/tournaments/{id}/games/winners
    // =====================

    @Nested
    @DisplayName("POST /api/tournaments/{id}/games/winners")
    class AddWinnerTests {

        @Test
        @DisplayName("Should return 201 Created when winner added")
        void addWinner_Returns201() throws Exception {
            when(gameService.addWinner(anyLong(), any(WinnerRequest.class), anyLong()))
                    .thenReturn(winnerResponse);

            mockMvc.perform(post("/api/tournaments/1/games/winners")
                            .with(user(testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(winnerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.place").value(1))
                    .andExpect(jsonPath("$.awardType").value("PLACE"));
        }
    }

    // =====================
    // POST /api/tournaments/{id}/games/winners/all
    // =====================

    @Nested
    @DisplayName("POST /api/tournaments/{id}/games/winners/all")
    class AddAllAsWinnersTests {

        @Test
        @DisplayName("Should return 201 Created when all added as winners")
        void addAllAsWinners_Returns201() throws Exception {
            WinnerResponse winner1 = WinnerResponse.builder()
                    .id(1L)
                    .playerId(1L)
                    .awardType(TournamentWinner.AwardType.PARTICIPATION)
                    .awardTitle("Участник турнира")
                    .build();
            WinnerResponse winner2 = WinnerResponse.builder()
                    .id(2L)
                    .playerId(2L)
                    .awardType(TournamentWinner.AwardType.PARTICIPATION)
                    .awardTitle("Участник турнира")
                    .build();

            when(gameService.addAllAsWinners(anyLong(), anyString(), anyLong()))
                    .thenReturn(List.of(winner1, winner2));

            mockMvc.perform(post("/api/tournaments/1/games/winners/all")
                            .with(user(testUser))
                            .param("awardTitle", "Участник турнира"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].awardType").value("PARTICIPATION"));
        }
    }

    // =====================
    // GET /api/tournaments/{id}/games/winners
    // =====================

    @Nested
    @DisplayName("GET /api/tournaments/{id}/games/winners")
    class GetWinnersTests {

        @Test
        @DisplayName("Should return 200 OK with list of winners")
        void getWinners_Returns200() throws Exception {
            when(gameService.getWinners(1L)).thenReturn(List.of(winnerResponse));

            mockMvc.perform(get("/api/tournaments/1/games/winners")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].place").value(1));
        }
    }

    // =====================
    // DELETE /api/tournaments/{id}/games/winners/{winnerId}
    // =====================

    @Nested
    @DisplayName("DELETE /api/tournaments/{id}/games/winners/{winnerId}")
    class RemoveWinnerTests {

        @Test
        @DisplayName("Should return 204 No Content when winner removed")
        void removeWinner_Returns204() throws Exception {
            doNothing().when(gameService).removeWinner(anyLong(), anyLong(), anyLong());

            mockMvc.perform(delete("/api/tournaments/1/games/winners/1")
                            .with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(gameService).removeWinner(eq(1L), eq(1L), eq(1L));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when not organizer")
        void removeWinner_Returns403() throws Exception {
            doThrow(new AccessDeniedException("Only organizer can remove winners"))
                    .when(gameService).removeWinner(anyLong(), anyLong(), anyLong());

            mockMvc.perform(delete("/api/tournaments/1/games/winners/1")
                            .with(user(testUser)))
                    .andExpect(status().isForbidden());
        }
    }

    // =====================
    // DELETE /api/tournaments/{id}/games/{gameId}
    // =====================

    @Nested
    @DisplayName("DELETE /api/tournaments/{id}/games/{gameId}")
    class DeleteGameTests {

        @Test
        @DisplayName("Should return 204 No Content when game deleted")
        void deleteGame_Returns204() throws Exception {
            doNothing().when(gameService).deleteGame(anyLong(), anyLong(), anyLong());

            mockMvc.perform(delete("/api/tournaments/1/games/1")
                            .with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(gameService).deleteGame(eq(1L), eq(1L), eq(1L));
        }

        @Test
        @DisplayName("Should return 500 when trying to delete finished game (IllegalStateException)")
        void deleteGame_Returns500() throws Exception {
            doThrow(new IllegalStateException("Cannot delete finished game"))
                    .when(gameService).deleteGame(anyLong(), anyLong(), anyLong());

            mockMvc.perform(delete("/api/tournaments/1/games/1")
                            .with(user(testUser)))
                    .andExpect(status().isInternalServerError());
        }
    }
}
