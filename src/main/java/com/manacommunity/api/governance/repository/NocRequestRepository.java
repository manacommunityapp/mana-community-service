package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.NocRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NocRequestRepository extends JpaRepository<NocRequest, Long> {
    List<NocRequest> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<NocRequest> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
    List<NocRequest> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
