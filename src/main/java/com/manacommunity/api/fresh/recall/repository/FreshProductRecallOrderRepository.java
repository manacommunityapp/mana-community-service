package com.manacommunity.api.fresh.recall.repository;

import com.manacommunity.api.fresh.recall.model.FreshProductRecallOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreshProductRecallOrderRepository extends JpaRepository<FreshProductRecallOrder, Long> {

    List<FreshProductRecallOrder> findByRecallId(Long recallId);

    List<FreshProductRecallOrder> findByCustomerId(Long customerId);

    List<FreshProductRecallOrder> findByOrderId(Long orderId);
}
