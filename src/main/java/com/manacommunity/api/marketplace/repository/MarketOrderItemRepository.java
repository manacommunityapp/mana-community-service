package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketOrderItemRepository extends JpaRepository<MarketOrderItem, Long> {
    List<MarketOrderItem> findByOrderId(Long orderId);
}
