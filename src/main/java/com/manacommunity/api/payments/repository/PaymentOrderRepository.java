package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentOrder> findByGatewayAndGatewayOrderId(PaymentGateway gateway, String gatewayOrderId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<PaymentOrder> findByReferenceTypeAndReferenceId(
            PaymentReferenceType referenceType, Long referenceId);

    Page<PaymentOrder> findByCommunityIdAndStatus(
            Long communityId, PaymentOrderStatus status, Pageable pageable);
}
