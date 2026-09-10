package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsTournamentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SportsTournamentConfigRepository extends JpaRepository<SportsTournamentConfig, Long> {
    List<SportsTournamentConfig> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<SportsTournamentConfig> findByStatus(SportsTournamentConfig.TournamentStatus status);
}
