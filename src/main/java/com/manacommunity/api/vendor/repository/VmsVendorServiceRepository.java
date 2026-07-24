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

    Page<VmsVendorService> findByCommunityIdAndStatus(Long communityId, VmsVendorService.ServiceStatus status, Pageable pageable);

    Page<VmsVendorService> findByCommunityIdAndCategoryIdAndStatus(Long communityId, Long categoryId, VmsVendorService.ServiceStatus status, Pageable pageable);

    @Query("SELECT s FROM VmsVendorService s WHERE s.community.id = :communityId AND s.status = 'ACTIVE' " +
           "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<VmsVendorService> searchByCommunity(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);
}
