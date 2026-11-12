package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface VmsVendorRegistrationRepository extends JpaRepository<VmsVendorRegistration, Long> {
    Page<VmsVendorRegistration> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);
    Page<VmsVendorRegistration> findByCommunityId(Long communityId, Pageable pageable);
    Optional<VmsVendorRegistration> findByIdAndCommunityId(Long id, Long communityId);
    boolean existsByCommunityIdAndApplicantUserIdAndStatusIn(Long communityId, Long applicantUserId,
                                                               Collection<VmsVendorRegistration.RegistrationStatus> statuses);
    List<VmsVendorRegistration> findByCommunityIdAndApplicantUserIdOrderByCreatedAtDesc(Long communityId, Long applicantUserId);
}
