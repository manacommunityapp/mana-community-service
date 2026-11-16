package com.manacommunity.api.fresh.weight.repository;

import com.manacommunity.api.fresh.weight.model.FreshWeightAdjustmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshWeightAdjustmentLogRepository extends JpaRepository<FreshWeightAdjustmentLog, Long> {

    List<FreshWeightAdjustmentLog> findByOrderId(Long orderId);

    List<FreshWeightAdjustmentLog> findByFulfilmentItemId(Long fulfilmentItemId);

    List<FreshWeightAdjustmentLog> findByCustomerId(Long customerId);

    Optional<FreshWeightAdjustmentLog> findByAdjustmentReference(String adjustmentReference);
}
