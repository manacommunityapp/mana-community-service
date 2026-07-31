package com.cpos.domain.property.repository;

import com.cpos.domain.property.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByPropertyCodeAndIsDeletedFalse(String propertyCode);

    Page<Property> findByTenantIdAndCommunityIdAndIsDeletedFalse(UUID tenantId, UUID communityId, Pageable pageable);

    List<Property> findByTenantIdAndOccupancyStatusAndIsDeletedFalse(UUID tenantId, String occupancyStatus);

    List<Property> findByTenantIdAndPropertyStatusAndIsDeletedFalse(UUID tenantId, String propertyStatus);

    @Query("SELECT p FROM Property p WHERE p.tenantId = :tenantId AND p.communityId = :communityId " +
           "AND (:type IS NULL OR p.propertyTypeCode = :type) " +
           "AND (:status IS NULL OR p.occupancyStatus = :status) " +
           "AND p.isDeleted = false")
    Page<Property> searchProperties(
            @Param("tenantId") UUID tenantId,
            @Param("communityId") UUID communityId,
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable);

    long countByTenantIdAndCommunityIdAndOccupancyStatusAndIsDeletedFalse(UUID tenantId, UUID communityId, String status);
}
