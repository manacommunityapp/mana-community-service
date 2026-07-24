package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProviderServiceOfferingRepository extends JpaRepository<ProviderServiceOffering, Long> {

    List<ProviderServiceOffering> findByProviderIdAndAvailableTrue(Long providerId);

    List<ProviderServiceOffering> findByProviderId(Long providerId);

    Page<ProviderServiceOffering> findByCategoryIdAndAvailableTrue(Long categoryId, Pageable pageable);

    @Query("SELECT o FROM ProviderServiceOffering o " +
           "JOIN o.provider p " +
           "WHERE o.category.id = :categoryId " +
           "AND o.available = true " +
           "AND p.community.id = :communityId " +
           "AND p.verificationStatus = 'VERIFIED'")
    Page<ProviderServiceOffering> findVerifiedByCategoryAndCommunity(
            @Param("categoryId") Long categoryId,
            @Param("communityId") Long communityId,
            Pageable pageable);

    boolean existsByProviderIdAndCategoryId(Long providerId, Long categoryId);
}
