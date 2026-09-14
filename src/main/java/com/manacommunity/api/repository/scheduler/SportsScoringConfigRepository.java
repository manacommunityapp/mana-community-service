package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsScoringConfigRepository extends JpaRepository<SportsScoringConfig, Long> {

    Optional<SportsScoringConfig> findByConfigId(Long configId);

    List<SportsScoringConfig> findBySportType(String sportType);
}
