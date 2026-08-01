package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VmsVendorRepository extends JpaRepository<VmsVendor, Long> {

    Page<VmsVendor> findByCommunityIdAndStatus(Long communityId, VmsVendor.VendorStatus status, Pageable pageable);

    Page<VmsVendor> findByCommunityId(Long communityId, Pageable pageable);

    Page<VmsVendor> findByCommunityIdAndCategoryId(Long communityId, Long categoryId, Pageable pageable);

    Optional<VmsVendor> findByIdAndCommunityId(Long id, Long communityId);

    Optional<VmsVendor> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<VmsVendor> findByUserIdAndStatus(Long userId, VmsVendor.VendorStatus status);

    @Query("SELECT v FROM VmsVendor v WHERE v.community.id = :communityId AND v.status = 'APPROVED' " +
           "AND (LOWER(v.businessName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(v.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<VmsVendor> searchByCommunity(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);

    long countByCommunityIdAndStatus(Long communityId, VmsVendor.VendorStatus status);
}
