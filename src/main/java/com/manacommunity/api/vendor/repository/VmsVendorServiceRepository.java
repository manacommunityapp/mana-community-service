package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VmsVendorServiceRepository extends JpaRepository<VmsVendorService, Long> {

    List<VmsVendorService> findByVendorIdAndStatus(Long vendorId, VmsVendorService.ServiceStatus status);

    @Query("SELECT s FROM VmsVendorService s WHERE s.vendor.community.id = :communityId AND s.status = :status")
    Page<VmsVendorService> findByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") VmsVendorService.ServiceStatus status, Pageable pageable);

    @Query("SELECT s FROM VmsVendorService s WHERE s.vendor.community.id = :communityId AND s.category.id = :categoryId AND s.status = :status")
    Page<VmsVendorService> findByCommunityIdAndCategoryIdAndStatus(@Param("communityId") Long communityId, @Param("categoryId") Long categoryId, @Param("status") VmsVendorService.ServiceStatus status, Pageable pageable);

    @Query("SELECT s FROM VmsVendorService s WHERE s.vendor.community.id = :communityId AND s.status = 'ACTIVE' " +
           "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<VmsVendorService> searchByCommunity(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);
}
