package com.manacommunity.api.transaction.unit;

import com.manacommunity.api.transaction.adapter.*;
import com.manacommunity.api.transaction.engine.TransactionCoreEngine;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.enums.TransactionStatus;
import com.manacommunity.api.transaction.model.TransactionExecutionResult;
import com.manacommunity.api.transaction.model.TransactionIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainAdaptersTest {

    @Mock private TransactionCoreEngine coreEngine;

    @Test
    @DisplayName("Marketplace adapter creates intent with escrow and correct domain")
    void testMarketplaceAdapter() {
        MarketplaceTransactionAdapter adapter = new MarketplaceTransactionAdapter(coreEngine);

        when(coreEngine.executeTransaction(any())).thenReturn(TransactionExecutionResult.builder()
                .domain(TransactionDomain.MARKETPLACE)
                .status(TransactionStatus.IN_ESCROW)
                .build());

        adapter.processMarketplaceOrder(10L, 20L, 1L, new BigDecimal("1200.00"),
                new BigDecimal("216.00"), TransactionPaymentMethod.WALLET, "ORD-1234", List.of(), true);

        ArgumentCaptor<TransactionIntent> captor = ArgumentCaptor.forClass(TransactionIntent.class);
        verify(coreEngine).executeTransaction(captor.capture());

        TransactionIntent intent = captor.getValue();
        assertThat(intent.getDomain()).isEqualTo(TransactionDomain.MARKETPLACE);
        assertThat(intent.getPayerId()).isEqualTo(10L);
        assertThat(intent.getPayeeId()).isEqualTo(20L);
        assertThat(intent.isEscrowRequired()).isTrue();
    }

    @Test
    @DisplayName("Sports adapter creates intent for court booking")
    void testSportsAdapter() {
        SportsTransactionAdapter adapter = new SportsTransactionAdapter(coreEngine);

        when(coreEngine.executeTransaction(any())).thenReturn(TransactionExecutionResult.builder()
                .domain(TransactionDomain.SPORTS)
                .status(TransactionStatus.CAPTURED)
                .build());

        adapter.processCourtBooking(10L, 5L, 1L, new BigDecimal("350.00"),
                "Badminton", "Court A", TransactionPaymentMethod.WALLET, "07:00-08:00");

        ArgumentCaptor<TransactionIntent> captor = ArgumentCaptor.forClass(TransactionIntent.class);
        verify(coreEngine).executeTransaction(captor.capture());

        TransactionIntent intent = captor.getValue();
        assertThat(intent.getDomain()).isEqualTo(TransactionDomain.SPORTS);
        assertThat(intent.getAmount()).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("Food adapter creates intent for dining / kitchen order")
    void testFoodAdapter() {
        FoodTransactionAdapter adapter = new FoodTransactionAdapter(coreEngine);

        when(coreEngine.executeTransaction(any())).thenReturn(TransactionExecutionResult.builder()
                .domain(TransactionDomain.FOOD)
                .status(TransactionStatus.CAPTURED)
                .build());

        adapter.processFoodDiningOrder(10L, 15L, 1L, new BigDecimal("450.00"),
                new BigDecimal("22.50"), TransactionPaymentMethod.UPI, "FOOD-9988", List.of());

        ArgumentCaptor<TransactionIntent> captor = ArgumentCaptor.forClass(TransactionIntent.class);
        verify(coreEngine).executeTransaction(captor.capture());

        TransactionIntent intent = captor.getValue();
        assertThat(intent.getDomain()).isEqualTo(TransactionDomain.FOOD);
        assertThat(intent.getPaymentMethod()).isEqualTo(TransactionPaymentMethod.UPI);
    }
}
