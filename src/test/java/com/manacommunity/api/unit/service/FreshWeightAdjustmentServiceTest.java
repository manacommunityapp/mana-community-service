package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentGroup;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.weight.dto.RecordWeightRequest;
import com.manacommunity.api.fresh.weight.dto.WeightAdjustmentDto;
import com.manacommunity.api.fresh.weight.model.FreshWeightAdjustmentLog;
import com.manacommunity.api.fresh.weight.model.WeightAdjustmentStatus;
import com.manacommunity.api.fresh.weight.repository.FreshWeightAdjustmentLogRepository;
import com.manacommunity.api.fresh.weight.service.FreshWeightAdjustmentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreshWeightAdjustmentServiceTest {

    @Mock
    private FreshFulfilmentItemRepository itemRepository;
    @Mock
    private FreshWeightAdjustmentLogRepository weightLogRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private FreshWeightAdjustmentService weightAdjustmentService;

    private FreshFulfilmentItem mockItem;
    private CommerceOrder mockOrder;
    private AppUser customer;

    @BeforeEach
    void setUp() {
        customer = AppUser.builder().id(201L).fullName("Pooja Sharma").build();
        mockOrder = CommerceOrder.builder().id(800L).orderNumber("ORD-202611-002").buyer(customer).build();
        FreshFulfilmentGroup mockGroup = FreshFulfilmentGroup.builder().id(10L).order(mockOrder).build();

        mockItem = FreshFulfilmentItem.builder()
                .id(50L)
                .fulfilmentGroup(mockGroup)
                .requestedQuantity(new BigDecimal("1.000"))
                .unitOfMeasure("KG")
                .unitPrice(new BigDecimal("100.00"))
                .billedPrice(new BigDecimal("100.00"))
                .isWeightAdjusted(false)
                .build();
    }

    @Test
    @DisplayName("recordActualWeight with under-weight within 15% adjusts price and refunds difference")
    void testUnderWeightWithinTolerance() {
        when(itemRepository.findById(50L)).thenReturn(Optional.of(mockItem));
        when(weightLogRepository.save(any(FreshWeightAdjustmentLog.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(50L)
                .actualWeightG(new BigDecimal("900.000")) // 900g instead of 1000g (-10%)
                .reason("Fresh catch scaled weight")
                .build();

        WeightAdjustmentDto result = weightAdjustmentService.recordActualWeight(req);

        assertThat(result.getAdjustmentStatus()).isEqualTo(WeightAdjustmentStatus.REFUNDED_TO_WALLET);
        assertThat(result.getWeightDifferencePct()).isEqualByComparingTo(new BigDecimal("-10.00"));
        assertThat(result.getAdjustedAmount()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(result.getRefundOrChargeAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(mockItem.getIsWeightAdjusted()).isTrue();
        assertThat(mockItem.getBilledPrice()).isEqualByComparingTo(new BigDecimal("90.00"));

        verify(itemRepository).save(mockItem);
        verify(weightLogRepository).save(any(FreshWeightAdjustmentLog.class));
    }

    @Test
    @DisplayName("recordActualWeight with over-weight within 15% adjusts price and charges difference")
    void testOverWeightWithinTolerance() {
        when(itemRepository.findById(50L)).thenReturn(Optional.of(mockItem));
        when(weightLogRepository.save(any(FreshWeightAdjustmentLog.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(50L)
                .actualWeightG(new BigDecimal("1080.000")) // 1080g (+8%)
                .build();

        WeightAdjustmentDto result = weightAdjustmentService.recordActualWeight(req);

        assertThat(result.getAdjustmentStatus()).isEqualTo(WeightAdjustmentStatus.CHARGED_TO_WALLET);
        assertThat(result.getWeightDifferencePct()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(result.getAdjustedAmount()).isEqualByComparingTo(new BigDecimal("108.00"));
        assertThat(result.getRefundOrChargeAmount()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    @DisplayName("recordActualWeight with excess weight beyond 15% marks PENDING_CUSTOMER_APPROVAL")
    void testToleranceExceededRequiresApproval() {
        when(itemRepository.findById(50L)).thenReturn(Optional.of(mockItem));
        when(weightLogRepository.save(any(FreshWeightAdjustmentLog.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(50L)
                .actualWeightG(new BigDecimal("1300.000")) // +30%, exceeds 15%
                .build();

        WeightAdjustmentDto result = weightAdjustmentService.recordActualWeight(req);

        assertThat(result.getAdjustmentStatus()).isEqualTo(WeightAdjustmentStatus.PENDING_CUSTOMER_APPROVAL);
        assertThat(result.getWeightDifferencePct()).isEqualByComparingTo(new BigDecimal("30.00"));
        // Billed price on item should remain untouched until approved
        assertThat(mockItem.getBilledPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
