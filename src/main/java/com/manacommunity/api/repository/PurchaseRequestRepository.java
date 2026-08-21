package com.manacommunity.api.repository;

import com.manacommunity.api.model.ProcurementStatus;
import com.manacommunity.api.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<PurchaseRequest> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, ProcurementStatus status);
    java.util.Optional<PurchaseRequest> findByIdAndCommunityId(Long id, Long communityId);

    /** @deprecated Use community-scoped variants. */
    @Deprecated
    List<PurchaseRequest> findByStatusOrderByCreatedAtDesc(ProcurementStatus status);
    @Deprecated
    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(ProcurementStatus status);
}
