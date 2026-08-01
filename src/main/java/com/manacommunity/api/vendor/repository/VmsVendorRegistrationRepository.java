package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsVendorRegistrationRepository extends JpaRepository<VmsVendorRegistration, Long> {
    Page<VmsVendorRegistration> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);
    Page<VmsVendorRegistration> findByCommunityId(Long communityId, Pageable pageable);
    Optional<VmsVendorRegistration> findByIdAndCommunityId(Long id, Long communityId);
}
