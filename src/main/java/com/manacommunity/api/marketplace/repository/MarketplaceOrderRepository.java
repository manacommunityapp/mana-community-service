package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketplaceOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    Page<MarketplaceOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<MarketplaceOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<MarketplaceOrder> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, MarketplaceOrder.OrderStatus status, Pageable pageable);

    Optional<MarketplaceOrder> findByOrderNumber(String orderNumber);

    long countByBuyerIdAndStatus(Long buyerId, MarketplaceOrder.OrderStatus status);
}
