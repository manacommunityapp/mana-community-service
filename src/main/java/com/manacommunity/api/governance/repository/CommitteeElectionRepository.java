package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.CommitteeElection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitteeElectionRepository extends JpaRepository<CommitteeElection, Long> {
    List<CommitteeElection> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<CommitteeElection> findByCommunityIdAndStatus(Long communityId, String status);
}
