package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsTournamentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SportsTournamentGroupRepository extends JpaRepository<SportsTournamentGroup, Long> {
    List<SportsTournamentGroup> findByConfigIdOrderByGroupOrder(Long configId);
}
