package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsPurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsPurchaseOrderRepository extends JpaRepository<VmsPurchaseOrder, Long> {
    Page<VmsPurchaseOrder> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsPurchaseOrder> findByCommunityIdAndStatus(Long communityId, VmsPurchaseOrder.PurchaseOrderStatus status, Pageable pageable);
    Page<VmsPurchaseOrder> findByVendorId(Long vendorId, Pageable pageable);
    Optional<VmsPurchaseOrder> findByIdAndCommunityId(Long id, Long communityId);
}
