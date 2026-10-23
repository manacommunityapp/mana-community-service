package com.manacommunity.api.repository;

import com.manacommunity.api.model.ProcurementRFQ;
import com.manacommunity.api.model.RFQStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementRFQRepository extends JpaRepository<ProcurementRFQ, Long> {

    List<ProcurementRFQ> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<ProcurementRFQ> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, RFQStatus status);

    Optional<ProcurementRFQ> findByIdAndCommunityId(Long id, Long communityId);

    long countByCommunityIdAndStatusIn(Long communityId, List<RFQStatus> statuses);
}
