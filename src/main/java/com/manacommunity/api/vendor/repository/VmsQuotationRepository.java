package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsQuotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VmsQuotationRepository extends JpaRepository<VmsQuotation, Long> {
    Page<VmsQuotation> findByCommunityId(Long communityId, Pageable pageable);
    List<VmsQuotation> findByPurchaseRequestId(Long purchaseRequestId);
    Page<VmsQuotation> findByVendorId(Long vendorId, Pageable pageable);
    Optional<VmsQuotation> findByIdAndCommunityId(Long id, Long communityId);
}
