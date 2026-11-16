package com.manacommunity.api.fresh.dispatch.repository;

import com.manacommunity.api.fresh.dispatch.model.FreshPurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FreshPurchaseOrderItemRepository extends JpaRepository<FreshPurchaseOrderItem, Long> {
    List<FreshPurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);
}