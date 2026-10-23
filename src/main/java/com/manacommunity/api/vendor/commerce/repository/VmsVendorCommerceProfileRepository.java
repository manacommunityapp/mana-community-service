package com.manacommunity.api.vendor.commerce.repository;

import com.manacommunity.api.vendor.commerce.model.VmsVendorCommerceProfile;
import com.manacommunity.api.vendor.commerce.model.VmsVendorCommerceProfile.CommerceVendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VmsVendorCommerceProfileRepository extends JpaRepository<VmsVendorCommerceProfile, Long> {

    Optional<VmsVendorCommerceProfile> findByVendorId(Long vendorId);

    boolean existsByVendorId(Long vendorId);

    @Query("SELECT p FROM VmsVendorCommerceProfile p " +
           "JOIN p.vendor v " +
           "WHERE v.community.id = :communityId")
    Page<VmsVendorCommerceProfile> findByCommunityId(Long communityId, Pageable pageable);

    @Query("SELECT p FROM VmsVendorCommerceProfile p " +
           "JOIN p.vendor v " +
           "WHERE v.community.id = :communityId AND p.commerceStatus = :status")
    Page<VmsVendorCommerceProfile> findByCommunityIdAndStatus(Long communityId, CommerceVendorStatus status, Pageable pageable);

    @Query("SELECT p FROM VmsVendorCommerceProfile p " +
           "JOIN p.vendor v " +
           "WHERE v.community.id = :communityId AND p.communityVisible = TRUE AND p.commerceStatus = 'VERIFIED'")
    Page<VmsVendorCommerceProfile> findVerifiedVisibleByCommunity(Long communityId, Pageable pageable);
}
