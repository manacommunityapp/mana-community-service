package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsScoringCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SportsScoringCommandRepository extends JpaRepository<SportsScoringCommand, UUID> {
    Optional<SportsScoringCommand> findByCommandId(UUID commandId);
    Optional<SportsScoringCommand> findTopByMatchIdOrderBySequenceNoDesc(Long matchId);
}
