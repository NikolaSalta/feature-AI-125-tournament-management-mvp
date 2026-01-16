package com.chessai.tournament.repository;

import com.chessai.tournament.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для TournamentRepository.
 * Проверяет персистентность и запросы к БД.
 */
@DataJpaTest
@DisplayName("TournamentRepository Tests")
class TournamentRepositoryTest {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testOrganizer;
    private Tournament testTournament;

    @BeforeEach
    void setUp() {
        // Создаём тестового организатора
        testOrganizer = new User("organizer", "organizer@test.com", "password");
        testOrganizer.addRole(Role.ORGANIZER);
        testOrganizer = userRepository.save(testOrganizer);

        // Создаём тестовый турнир
        testTournament = Tournament.builder()
                .name("Test Tournament")
                .description("Test Description")
                .format(TournamentFormat.SWISS)
                .status(TournamentStatus.DRAFT)
                .organizerId(testOrganizer.getId())
                .startDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(32)
                .timeControlMinutes(15)
                .timeIncrementSeconds(10)
                .isPublic(true)
                .build();
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("Should save tournament successfully")
        void shouldSaveTournament() {
            // When
            Tournament saved = tournamentRepository.save(testTournament);

            // Then
            assertNotNull(saved.getId());
            assertEquals("Test Tournament", saved.getName());
            assertEquals(TournamentFormat.SWISS, saved.getFormat());
            assertEquals(TournamentStatus.DRAFT, saved.getStatus());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
            assertEquals(0, saved.getCurrentParticipants());
        }

        @Test
        @DisplayName("Should find tournament by id")
        void shouldFindTournamentById() {
            // Given
            Tournament saved = tournamentRepository.save(testTournament);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<Tournament> found = tournamentRepository.findById(saved.getId());

            // Then
            assertTrue(found.isPresent());
            assertEquals("Test Tournament", found.get().getName());
        }

        @Test
        @DisplayName("Should update tournament")
        void shouldUpdateTournament() {
            // Given
            Tournament saved = tournamentRepository.save(testTournament);
            LocalDateTime originalUpdatedAt = saved.getUpdatedAt();
            entityManager.flush();
            entityManager.clear();

            // Wait a bit to ensure timestamp changes
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            Tournament found = tournamentRepository.findById(saved.getId()).orElseThrow();
            found.setName("Updated Tournament");
            found.setStatus(TournamentStatus.REGISTRATION_OPEN);
            Tournament updated = tournamentRepository.save(found);
            entityManager.flush();

            // Then
            assertEquals("Updated Tournament", updated.getName());
            assertEquals(TournamentStatus.REGISTRATION_OPEN, updated.getStatus());
            assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt) || 
                      updated.getUpdatedAt().equals(originalUpdatedAt));
        }

        @Test
        @DisplayName("Should delete tournament")
        void shouldDeleteTournament() {
            // Given
            Tournament saved = tournamentRepository.save(testTournament);
            Long id = saved.getId();

            // When
            tournamentRepository.deleteById(id);
            entityManager.flush();

            // Then
            assertFalse(tournamentRepository.findById(id).isPresent());
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("Should find tournaments by status")
        void shouldFindByStatus() {
            // Given
            tournamentRepository.save(testTournament);
            
            Tournament another = Tournament.builder()
                    .name("Another Tournament")
                    .format(TournamentFormat.ROUND_ROBIN)
                    .status(TournamentStatus.REGISTRATION_OPEN)
                    .organizerId(testOrganizer.getId())
                    .startDate(LocalDateTime.now().plusDays(10))
                    .isPublic(true)
                    .build();
            tournamentRepository.save(another);
            entityManager.flush();

            // When
            List<Tournament> drafts = tournamentRepository.findByStatus(TournamentStatus.DRAFT);
            List<Tournament> open = tournamentRepository.findByStatus(TournamentStatus.REGISTRATION_OPEN);

            // Then
            assertEquals(1, drafts.size());
            assertEquals("Test Tournament", drafts.get(0).getName());
            assertEquals(1, open.size());
            assertEquals("Another Tournament", open.get(0).getName());
        }

        @Test
        @DisplayName("Should find tournaments by organizer")
        void shouldFindByOrganizerId() {
            // Given
            tournamentRepository.save(testTournament);
            
            // Create another organizer and tournament
            User anotherOrganizer = new User("another", "another@test.com", "password");
            anotherOrganizer = userRepository.save(anotherOrganizer);
            
            Tournament anotherTournament = Tournament.builder()
                    .name("Another Tournament")
                    .format(TournamentFormat.KNOCKOUT)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(anotherOrganizer.getId())
                    .startDate(LocalDateTime.now().plusDays(5))
                    .isPublic(true)
                    .build();
            tournamentRepository.save(anotherTournament);
            entityManager.flush();

            // When
            List<Tournament> organizerTournaments = tournamentRepository.findByOrganizerId(testOrganizer.getId());

            // Then
            assertEquals(1, organizerTournaments.size());
            assertEquals("Test Tournament", organizerTournaments.get(0).getName());
        }

        @Test
        @DisplayName("Should find public tournaments")
        void shouldFindPublicTournaments() {
            // Given
            tournamentRepository.save(testTournament);
            
            Tournament privateTournament = Tournament.builder()
                    .name("Private Tournament")
                    .format(TournamentFormat.SWISS)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(testOrganizer.getId())
                    .startDate(LocalDateTime.now().plusDays(5))
                    .isPublic(false)
                    .build();
            tournamentRepository.save(privateTournament);
            entityManager.flush();

            // When
            List<Tournament> publicTournaments = tournamentRepository.findByIsPublicTrue();

            // Then
            assertEquals(1, publicTournaments.size());
            assertEquals("Test Tournament", publicTournaments.get(0).getName());
        }

        @Test
        @DisplayName("Should find upcoming tournaments")
        void shouldFindUpcomingTournaments() {
            // Given
            testTournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            tournamentRepository.save(testTournament);
            entityManager.flush();

            // When
            List<Tournament> upcoming = tournamentRepository.findUpcomingTournaments(LocalDateTime.now());

            // Then
            assertEquals(1, upcoming.size());
            assertEquals("Test Tournament", upcoming.get(0).getName());
        }

        @Test
        @DisplayName("Should check tournament name existence")
        void shouldCheckTournamentNameExists() {
            // Given
            tournamentRepository.save(testTournament);
            entityManager.flush();

            // When
            boolean exists = tournamentRepository.existsByNameAndOrganizerId(
                    "Test Tournament", 
                    testOrganizer.getId()
            );
            boolean notExists = tournamentRepository.existsByNameAndOrganizerId(
                    "Non-existent Tournament", 
                    testOrganizer.getId()
            );

            // Then
            assertTrue(exists);
            assertFalse(notExists);
        }

        @Test
        @DisplayName("Should count tournaments by organizer and status")
        void shouldCountByOrganizerAndStatus() {
            // Given
            tournamentRepository.save(testTournament);
            
            Tournament another = Tournament.builder()
                    .name("Another Draft")
                    .format(TournamentFormat.ROUND_ROBIN)
                    .status(TournamentStatus.DRAFT)
                    .organizerId(testOrganizer.getId())
                    .startDate(LocalDateTime.now().plusDays(10))
                    .isPublic(true)
                    .build();
            tournamentRepository.save(another);
            entityManager.flush();

            // When
            long count = tournamentRepository.countByOrganizerIdAndStatus(
                    testOrganizer.getId(), 
                    TournamentStatus.DRAFT
            );

            // Then
            assertEquals(2, count);
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogic {

        @Test
        @DisplayName("Should validate tournament constraints")
        void shouldValidateConstraints() {
            // Given
            Tournament tournament = tournamentRepository.save(testTournament);

            // Then
            assertFalse(tournament.canRegisterParticipant()); // DRAFT status, method checks REGISTRATION_OPEN
            assertTrue(tournament.isRatingEligible(1500));
            assertTrue(tournament.isOrganizer(testOrganizer.getId()));
            assertTrue(tournament.isEditable());
            assertFalse(tournament.hasStarted());
        }

        @Test
        @DisplayName("Should handle participant registration logic")
        void shouldHandleParticipantRegistration() {
            // Given
            testTournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            Tournament tournament = tournamentRepository.save(testTournament);

            // When
            boolean canRegister = tournament.canRegisterParticipant();

            // Then
            assertTrue(canRegister);
            assertEquals(0, tournament.getCurrentParticipants());
            assertEquals(32, tournament.getMaxParticipants());
        }

        @Test
        @DisplayName("Should validate rating eligibility")
        void shouldValidateRatingEligibility() {
            // Given
            testTournament.setMinRating(1200);
            testTournament.setMaxRating(1800);
            Tournament tournament = tournamentRepository.save(testTournament);

            // Then
            assertFalse(tournament.isRatingEligible(1000)); // Too low
            assertTrue(tournament.isRatingEligible(1500));  // OK
            assertFalse(tournament.isRatingEligible(2000)); // Too high
        }

        @Test
        @DisplayName("Should handle tournament lifecycle")
        void shouldHandleTournamentLifecycle() {
            // Given
            Tournament tournament = tournamentRepository.save(testTournament);

            // DRAFT -> REGISTRATION_OPEN
            tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            tournament = tournamentRepository.save(tournament);
            assertTrue(tournament.isEditable());
            assertFalse(tournament.hasStarted());

            // REGISTRATION_OPEN -> IN_PROGRESS
            tournament.setStatus(TournamentStatus.IN_PROGRESS);
            tournament = tournamentRepository.save(tournament);
            assertFalse(tournament.isEditable());
            assertTrue(tournament.hasStarted());

            // IN_PROGRESS -> COMPLETED
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournament.setEndDate(LocalDateTime.now());
            tournament = tournamentRepository.save(tournament);
            assertFalse(tournament.isEditable());
            assertTrue(tournament.hasStarted());

            // Verify persistence
            entityManager.flush();
            entityManager.clear();
            
            Optional<Tournament> found = tournamentRepository.findById(tournament.getId());
            assertTrue(found.isPresent());
            assertEquals(TournamentStatus.COMPLETED, found.get().getStatus());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle tournament without max participants")
        void shouldHandleUnlimitedParticipants() {
            // Given
            testTournament.setMaxParticipants(null);
            testTournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
            Tournament tournament = tournamentRepository.save(testTournament);

            // Then
            assertTrue(tournament.canRegisterParticipant());
        }

        @Test
        @DisplayName("Should handle tournament without rating restrictions")
        void shouldHandleNoRatingRestrictions() {
            // Given
            testTournament.setMinRating(null);
            testTournament.setMaxRating(null);
            Tournament tournament = tournamentRepository.save(testTournament);

            // Then
            assertTrue(tournament.isRatingEligible(0));
            assertTrue(tournament.isRatingEligible(3000));
        }

        @Test
        @DisplayName("Should handle different tournament formats")
        void shouldHandleDifferentFormats() {
            // Test all formats
            for (TournamentFormat format : TournamentFormat.values()) {
                Tournament tournament = Tournament.builder()
                        .name("Tournament " + format.name())
                        .format(format)
                        .status(TournamentStatus.DRAFT)
                        .organizerId(testOrganizer.getId())
                        .startDate(LocalDateTime.now().plusDays(7))
                        .isPublic(true)
                        .build();
                
                Tournament saved = tournamentRepository.save(tournament);
                assertNotNull(saved.getId());
                assertEquals(format, saved.getFormat());
            }
        }
    }
}

