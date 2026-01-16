package com.chessai.tournament.repository;

import com.chessai.tournament.entity.Tournament;
import com.chessai.tournament.entity.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с турнирами
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    /**
     * Найти все турниры по статусу
     */
    List<Tournament> findByStatus(TournamentStatus status);

    /**
     * Найти все турниры по статусу с пагинацией
     */
    Page<Tournament> findByStatus(TournamentStatus status, Pageable pageable);

    /**
     * Найти все турниры организатора
     */
    List<Tournament> findByOrganizerId(Long organizerId);

    /**
     * Найти все турниры организатора с пагинацией
     */
    Page<Tournament> findByOrganizerId(Long organizerId, Pageable pageable);

    /**
     * Найти все публичные турниры
     */
    List<Tournament> findByIsPublicTrue();

    /**
     * Найти все публичные турниры с пагинацией
     */
    Page<Tournament> findByIsPublicTrue(Pageable pageable);

    /**
     * Найти турниры по статусу и публичности
     */
    List<Tournament> findByStatusAndIsPublicTrue(TournamentStatus status);

    /**
     * Найти турниры, начинающиеся в заданном диапазоне дат
     */
    List<Tournament> findByStartDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Найти турниры по организатору и статусу
     */
    List<Tournament> findByOrganizerIdAndStatus(Long organizerId, TournamentStatus status);

    /**
     * Проверить существование турнира по имени и организатору
     */
    boolean existsByNameAndOrganizerId(String name, Long organizerId);

    /**
     * Найти активные турниры (регистрация открыта или в процессе)
     */
    @Query("SELECT t FROM Tournament t WHERE t.status IN ('REGISTRATION_OPEN', 'IN_PROGRESS') AND t.isPublic = true")
    List<Tournament> findActiveTournaments();

    /**
     * Найти предстоящие турниры (регистрация открыта)
     */
    @Query("SELECT t FROM Tournament t WHERE t.status = 'REGISTRATION_OPEN' AND t.startDate > :now AND t.isPublic = true ORDER BY t.startDate ASC")
    List<Tournament> findUpcomingTournaments(@Param("now") LocalDateTime now);

    /**
     * Найти предстоящие турниры с пагинацией
     */
    @Query("SELECT t FROM Tournament t WHERE t.startDate > :now AND t.isPublic = true ORDER BY t.startDate ASC")
    Page<Tournament> findUpcomingTournaments(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Найти завершённые турниры организатора
     */
    @Query("SELECT t FROM Tournament t WHERE t.organizerId = :organizerId AND t.status = 'COMPLETED' ORDER BY t.endDate DESC")
    List<Tournament> findCompletedTournamentsByOrganizer(@Param("organizerId") Long organizerId);

    /**
     * Подсчитать количество турниров организатора по статусу
     */
    long countByOrganizerIdAndStatus(Long organizerId, TournamentStatus status);

    /**
     * Найти турниры с доступными местами
     */
    @Query("SELECT t FROM Tournament t WHERE t.status = 'REGISTRATION_OPEN' " +
           "AND t.isPublic = true " +
           "AND (t.maxParticipants IS NULL OR t.currentParticipants < t.maxParticipants) " +
           "ORDER BY t.startDate ASC")
    List<Tournament> findTournamentsWithAvailableSlots();
}


