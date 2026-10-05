package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceRefundRepository extends JpaRepository<CommerceRefund, Long> {
    List<CommerceRefund> findByOrderId(Long orderId);
}
