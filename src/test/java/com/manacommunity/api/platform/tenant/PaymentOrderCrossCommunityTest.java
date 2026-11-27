package com.manacommunity.api.platform.tenant;

import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that payment order queries for community B return nothing when only
 * community A has orders, ensuring financial data does not leak across tenants.
 */
@DataJpaTest
class PaymentOrderCrossCommunityTest extends CrossCommunityTestBase {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void communityB_cannotSee_communityA_paymentOrders() {
        // Arrange: persist a PaymentOrder for community A
        PaymentOrder order = PaymentOrder.builder()
                .communityId(COMMUNITY_A)
                .idempotencyKey("test-key-" + System.nanoTime())
                .gateway(PaymentGateway.MANUAL)
                .amount(BigDecimal.valueOf(500))
                .currency("INR")
                .status(PaymentOrderStatus.SUCCESS)
                .refundedAmount(BigDecimal.ZERO)
                .payerUserId(42L)
                .webhookReceivedAt(OffsetDateTime.now())
                .build();
        em.persist(order);
        em.flush();

        setTenantContext(COMMUNITY_B);

        // Act: query orders for community B
        Page<PaymentOrder> results = paymentOrderRepository.findByCommunityId(
                COMMUNITY_B, PageRequest.of(0, 10));

        // Assert: community B sees none of community A's orders
        assertThat(results.getContent()).isEmpty();
    }

    @Override
    protected void assertCommunityBCannotSeeA() {
        Page<PaymentOrder> results = paymentOrderRepository.findByCommunityId(
                COMMUNITY_B, PageRequest.of(0, 10));
        assertThat(results.getContent()).isEmpty();
    }
}
