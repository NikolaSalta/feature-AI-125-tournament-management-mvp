package com.tournament.management.repository;

import com.tournament.management.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    List<Match> findByTournamentId(Long tournamentId);
    
    List<Match> findByTournamentIdAndRound(Long tournamentId, Integer round);
    
    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId AND (m.player1.id = :playerId OR m.player2.id = :playerId)")
    List<Match> findByTournamentIdAndPlayerId(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);
    
    @Query("SELECT DISTINCT m.round FROM Match m WHERE m.tournament.id = :tournamentId ORDER BY m.round")
    List<Integer> findDistinctRoundsByTournamentId(@Param("tournamentId") Long tournamentId);
}
