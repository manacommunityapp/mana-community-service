package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsPurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsPurchaseRequestRepository extends JpaRepository<VmsPurchaseRequest, Long> {
    Page<VmsPurchaseRequest> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsPurchaseRequest> findByCommunityIdAndStatus(Long communityId, VmsPurchaseRequest.PurchaseRequestStatus status, Pageable pageable);
    Optional<VmsPurchaseRequest> findByIdAndCommunityId(Long id, Long communityId);
}
