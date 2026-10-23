package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.model.PatrolCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrolCheckpointRepository extends JpaRepository<PatrolCheckpoint, Long> {

    List<PatrolCheckpoint> findByCommunityIdAndActiveTrueOrderBySequenceOrderAsc(Long communityId);
}
