package com.tournament.management.service;

import com.tournament.management.dto.MatchRequest;
import com.tournament.management.dto.MatchResponse;
import com.tournament.management.entity.Match;
import com.tournament.management.entity.Player;
import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.MatchRepository;
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
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private MatchService matchService;

    private Tournament testTournament;
    private Player player1;
    private Player player2;
    private Match testMatch;
    private MatchRequest testRequest;

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

        testRequest = new MatchRequest(1L, 2L, 1);
    }

    @Test
    void createMatch_Success() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerService.getPlayerEntity(1L)).thenReturn(player1);
        when(playerService.getPlayerEntity(2L)).thenReturn(player2);
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        MatchResponse response = matchService.createMatch(1L, testRequest);

        assertNotNull(response);
        assertEquals(1L, response.getPlayer1Id());
        assertEquals(2L, response.getPlayer2Id());
        assertEquals(1, response.getRound());
        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void createMatch_NotOngoing_ThrowsException() {
        testTournament.setStatus(TournamentStatus.REGISTRATION);
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);

        assertThrows(IllegalStateException.class, () -> 
            matchService.createMatch(1L, testRequest)
        );
    }

    @Test
    void createMatch_SamePlayer_ThrowsException() {
        testRequest = new MatchRequest(1L, 1L, 1);
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);

        assertThrows(IllegalArgumentException.class, () -> 
            matchService.createMatch(1L, testRequest)
        );
    }

    @Test
    void createMatch_PlayerNotInTournament_ThrowsException() {
        Tournament otherTournament = new Tournament("Other", "Other", 8);
        otherTournament.setId(2L);
        player1.setTournament(otherTournament);

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerService.getPlayerEntity(1L)).thenReturn(player1);
        when(playerService.getPlayerEntity(2L)).thenReturn(player2);

        assertThrows(IllegalArgumentException.class, () -> 
            matchService.createMatch(1L, testRequest)
        );
    }

    @Test
    void getMatchById_Success() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

        MatchResponse response = matchService.getMatchById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getMatchById_NotFound() {
        when(matchRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            matchService.getMatchById(1L)
        );
    }

    @Test
    void getMatchesByTournament_Success() {
        Match match2 = new Match(testTournament, player2, player1, 2);
        match2.setId(2L);
        match2.setCreatedAt(LocalDateTime.now());

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(matchRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(testMatch, match2));

        List<MatchResponse> matches = matchService.getMatchesByTournament(1L);

        assertEquals(2, matches.size());
    }

    @Test
    void getMatchesByTournamentAndRound_Success() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(matchRepository.findByTournamentIdAndRound(1L, 1)).thenReturn(Arrays.asList(testMatch));

        List<MatchResponse> matches = matchService.getMatchesByTournamentAndRound(1L, 1);

        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).getRound());
    }

    @Test
    void getRoundsByTournament_Success() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(matchRepository.findDistinctRoundsByTournamentId(1L)).thenReturn(Arrays.asList(1, 2, 3));

        List<Integer> rounds = matchService.getRoundsByTournament(1L);

        assertEquals(3, rounds.size());
        assertTrue(rounds.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void deleteMatch_Success() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        doNothing().when(matchRepository).delete(testMatch);

        assertDoesNotThrow(() -> matchService.deleteMatch(1L));
        verify(matchRepository, times(1)).delete(testMatch);
    }

    @Test
    void deleteMatch_HasResult_ThrowsException() {
        testMatch.setResult(new com.tournament.management.entity.Result(testMatch, 2, 1));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

        assertThrows(IllegalStateException.class, () -> 
            matchService.deleteMatch(1L)
        );
    }

    @Test
    void deleteMatch_TournamentFinished_ThrowsException() {
        testTournament.setStatus(TournamentStatus.FINISHED);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

        assertThrows(IllegalStateException.class, () -> 
            matchService.deleteMatch(1L)
        );
    }
}
