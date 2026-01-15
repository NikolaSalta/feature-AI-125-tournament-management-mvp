package com.tournament.management.repository;

import com.tournament.management.entity.Tournament;
import com.tournament.management.entity.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    
    List<Tournament> findByStatus(TournamentStatus status);
    
    List<Tournament> findByNameContainingIgnoreCase(String name);
}
