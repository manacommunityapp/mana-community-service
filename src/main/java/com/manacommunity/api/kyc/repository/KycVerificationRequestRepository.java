package com.manacommunity.api.kyc.repository;

import com.manacommunity.api.kyc.entity.KycVerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KycVerificationRequestRepository extends JpaRepository<KycVerificationRequest, Long> {
    Optional<KycVerificationRequest> findTopByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<KycVerificationRequest> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
    List<KycVerificationRequest> findByResidentIdOrderByCreatedAtDesc(Long residentId);
}
