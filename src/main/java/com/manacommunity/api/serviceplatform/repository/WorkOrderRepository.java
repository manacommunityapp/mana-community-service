package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByServiceRequestId(Long serviceRequestId);

    List<WorkOrder> findByProviderIdAndStatus(Long providerId, WorkOrderStatus status);

    List<WorkOrder> findByProviderId(Long providerId);

    List<WorkOrder> findByCommunityId(Long communityId);

    Page<WorkOrder> findByProviderIdAndStatusIn(Long providerId,
                                                 List<WorkOrderStatus> statuses,
                                                 Pageable pageable);

    Page<WorkOrder> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    Page<WorkOrder> findByCommunityIdAndStatus(Long communityId,
                                                WorkOrderStatus status,
                                                Pageable pageable);

    Page<WorkOrder> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);
}
