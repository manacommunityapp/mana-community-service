package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SportsTournamentConfigRepository extends JpaRepository<SportsTournamentConfig, Long> {
    List<SportsTournamentConfig> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<SportsTournamentConfig> findByStatus(SportsTournamentConfig.TournamentStatus status);
    List<SportsTournamentConfig> findByEventId(Long eventId);
}
