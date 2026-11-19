package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.DeliveryBatchOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryBatchOrderRepository extends JpaRepository<DeliveryBatchOrder, Long> {
    List<DeliveryBatchOrder> findByBatchId(Long batchId);
}
