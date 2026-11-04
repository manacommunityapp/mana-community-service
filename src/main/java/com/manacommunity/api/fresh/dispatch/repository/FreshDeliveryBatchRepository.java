package com.manacommunity.api.fresh.dispatch.repository;

import com.manacommunity.api.fresh.dispatch.model.DeliveryBatchStatus;
import com.manacommunity.api.fresh.dispatch.model.FreshDeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FreshDeliveryBatchRepository extends JpaRepository<FreshDeliveryBatch, Long> {
    Optional<FreshDeliveryBatch> findByBatchCode(String batchCode);
    List<FreshDeliveryBatch> findByCommunityIdAndDeliveryDate(Long communityId, LocalDate deliveryDate);
    List<FreshDeliveryBatch> findByStatus(DeliveryBatchStatus status);
}