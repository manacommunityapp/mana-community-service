package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceOrderItemRepository extends JpaRepository<CommerceOrderItem, Long> {
    List<CommerceOrderItem> findByOrderId(Long orderId);
}