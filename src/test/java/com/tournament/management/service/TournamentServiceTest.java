package com.tournament.management.service;

import com.tournament.management.dto.TournamentRequest;
import com.tournament.management.dto.TournamentResponse;
import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import com.tournament.management.repository.TournamentRepository;
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
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentService tournamentService;

    private Tournament testTournament;
    private TournamentRequest testRequest;

    @BeforeEach
    void setUp() {
        testTournament = new Tournament("Test Tournament", "Test Description", 16);
        testTournament.setId(1L);
        testTournament.setStatus(TournamentStatus.DRAFT);
        testTournament.setCreatedAt(LocalDateTime.now());
        testTournament.setUpdatedAt(LocalDateTime.now());

        testRequest = new TournamentRequest("Test Tournament", "Test Description", 16);
    }

    @Test
    void createTournament_Success() {
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(testTournament);

        TournamentResponse response = tournamentService.createTournament(testRequest);

        assertNotNull(response);
        assertEquals("Test Tournament", response.getName());
        assertEquals(TournamentStatus.DRAFT, response.getStatus());
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }

    @Test
    void getTournamentById_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));

        TournamentResponse response = tournamentService.getTournamentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Tournament", response.getName());
    }

    @Test
    void getTournamentById_NotFound() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            tournamentService.getTournamentById(1L)
        );
    }

    @Test
    void getAllTournaments_Success() {
        Tournament tournament2 = new Tournament("Tournament 2", "Description 2", 8);
        tournament2.setId(2L);
        tournament2.setStatus(TournamentStatus.DRAFT);
        tournament2.setCreatedAt(LocalDateTime.now());
        tournament2.setUpdatedAt(LocalDateTime.now());

        when(tournamentRepository.findAll()).thenReturn(Arrays.asList(testTournament, tournament2));

        List<TournamentResponse> tournaments = tournamentService.getAllTournaments();

        assertEquals(2, tournaments.size());
    }

    @Test
    void getTournamentsByStatus_Success() {
        when(tournamentRepository.findByStatus(TournamentStatus.DRAFT))
            .thenReturn(Arrays.asList(testTournament));

        List<TournamentResponse> tournaments = tournamentService.getTournamentsByStatus(TournamentStatus.DRAFT);

        assertEquals(1, tournaments.size());
        assertEquals(TournamentStatus.DRAFT, tournaments.get(0).getStatus());
    }

    @Test
    void updateTournament_Success() {
        TournamentRequest updateRequest = new TournamentRequest("Updated Name", "Updated Description", 32);
        
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(i -> i.getArgument(0));

        TournamentResponse response = tournamentService.updateTournament(1L, updateRequest);

        assertEquals("Updated Name", response.getName());
        assertEquals("Updated Description", response.getDescription());
        assertEquals(32, response.getMaxPlayers());
    }

    @Test
    void updateTournament_NotInDraftStatus_ThrowsException() {
        testTournament.setStatus(TournamentStatus.ONGOING);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));

        assertThrows(IllegalStateException.class, () -> 
            tournamentService.updateTournament(1L, testRequest)
        );
    }

    @Test
    void updateTournamentStatus_DraftToRegistration_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(i -> i.getArgument(0));

        TournamentResponse response = tournamentService.updateTournamentStatus(1L, TournamentStatus.REGISTRATION);

        assertEquals(TournamentStatus.REGISTRATION, response.getStatus());
    }

    @Test
    void updateTournamentStatus_InvalidTransition_ThrowsException() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));

        assertThrows(IllegalStateException.class, () -> 
            tournamentService.updateTournamentStatus(1L, TournamentStatus.FINISHED)
        );
    }

    @Test
    void deleteTournament_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));
        doNothing().when(tournamentRepository).delete(testTournament);

        assertDoesNotThrow(() -> tournamentService.deleteTournament(1L));
        verify(tournamentRepository, times(1)).delete(testTournament);
    }

    @Test
    void deleteTournament_NotInDraftStatus_ThrowsException() {
        testTournament.setStatus(TournamentStatus.ONGOING);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(testTournament));

        assertThrows(IllegalStateException.class, () -> 
            tournamentService.deleteTournament(1L)
        );
    }
}
