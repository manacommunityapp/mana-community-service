package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsScoringConfigRepository extends JpaRepository<SportsScoringConfig, Long> {

    Optional<SportsScoringConfig> findByConfigId(Long configId);

    List<SportsScoringConfig> findBySportType(String sportType);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsScoringConfig s WHERE s.config.id = :configId")
    void deleteByTournamentConfigId(@Param("configId") Long configId);
}
