package com.chessai.tournament.repository;

import com.chessai.tournament.entity.TournamentPlayer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с участниками турнира
 */
@Repository
public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, Long> {

    /**
     * Найти участника по турниру и пользователю
     */
    Optional<TournamentPlayer> findByTournamentIdAndUserId(Long tournamentId, Long userId);

    /**
     * Проверить, зарегистрирован ли пользователь в турнире
     */
    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

    /**
     * Найти всех участников турнира
     */
    List<TournamentPlayer> findByTournamentId(Long tournamentId);

    /**
     * Найти всех участников турнира с пагинацией
     */
    Page<TournamentPlayer> findByTournamentId(Long tournamentId, Pageable pageable);

    /**
     * Найти участников турнира, отсортированных по очкам (standings)
     */
    @Query("SELECT tp FROM TournamentPlayer tp " +
           "WHERE tp.tournament.id = :tournamentId " +
           "AND tp.status IN ('REGISTERED', 'ACTIVE') " +
           "ORDER BY tp.score DESC, tp.wins DESC, tp.gamesPlayed ASC")
    List<TournamentPlayer> findStandingsByTournamentId(@Param("tournamentId") Long tournamentId);

    /**
     * Найти участников турнира, отсортированных по очкам с пагинацией
     */
    @Query("SELECT tp FROM TournamentPlayer tp " +
           "WHERE tp.tournament.id = :tournamentId " +
           "AND tp.status IN ('REGISTERED', 'ACTIVE') " +
           "ORDER BY tp.score DESC, tp.wins DESC, tp.gamesPlayed ASC")
    Page<TournamentPlayer> findStandingsByTournamentId(@Param("tournamentId") Long tournamentId, Pageable pageable);

    /**
     * Подсчитать количество активных участников турнира
     */
    @Query("SELECT COUNT(tp) FROM TournamentPlayer tp " +
           "WHERE tp.tournament.id = :tournamentId " +
           "AND tp.status IN ('REGISTERED', 'ACTIVE')")
    long countActiveByTournamentId(@Param("tournamentId") Long tournamentId);

    /**
     * Найти все турниры, в которых участвует пользователь
     */
    List<TournamentPlayer> findByUserId(Long userId);

    /**
     * Найти все турниры пользователя с пагинацией
     */
    Page<TournamentPlayer> findByUserId(Long userId, Pageable pageable);

    /**
     * Найти участников по статусу в турнире
     */
    List<TournamentPlayer> findByTournamentIdAndStatus(Long tournamentId, TournamentPlayer.PlayerStatus status);

    /**
     * Удалить всех участников турнира (для отмены турнира)
     */
    void deleteByTournamentId(Long tournamentId);

    /**
     * Найти топ-N участников турнира по очкам
     */
    @Query("SELECT tp FROM TournamentPlayer tp " +
           "WHERE tp.tournament.id = :tournamentId " +
           "AND tp.status IN ('REGISTERED', 'ACTIVE') " +
           "ORDER BY tp.score DESC, tp.wins DESC")
    List<TournamentPlayer> findTopPlayersByTournamentId(@Param("tournamentId") Long tournamentId, Pageable pageable);
}
