package com.chessai.tournament.service;

import com.chessai.tournament.dto.TournamentRequest;
import com.chessai.tournament.dto.TournamentResponse;
import com.chessai.tournament.entity.Tournament;
import com.chessai.tournament.entity.TournamentFormat;
import com.chessai.tournament.entity.TournamentStatus;
import com.chessai.tournament.exception.TournamentNotEditableException;
import com.chessai.tournament.exception.TournamentNotFoundException;
import com.chessai.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentService Unit Tests")
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentService tournamentService;

    private TournamentRequest validRequest;
    private Tournament tournament;

    @BeforeEach
    void setUp() {
        validRequest = TournamentRequest.builder()
                .name("Test Tournament")
                .description("Test Description")
                .format(TournamentFormat.SWISS)
                .startDate(LocalDateTime.now().plusDays(30))
                .endDate(LocalDateTime.now().plusDays(40))
                .maxParticipants(64)
                .timeControlMinutes(15)
                .timeIncrementSeconds(10)
                .minRating(2000)
                .maxRating(2800)
                .prizePool(10000)
                .entryFee(50)
                .isPublic(true)
                .build();

        tournament = Tournament.builder()
                .id(1L)
                .name("Test Tournament")
                .description("Test Description")
                .format(TournamentFormat.SWISS)
                .status(TournamentStatus.DRAFT)
                .organizerId(1L)
                .startDate(LocalDateTime.now().plusDays(30))
                .endDate(LocalDateTime.now().plusDays(40))
                .maxParticipants(64)
                .currentParticipants(0)
                .timeControlMinutes(15)
                .timeIncrementSeconds(10)
                .minRating(2000)
                .maxRating(2800)
                .prizePool(10000)
                .entryFee(50)
                .isPublic(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Tournament")
    class CreateTournamentTests {

        @Test
        @DisplayName("Should create tournament successfully")
        void shouldCreateTournamentSuccessfully() {
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

            TournamentResponse response = tournamentService.createTournament(validRequest, 1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Test Tournament");
            assertThat(response.getOrganizerId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(TournamentStatus.DRAFT);
            assertThat(response.getCurrentParticipants()).isEqualTo(0);

            verify(tournamentRepository, times(1)).save(any(Tournament.class));
        }

        @Test
        @DisplayName("Should set default status to DRAFT when not provided")
        void shouldSetDefaultStatusToDraft() {
            validRequest.setStatus(null);
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

            TournamentResponse response = tournamentService.createTournament(validRequest, 1L);

            assertThat(response.getStatus()).isEqualTo(TournamentStatus.DRAFT);
        }

        @Test
        @DisplayName("Should set default isPublic to true when not provided")
        void shouldSetDefaultIsPublicToTrue() {
            validRequest.setIsPublic(null);
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

            TournamentResponse response = tournamentService.createTournament(validRequest, 1L);

            assertThat(response.getIsPublic()).isTrue();
        }
    }

    @Nested
    @DisplayName("Get Tournament")
    class GetTournamentTests {

        @Test
        @DisplayName("Should get tournament by ID successfully")
        void shouldGetTournamentByIdSuccessfully() {
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            TournamentResponse response = tournamentService.getTournamentById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Test Tournament");

            verify(tournamentRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw TournamentNotFoundException when tournament not found")
        void shouldThrowExceptionWhenTournamentNotFound() {
            when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tournamentService.getTournamentById(99L))
                    .isInstanceOf(TournamentNotFoundException.class)
                    .hasMessageContaining("99");

            verify(tournamentRepository, times(1)).findById(99L);
        }

        @Test
        @DisplayName("Should get all tournaments with pagination")
        void shouldGetAllTournamentsWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tournament> page = new PageImpl<>(List.of(tournament));
            when(tournamentRepository.findAll(pageable)).thenReturn(page);

            Page<TournamentResponse> response = tournamentService.getAllTournaments(pageable);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(1L);

            verify(tournamentRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get public tournaments")
        void shouldGetPublicTournaments() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tournament> page = new PageImpl<>(List.of(tournament));
            when(tournamentRepository.findByIsPublicTrue(pageable)).thenReturn(page);

            Page<TournamentResponse> response = tournamentService.getPublicTournaments(pageable);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getIsPublic()).isTrue();

            verify(tournamentRepository, times(1)).findByIsPublicTrue(pageable);
        }

        @Test
        @DisplayName("Should get tournaments by status")
        void shouldGetTournamentsByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tournament> page = new PageImpl<>(List.of(tournament));
            when(tournamentRepository.findByStatus(TournamentStatus.DRAFT, pageable)).thenReturn(page);

            Page<TournamentResponse> response = tournamentService.getTournamentsByStatus(TournamentStatus.DRAFT, pageable);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getStatus()).isEqualTo(TournamentStatus.DRAFT);

            verify(tournamentRepository, times(1)).findByStatus(TournamentStatus.DRAFT, pageable);
        }

        @Test
        @DisplayName("Should get upcoming tournaments")
        void shouldGetUpcomingTournaments() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tournament> page = new PageImpl<>(List.of(tournament));
            when(tournamentRepository.findUpcomingTournaments(any(LocalDateTime.class), eq(pageable))).thenReturn(page);

            Page<TournamentResponse> response = tournamentService.getUpcomingTournaments(pageable);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);

            verify(tournamentRepository, times(1)).findUpcomingTournaments(any(LocalDateTime.class), eq(pageable));
        }

        @Test
        @DisplayName("Should get tournaments by organizer")
        void shouldGetTournamentsByOrganizer() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tournament> page = new PageImpl<>(List.of(tournament));
            when(tournamentRepository.findByOrganizerId(1L, pageable)).thenReturn(page);

            Page<TournamentResponse> response = tournamentService.getTournamentsByOrganizer(1L, pageable);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getOrganizerId()).isEqualTo(1L);

            verify(tournamentRepository, times(1)).findByOrganizerId(1L, pageable);
        }
    }

    @Nested
    @DisplayName("Update Tournament")
    class UpdateTournamentTests {

        @Test
        @DisplayName("Should update tournament successfully")
        void shouldUpdateTournamentSuccessfully() {
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

            validRequest.setName("Updated Name");
            validRequest.setMaxParticipants(128);

            TournamentResponse response = tournamentService.updateTournament(1L, validRequest, 1L);

            assertThat(response).isNotNull();
            verify(tournamentRepository, times(1)).findById(1L);
            verify(tournamentRepository, times(1)).save(any(Tournament.class));
        }

        @Test
        @DisplayName("Should throw exception when tournament not found")
        void shouldThrowExceptionWhenTournamentNotFound() {
            when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tournamentService.updateTournament(99L, validRequest, 1L))
                    .isInstanceOf(TournamentNotFoundException.class);

            verify(tournamentRepository, times(1)).findById(99L);
            verify(tournamentRepository, never()).save(any(Tournament.class));
        }

        @Test
        @DisplayName("Should throw exception when tournament not editable")
        void shouldThrowExceptionWhenTournamentNotEditable() {
            tournament.setStatus(TournamentStatus.COMPLETED);
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            assertThatThrownBy(() -> tournamentService.updateTournament(1L, validRequest, 1L))
                    .isInstanceOf(TournamentNotEditableException.class);

            verify(tournamentRepository, times(1)).findById(1L);
            verify(tournamentRepository, never()).save(any(Tournament.class));
        }
    }

    @Nested
    @DisplayName("Delete Tournament")
    class DeleteTournamentTests {

        @Test
        @DisplayName("Should delete tournament successfully")
        void shouldDeleteTournamentSuccessfully() {
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            doNothing().when(tournamentRepository).delete(tournament);

            tournamentService.deleteTournament(1L, 1L);

            verify(tournamentRepository, times(1)).findById(1L);
            verify(tournamentRepository, times(1)).delete(tournament);
        }

        @Test
        @DisplayName("Should throw exception when tournament not found")
        void shouldThrowExceptionWhenTournamentNotFound() {
            when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tournamentService.deleteTournament(99L, 1L))
                    .isInstanceOf(TournamentNotFoundException.class);

            verify(tournamentRepository, times(1)).findById(99L);
            verify(tournamentRepository, never()).delete(any(Tournament.class));
        }

        @Test
        @DisplayName("Should throw exception when tournament not editable")
        void shouldThrowExceptionWhenTournamentNotEditable() {
            tournament.setStatus(TournamentStatus.IN_PROGRESS);
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            assertThatThrownBy(() -> tournamentService.deleteTournament(1L, 1L))
                    .isInstanceOf(TournamentNotEditableException.class);

            verify(tournamentRepository, times(1)).findById(1L);
            verify(tournamentRepository, never()).delete(any(Tournament.class));
        }
    }

    @Nested
    @DisplayName("Update Tournament Status")
    class UpdateTournamentStatusTests {

        @Test
        @DisplayName("Should update tournament status successfully")
        void shouldUpdateTournamentStatusSuccessfully() {
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

            TournamentResponse response = tournamentService.updateTournamentStatus(1L, TournamentStatus.REGISTRATION_OPEN, 1L);

            assertThat(response).isNotNull();
            verify(tournamentRepository, times(1)).findById(1L);
            verify(tournamentRepository, times(1)).save(any(Tournament.class));
        }

        @Test
        @DisplayName("Should throw exception when tournament not found")
        void shouldThrowExceptionWhenTournamentNotFound() {
            when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tournamentService.updateTournamentStatus(99L, TournamentStatus.REGISTRATION_OPEN, 1L))
                    .isInstanceOf(TournamentNotFoundException.class);

            verify(tournamentRepository, times(1)).findById(99L);
            verify(tournamentRepository, never()).save(any(Tournament.class));
        }
    }

    @Nested
    @DisplayName("Response Mapping")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFieldsCorrectly() {
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

            TournamentResponse response = tournamentService.getTournamentById(1L);

            assertThat(response.getId()).isEqualTo(tournament.getId());
            assertThat(response.getName()).isEqualTo(tournament.getName());
            assertThat(response.getDescription()).isEqualTo(tournament.getDescription());
            assertThat(response.getFormat()).isEqualTo(tournament.getFormat());
            assertThat(response.getStatus()).isEqualTo(tournament.getStatus());
            assertThat(response.getOrganizerId()).isEqualTo(tournament.getOrganizerId());
            assertThat(response.getStartDate()).isEqualTo(tournament.getStartDate());
            assertThat(response.getEndDate()).isEqualTo(tournament.getEndDate());
            assertThat(response.getMaxParticipants()).isEqualTo(tournament.getMaxParticipants());
            assertThat(response.getCurrentParticipants()).isEqualTo(tournament.getCurrentParticipants());
            assertThat(response.getTimeControlMinutes()).isEqualTo(tournament.getTimeControlMinutes());
            assertThat(response.getTimeIncrementSeconds()).isEqualTo(tournament.getTimeIncrementSeconds());
            assertThat(response.getMinRating()).isEqualTo(tournament.getMinRating());
            assertThat(response.getMaxRating()).isEqualTo(tournament.getMaxRating());
            assertThat(response.getPrizePool()).isEqualTo(tournament.getPrizePool());
            assertThat(response.getEntryFee()).isEqualTo(tournament.getEntryFee());
            assertThat(response.getIsPublic()).isEqualTo(tournament.getIsPublic());
            assertThat(response.getCreatedAt()).isEqualTo(tournament.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(tournament.getUpdatedAt());
            assertThat(response.getHasAvailableSlots()).isEqualTo(tournament.canRegisterParticipant());
            assertThat(response.getHasStarted()).isEqualTo(tournament.hasStarted());
            assertThat(response.getIsEditable()).isEqualTo(tournament.isEditable());
        }
    }
}

