package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommercePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommercePaymentRepository extends JpaRepository<CommercePayment, Long> {
    Optional<CommercePayment> findByPaymentRef(String paymentRef);
    Optional<CommercePayment> findByOrderId(Long orderId);
}