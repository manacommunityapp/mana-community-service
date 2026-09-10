package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsTournamentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SportsTournamentGroupRepository extends JpaRepository<SportsTournamentGroup, Long> {
    List<SportsTournamentGroup> findByConfigIdOrderByGroupOrder(Long configId);
}
