package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsVendorFavoriteRepository extends JpaRepository<VmsVendorFavorite, Long> {
    Page<VmsVendorFavorite> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);
    Optional<VmsVendorFavorite> findByVendorIdAndUserId(Long vendorId, Long userId);
    boolean existsByVendorIdAndUserId(Long vendorId, Long userId);
}
