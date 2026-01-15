package com.tournament.management.repository;

import com.tournament.management.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    
    Optional<Result> findByMatchId(Long matchId);
    
    @Query("SELECT r FROM Result r WHERE r.match.tournament.id = :tournamentId")
    List<Result> findByTournamentId(@Param("tournamentId") Long tournamentId);
    
    @Query("SELECT r FROM Result r WHERE r.match.tournament.id = :tournamentId AND (r.match.player1.id = :playerId OR r.match.player2.id = :playerId)")
    List<Result> findByTournamentIdAndPlayerId(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);
}
