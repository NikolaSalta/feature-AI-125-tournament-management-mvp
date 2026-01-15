package com.tournament.management.service;

import com.tournament.management.dto.PlayerRequest;
import com.tournament.management.dto.PlayerResponse;
import com.tournament.management.entity.Player;
import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.PlayerRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private PlayerService playerService;

    private Tournament testTournament;
    private Player testPlayer;
    private PlayerRequest testRequest;

    @BeforeEach
    void setUp() {
        testTournament = new Tournament("Test Tournament", "Test Description", 16);
        testTournament.setId(1L);
        testTournament.setStatus(TournamentStatus.REGISTRATION);
        testTournament.setCreatedAt(LocalDateTime.now());
        testTournament.setUpdatedAt(LocalDateTime.now());

        testPlayer = new Player("Player One", "player1@test.com");
        testPlayer.setId(1L);
        testPlayer.setTournament(testTournament);
        testPlayer.setRegisteredAt(LocalDateTime.now());

        testRequest = new PlayerRequest("Player One", "player1@test.com");
    }

    @Test
    void registerPlayer_Success() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerRepository.existsByTournamentIdAndName(anyLong(), anyString())).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenReturn(testPlayer);

        PlayerResponse response = playerService.registerPlayer(1L, testRequest);

        assertNotNull(response);
        assertEquals("Player One", response.getName());
        assertEquals("player1@test.com", response.getEmail());
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    void registerPlayer_NotInRegistrationPhase_ThrowsException() {
        testTournament.setStatus(TournamentStatus.DRAFT);
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);

        assertThrows(IllegalStateException.class, () -> 
            playerService.registerPlayer(1L, testRequest)
        );
    }

    @Test
    void registerPlayer_MaxPlayersReached_ThrowsException() {
        testTournament.setMaxPlayers(1);
        testTournament.getPlayers().add(new Player("Existing Player", "existing@test.com"));
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);

        assertThrows(IllegalStateException.class, () -> 
            playerService.registerPlayer(1L, testRequest)
        );
    }

    @Test
    void registerPlayer_DuplicateName_ThrowsException() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerRepository.existsByTournamentIdAndName(1L, "Player One")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> 
            playerService.registerPlayer(1L, testRequest)
        );
    }

    @Test
    void getPlayerById_Success() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));

        PlayerResponse response = playerService.getPlayerById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Player One", response.getName());
    }

    @Test
    void getPlayerById_NotFound() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            playerService.getPlayerById(1L)
        );
    }

    @Test
    void getPlayersByTournament_Success() {
        Player player2 = new Player("Player Two", "player2@test.com");
        player2.setId(2L);
        player2.setTournament(testTournament);
        player2.setRegisteredAt(LocalDateTime.now());

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(testPlayer, player2));

        List<PlayerResponse> players = playerService.getPlayersByTournament(1L);

        assertEquals(2, players.size());
    }

    @Test
    void removePlayer_Success() {
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        doNothing().when(playerRepository).delete(testPlayer);

        assertDoesNotThrow(() -> playerService.removePlayer(1L, 1L));
        verify(playerRepository, times(1)).delete(testPlayer);
    }

    @Test
    void removePlayer_AfterRegistrationPhase_ThrowsException() {
        testTournament.setStatus(TournamentStatus.ONGOING);
        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);

        assertThrows(IllegalStateException.class, () -> 
            playerService.removePlayer(1L, 1L)
        );
    }

    @Test
    void removePlayer_WrongTournament_ThrowsException() {
        Tournament otherTournament = new Tournament("Other", "Other", 8);
        otherTournament.setId(2L);
        testPlayer.setTournament(otherTournament);

        when(tournamentService.getTournamentEntity(1L)).thenReturn(testTournament);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));

        assertThrows(IllegalArgumentException.class, () -> 
            playerService.removePlayer(1L, 1L)
        );
    }
}
