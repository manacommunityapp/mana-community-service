package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    Page<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    Page<ServiceRequest> findByCommunityIdAndStatus(Long communityId,
                                                     ServiceRequestStatus status,
                                                     Pageable pageable);

    Page<ServiceRequest> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<ServiceRequest> findByAssignedProviderIdAndStatusIn(Long providerId,
                                                              java.util.List<ServiceRequestStatus> statuses,
                                                              Pageable pageable);

    Page<ServiceRequest> findByCategoryIdAndStatus(Long categoryId,
                                                    ServiceRequestStatus status,
                                                    Pageable pageable);
}
