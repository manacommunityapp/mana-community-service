package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsPlayerTournamentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SportsPlayerTournamentStatsRepository extends JpaRepository<SportsPlayerTournamentStats, Long> {
    Optional<SportsPlayerTournamentStats> findByConfigIdAndPlayerId(Long configId, Long playerId);
    List<SportsPlayerTournamentStats> findByConfigIdOrderByTotalRunsDesc(Long configId);
    List<SportsPlayerTournamentStats> findByConfigIdOrderByTotalWicketsDesc(Long configId);
    List<SportsPlayerTournamentStats> findByConfigIdOrderByManOfMatchCountDesc(Long configId);
    List<SportsPlayerTournamentStats> findByPlayerId(Long playerId);
}
