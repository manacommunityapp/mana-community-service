package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.CommitteePosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitteePositionRepository extends JpaRepository<CommitteePosition, Long> {
    List<CommitteePosition> findByCommunityId(Long communityId);
}
