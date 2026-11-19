package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.TournamentMerchandise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentMerchandiseRepository extends JpaRepository<TournamentMerchandise, Long> {
    List<TournamentMerchandise> findByTournamentIdAndIsAvailableTrue(Long tournamentId);
    List<TournamentMerchandise> findByTournamentId(Long tournamentId);
}
