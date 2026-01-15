package com.tournament.management.service;

import com.tournament.management.dto.ResultRequest;
import com.tournament.management.dto.ResultResponse;
import com.tournament.management.entity.*;
import com.tournament.management.repository.ResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private ResultService resultService;

    private Tournament testTournament;
    private Player player1;
    private Player player2;
    private Match testMatch;
    private Result testResult;
    private ResultRequest testRequest;

    @BeforeEach
    void setUp() {
        testTournament = new Tournament("Test Tournament", "Test Description", 16);
        testTournament.setId(1L);
        testTournament.setStatus(TournamentStatus.ONGOING);
        testTournament.setCreatedAt(LocalDateTime.now());
        testTournament.setUpdatedAt(LocalDateTime.now());

        player1 = new Player("Player One", "player1@test.com");
        player1.setId(1L);
        player1.setTournament(testTournament);
        player1.setRegisteredAt(LocalDateTime.now());

        player2 = new Player("Player Two", "player2@test.com");
        player2.setId(2L);
        player2.setTournament(testTournament);
        player2.setRegisteredAt(LocalDateTime.now());

        testMatch = new Match(testTournament, player1, player2, 1);
        testMatch.setId(1L);
        testMatch.setCreatedAt(LocalDateTime.now());

        testResult = new Result(testMatch, 2, 1);
        testResult.setId(1L);
        testResult.setSubmittedAt(LocalDateTime.now());

        testRequest = new ResultRequest(2, 1);
    }

    @Test
    void submitResult_Success() {
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);
        when(resultRepository.save(any(Result.class))).thenReturn(testResult);

        ResultResponse response = resultService.submitResult(1L, testRequest);

        assertNotNull(response);
        assertEquals(2, response.getPlayer1Score());
        assertEquals(1, response.getPlayer2Score());
        assertEquals(player1.getId(), response.getWinnerId());
        assertFalse(response.getIsDraw());
        verify(resultRepository, times(1)).save(any(Result.class));
    }

    @Test
    void submitResult_Draw() {
        testRequest = new ResultRequest(1, 1);
        Result drawResult = new Result(testMatch, 1, 1);
        drawResult.setId(1L);
        drawResult.setSubmittedAt(LocalDateTime.now());

        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);
        when(resultRepository.save(any(Result.class))).thenReturn(drawResult);

        ResultResponse response = resultService.submitResult(1L, testRequest);

        assertTrue(response.getIsDraw());
        assertNull(response.getWinnerId());
    }

    @Test
    void submitResult_NotOngoing_ThrowsException() {
        testTournament.setStatus(TournamentStatus.REGISTRATION);
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);

        assertThrows(IllegalStateException.class, () -> 
            resultService.submitResult(1L, testRequest)
        );
    }

    @Test
    void submitResult_AlreadyHasResult_ThrowsException() {
        testMatch.setResult(testResult);
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);

        assertThrows(IllegalStateException.class, () -> 
            resultService.submitResult(1L, testRequest)
        );
    }

    @Test
    void updateResult_Success() {
        testMatch.setResult(testResult);
        ResultRequest updateRequest = new ResultRequest(3, 2);

        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);
        when(resultRepository.save(any(Result.class))).thenAnswer(i -> i.getArgument(0));

        ResultResponse response = resultService.updateResult(1L, updateRequest);

        assertEquals(3, response.getPlayer1Score());
        assertEquals(2, response.getPlayer2Score());
    }

    @Test
    void updateResult_TournamentFinished_ThrowsException() {
        testTournament.setStatus(TournamentStatus.FINISHED);
        testMatch.setResult(testResult);
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);

        assertThrows(IllegalStateException.class, () -> 
            resultService.updateResult(1L, testRequest)
        );
    }

    @Test
    void updateResult_NoExistingResult_ThrowsException() {
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);

        assertThrows(IllegalStateException.class, () -> 
            resultService.updateResult(1L, testRequest)
        );
    }

    @Test
    void getResultByMatchId_Success() {
        when(resultRepository.findByMatchId(1L)).thenReturn(Optional.of(testResult));

        ResultResponse response = resultService.getResultByMatchId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getMatchId());
    }

    @Test
    void getResultByMatchId_NotFound() {
        when(resultRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            resultService.getResultByMatchId(1L)
        );
    }

    @Test
    void getResultsByTournament_Success() {
        Match match2 = new Match(testTournament, player2, player1, 2);
        match2.setId(2L);
        match2.setCreatedAt(LocalDateTime.now());
        Result result2 = new Result(match2, 1, 2);
        result2.setId(2L);
        result2.setSubmittedAt(LocalDateTime.now());

        when(resultRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(testResult, result2));

        List<ResultResponse> results = resultService.getResultsByTournament(1L);

        assertEquals(2, results.size());
    }

    @Test
    void deleteResult_Success() {
        testMatch.setResult(testResult);
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);
        doNothing().when(resultRepository).delete(testResult);

        assertDoesNotThrow(() -> resultService.deleteResult(1L));
        verify(resultRepository, times(1)).delete(testResult);
    }

    @Test
    void deleteResult_TournamentFinished_ThrowsException() {
        testTournament.setStatus(TournamentStatus.FINISHED);
        testMatch.setResult(testResult);
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);

        assertThrows(IllegalStateException.class, () -> 
            resultService.deleteResult(1L)
        );
    }

    @Test
    void deleteResult_NoResult_ThrowsException() {
        when(matchService.getMatchEntity(1L)).thenReturn(testMatch);

        assertThrows(IllegalArgumentException.class, () -> 
            resultService.deleteResult(1L)
        );
    }
}
