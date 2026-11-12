package com.manacommunity.api.fresh.dispatch.repository;

import com.manacommunity.api.fresh.dispatch.model.FreshPurchaseOrder;
import com.manacommunity.api.fresh.dispatch.model.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FreshPurchaseOrderRepository extends JpaRepository<FreshPurchaseOrder, Long> {
    Optional<FreshPurchaseOrder> findByPoNumber(String poNumber);
    List<FreshPurchaseOrder> findByVendorIdOrderByTargetDeliveryDateDesc(Long vendorId);
    List<FreshPurchaseOrder> findByVendorIdAndStatus(Long vendorId, PurchaseOrderStatus status);
    List<FreshPurchaseOrder> findByTargetDeliveryDate(LocalDate targetDeliveryDate);
}