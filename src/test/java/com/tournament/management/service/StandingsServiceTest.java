package com.tournament.management.service;

import com.tournament.management.dto.StandingResponse;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandingsServiceTest {

    @Mock
    private TournamentService tournamentService;

    @Mock
    private ResultRepository resultRepository;

    @InjectMocks
    private StandingsService standingsService;

    private Tournament testTournament;
    private Player player1;
    private Player player2;
    private Player player3;

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

        player3 = new Player("Player Three", "player3@test.com");
        player3.setId(3L);
        player3.setTournament(testTournament);
        player3.setRegisteredAt(LocalDateTime.now());

        testTournament.getPlayers().addAll(Arrays.asList(player1, player2, player3));
    }

    @Test
    void getStandings_NoResults() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(resultRepository.findByTournamentId(1L)).thenReturn(Collections.emptyList());

        List<StandingResponse> standings = standingsService.getStandings(1L);

        assertEquals(3, standings.size());
        for (StandingResponse standing : standings) {
            assertEquals(0, standing.getPoints());
            assertEquals(0, standing.getWins());
            assertEquals(0, standing.getDraws());
            assertEquals(0, standing.getLosses());
            assertEquals(0, standing.getMatchesPlayed());
        }
    }

    @Test
    void getStandings_WithResults() {
        Match match1 = new Match(testTournament, player1, player2, 1);
        match1.setId(1L);
        match1.setCreatedAt(LocalDateTime.now());

        Result result1 = new Result(match1, 2, 1);
        result1.setId(1L);
        result1.setSubmittedAt(LocalDateTime.now());

        Match match2 = new Match(testTournament, player1, player3, 1);
        match2.setId(2L);
        match2.setCreatedAt(LocalDateTime.now());

        Result result2 = new Result(match2, 3, 0);
        result2.setId(2L);
        result2.setSubmittedAt(LocalDateTime.now());

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(resultRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(result1, result2));

        List<StandingResponse> standings = standingsService.getStandings(1L);

        assertEquals(3, standings.size());
        
        // Player 1 should be first (2 wins, 6 points)
        assertEquals(1L, standings.get(0).getPlayerId());
        assertEquals(6, standings.get(0).getPoints());
        assertEquals(2, standings.get(0).getWins());
        assertEquals(0, standings.get(0).getLosses());
        assertEquals(1, standings.get(0).getRank());

        // Player 2 and Player 3 should have 0 points (1 loss each)
        assertTrue(standings.get(1).getPoints() == 0 && standings.get(2).getPoints() == 0);
    }

    @Test
    void getStandings_WithDraw() {
        Match match1 = new Match(testTournament, player1, player2, 1);
        match1.setId(1L);
        match1.setCreatedAt(LocalDateTime.now());

        Result result1 = new Result(match1, 1, 1); // Draw
        result1.setId(1L);
        result1.setSubmittedAt(LocalDateTime.now());

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(resultRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(result1));

        List<StandingResponse> standings = standingsService.getStandings(1L);

        // Player 1 and Player 2 should have 1 point each for the draw
        StandingResponse player1Standing = standings.stream()
            .filter(s -> s.getPlayerId().equals(1L))
            .findFirst()
            .orElseThrow();
        
        StandingResponse player2Standing = standings.stream()
            .filter(s -> s.getPlayerId().equals(2L))
            .findFirst()
            .orElseThrow();

        assertEquals(1, player1Standing.getPoints());
        assertEquals(1, player1Standing.getDraws());
        assertEquals(1, player2Standing.getPoints());
        assertEquals(1, player2Standing.getDraws());
    }

    @Test
    void getStandings_RankAssignment() {
        Match match1 = new Match(testTournament, player1, player2, 1);
        match1.setId(1L);
        match1.setCreatedAt(LocalDateTime.now());
        Result result1 = new Result(match1, 2, 1);
        result1.setId(1L);
        result1.setSubmittedAt(LocalDateTime.now());

        Match match2 = new Match(testTournament, player2, player3, 1);
        match2.setId(2L);
        match2.setCreatedAt(LocalDateTime.now());
        Result result2 = new Result(match2, 2, 1);
        result2.setId(2L);
        result2.setSubmittedAt(LocalDateTime.now());

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(resultRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(result1, result2));

        List<StandingResponse> standings = standingsService.getStandings(1L);

        assertEquals(1, standings.get(0).getRank());
        assertEquals(2, standings.get(1).getRank());
        assertEquals(3, standings.get(2).getRank());
    }
}
