package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InteriorProjectInvitationRepository extends JpaRepository<InteriorProjectInvitation, Long> {
    Optional<InteriorProjectInvitation> findByServiceRequestIdAndProviderId(Long requestId, Long providerId);
    List<InteriorProjectInvitation> findByProviderIdAndStatusInOrderByCreatedAtDesc(Long providerId, List<String> statuses);
    List<InteriorProjectInvitation> findByServiceRequestId(Long requestId);
}
