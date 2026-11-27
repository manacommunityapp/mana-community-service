package com.manacommunity.api.opsresilience.repository;

import com.manacommunity.api.opsresilience.entity.IncidentPostmortem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentPostmortemRepository extends JpaRepository<IncidentPostmortem, Long> {
    Optional<IncidentPostmortem> findByIncidentCode(String incidentCode);
}
