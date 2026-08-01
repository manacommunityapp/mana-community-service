package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsWorkOrderRepository extends JpaRepository<VmsWorkOrder, Long> {

    Page<VmsWorkOrder> findByCommunityId(Long communityId, Pageable pageable);

    Page<VmsWorkOrder> findByCommunityIdAndStatus(Long communityId, VmsWorkOrder.WorkOrderStatus status, Pageable pageable);

    Page<VmsWorkOrder> findByVendorId(Long vendorId, Pageable pageable);

    Page<VmsWorkOrder> findByVendorIdAndStatus(Long vendorId, VmsWorkOrder.WorkOrderStatus status, Pageable pageable);

    Optional<VmsWorkOrder> findByIdAndCommunityId(Long id, Long communityId);

    Optional<VmsWorkOrder> findByWorkOrderNumberAndCommunityId(String workOrderNumber, Long communityId);

    long countByCommunityIdAndStatus(Long communityId, VmsWorkOrder.WorkOrderStatus status);
}
