package com.chessai.tournament.repository;

import com.chessai.tournament.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с партиями турнира
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Найти все партии турнира
     */
    List<Game> findByTournamentId(Long tournamentId);

    /**
     * Найти все партии турнира с пагинацией
     */
    Page<Game> findByTournamentId(Long tournamentId, Pageable pageable);

    /**
     * Найти партии конкретного раунда турнира
     */
    List<Game> findByTournamentIdAndRoundNumber(Long tournamentId, Integer roundNumber);

    /**
     * Найти партии конкретного раунда с сортировкой по номеру доски
     */
    @Query("SELECT g FROM Game g " +
           "WHERE g.tournament.id = :tournamentId AND g.roundNumber = :roundNumber " +
           "ORDER BY g.boardNumber ASC NULLS LAST")
    List<Game> findByTournamentIdAndRoundNumberOrdered(
            @Param("tournamentId") Long tournamentId, 
            @Param("roundNumber") Integer roundNumber);

    /**
     * Найти партии игрока в турнире (белыми или чёрными)
     */
    @Query("SELECT g FROM Game g " +
           "WHERE g.tournament.id = :tournamentId " +
           "AND (g.whitePlayer.id = :playerId OR g.blackPlayer.id = :playerId) " +
           "ORDER BY g.roundNumber ASC")
    List<Game> findByTournamentIdAndPlayerId(
            @Param("tournamentId") Long tournamentId, 
            @Param("playerId") Long playerId);

    /**
     * Найти партию между двумя игроками в турнире
     */
    @Query("SELECT g FROM Game g " +
           "WHERE g.tournament.id = :tournamentId " +
           "AND ((g.whitePlayer.id = :player1Id AND g.blackPlayer.id = :player2Id) " +
           "     OR (g.whitePlayer.id = :player2Id AND g.blackPlayer.id = :player1Id))")
    List<Game> findByTournamentIdAndPlayers(
            @Param("tournamentId") Long tournamentId,
            @Param("player1Id") Long player1Id,
            @Param("player2Id") Long player2Id);

    /**
     * Проверить, существует ли уже партия между двумя игроками в раунде
     */
    @Query("SELECT COUNT(g) > 0 FROM Game g " +
           "WHERE g.tournament.id = :tournamentId " +
           "AND g.roundNumber = :roundNumber " +
           "AND ((g.whitePlayer.id = :player1Id AND g.blackPlayer.id = :player2Id) " +
           "     OR (g.whitePlayer.id = :player2Id AND g.blackPlayer.id = :player1Id))")
    boolean existsByTournamentIdAndRoundAndPlayers(
            @Param("tournamentId") Long tournamentId,
            @Param("roundNumber") Integer roundNumber,
            @Param("player1Id") Long player1Id,
            @Param("player2Id") Long player2Id);

    /**
     * Найти незавершённые партии турнира
     */
    @Query("SELECT g FROM Game g " +
           "WHERE g.tournament.id = :tournamentId " +
           "AND g.status IN ('SCHEDULED', 'IN_PROGRESS') " +
           "ORDER BY g.roundNumber ASC, g.boardNumber ASC")
    List<Game> findUnfinishedByTournamentId(@Param("tournamentId") Long tournamentId);

    /**
     * Подсчитать количество завершённых партий в раунде
     */
    @Query("SELECT COUNT(g) FROM Game g " +
           "WHERE g.tournament.id = :tournamentId " +
           "AND g.roundNumber = :roundNumber " +
           "AND g.status = 'FINISHED'")
    long countFinishedByTournamentIdAndRound(
            @Param("tournamentId") Long tournamentId, 
            @Param("roundNumber") Integer roundNumber);

    /**
     * Подсчитать общее количество партий в раунде
     */
    long countByTournamentIdAndRoundNumber(Long tournamentId, Integer roundNumber);

    /**
     * Найти максимальный номер раунда в турнире
     */
    @Query("SELECT MAX(g.roundNumber) FROM Game g WHERE g.tournament.id = :tournamentId")
    Optional<Integer> findMaxRoundByTournamentId(@Param("tournamentId") Long tournamentId);

    /**
     * Удалить все партии турнира (для отмены турнира)
     */
    void deleteByTournamentId(Long tournamentId);

    /**
     * Найти партии по статусу в турнире
     */
    List<Game> findByTournamentIdAndStatus(Long tournamentId, Game.GameStatus status);
}
