package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsScoringCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SportsScoringCommandRepository extends JpaRepository<SportsScoringCommand, UUID> {
    Optional<SportsScoringCommand> findByCommandId(UUID commandId);
    Optional<SportsScoringCommand> findTopByMatchIdOrderBySequenceNoDesc(Long matchId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsScoringCommand c WHERE c.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
