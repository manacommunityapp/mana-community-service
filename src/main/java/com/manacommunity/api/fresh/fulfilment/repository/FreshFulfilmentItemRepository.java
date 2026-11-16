package com.manacommunity.api.fresh.fulfilment.repository;

import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreshFulfilmentItemRepository extends JpaRepository<FreshFulfilmentItem, Long> {

    List<FreshFulfilmentItem> findByFulfilmentGroupId(Long fulfilmentGroupId);

    List<FreshFulfilmentItem> findByOrderItemId(Long orderItemId);

    List<FreshFulfilmentItem> findByBatchId(Long batchId);
}
