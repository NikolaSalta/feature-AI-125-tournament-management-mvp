package com.chessai.tournament.repository;

import com.chessai.tournament.entity.TournamentWinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с победителями турнира
 */
@Repository
public interface TournamentWinnerRepository extends JpaRepository<TournamentWinner, Long> {

    /**
     * Найти всех победителей турнира
     */
    List<TournamentWinner> findByTournamentId(Long tournamentId);

    /**
     * Найти победителей турнира, отсортированных по месту
     */
    @Query("SELECT tw FROM TournamentWinner tw " +
           "WHERE tw.tournament.id = :tournamentId " +
           "ORDER BY tw.awardType, tw.place ASC NULLS LAST, tw.awardedAt ASC")
    List<TournamentWinner> findByTournamentIdOrdered(@Param("tournamentId") Long tournamentId);

    /**
     * Найти победителей по типу награды
     */
    List<TournamentWinner> findByTournamentIdAndAwardType(Long tournamentId, TournamentWinner.AwardType awardType);

    /**
     * Найти победителя по месту
     */
    Optional<TournamentWinner> findByTournamentIdAndPlace(Long tournamentId, Integer place);

    /**
     * Проверить, есть ли у игрока награда в турнире
     */
    boolean existsByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    /**
     * Найти все награды игрока
     */
    List<TournamentWinner> findByPlayerId(Long playerId);

    /**
     * Подсчитать количество победителей турнира
     */
    long countByTournamentId(Long tournamentId);

    /**
     * Удалить всех победителей турнира
     */
    void deleteByTournamentId(Long tournamentId);

    /**
     * Найти призовые места (1, 2, 3...)
     */
    @Query("SELECT tw FROM TournamentWinner tw " +
           "WHERE tw.tournament.id = :tournamentId " +
           "AND tw.awardType = 'PLACE' " +
           "ORDER BY tw.place ASC")
    List<TournamentWinner> findPlaceWinnersByTournamentId(@Param("tournamentId") Long tournamentId);
}
