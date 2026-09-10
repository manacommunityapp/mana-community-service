package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketOrderStatusHistoryRepository extends JpaRepository<MarketOrderStatusHistory, Long> {
    List<MarketOrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
