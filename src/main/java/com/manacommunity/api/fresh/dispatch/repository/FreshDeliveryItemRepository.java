package com.manacommunity.api.fresh.dispatch.repository;

import com.manacommunity.api.fresh.dispatch.model.FreshDeliveryItem;
import com.manacommunity.api.fresh.dispatch.model.HandoverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FreshDeliveryItemRepository extends JpaRepository<FreshDeliveryItem, Long> {
    List<FreshDeliveryItem> findByDeliveryBatchIdOrderBySortSequenceAsc(Long deliveryBatchId);
    List<FreshDeliveryItem> findByDeliveryBatchIdAndHandoverStatus(Long deliveryBatchId, HandoverStatus status);
}