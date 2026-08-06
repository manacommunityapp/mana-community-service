package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VmsReviewRepository extends JpaRepository<VmsReview, Long> {
    Page<VmsReview> findByVendorIdAndStatus(Long vendorId, String status, Pageable pageable);
    Page<VmsReview> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);
}
