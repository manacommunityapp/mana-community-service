package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.DeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {
    List<DeliveryBatch> findBySlotId(Long slotId);
}
