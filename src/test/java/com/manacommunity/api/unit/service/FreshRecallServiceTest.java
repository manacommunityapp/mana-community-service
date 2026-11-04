package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentGroup;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.recall.dto.InitiateRecallRequest;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.model.*;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallOrderRepository;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallRepository;
import com.manacommunity.api.fresh.recall.service.FreshRecallService;
import com.manacommunity.api.freshkitchen.model.BatchStatus;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshRecallServiceTest {

    @Mock
    private FreshProductRecallRepository recallRepository;
    @Mock
    private FreshProductRecallOrderRepository recallOrderRepository;
    @Mock
    private FreshFulfilmentItemRepository fulfilmentItemRepository;
    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private FoodProductionBatchRepository batchRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private FreshRecallService recallService;

    private VendorProduct product;
    private FoodProductionBatch batch;
    private AppUser admin;
    private AppUser customer;
    private CommerceOrder order;
    private FreshFulfilmentItem item;

    @BeforeEach
    void setUp() {
        product = VendorProduct.builder().id(300L).name("Organic Paneer 200g").build();
        batch = FoodProductionBatch.builder()
                .id(40L)
                .batchNumber("BATCH-PAN-202611-01")
                .batchStatus(BatchStatus.RELEASED)
                .build();
        admin = AppUser.builder().id(1L).fullName("QA Director").build();
        customer = AppUser.builder().id(99L).fullName("Aditya Roy").build();
        order = CommerceOrder.builder().id(700L).orderNumber("ORD-202611-003").buyer(customer).build();
        FreshFulfilmentGroup group = FreshFulfilmentGroup.builder().id(80L).order(order).build();

        item = FreshFulfilmentItem.builder()
                .id(12L)
                .fulfilmentGroup(group)
                .vendorProduct(product)
                .batch(batch)
                .requestedQuantity(new BigDecimal("2.000"))
                .pickedQuantity(new BigDecimal("2.000"))
                .billedPrice(new BigDecimal("180.00"))
                .build();
    }

    @Test
    @DisplayName("initiateRecall quarantines batch, identifies affected orders and computes total refund")
    void testInitiateRecall() {
        when(vendorProductRepository.findById(300L)).thenReturn(Optional.of(product));
        when(batchRepository.findById(40L)).thenReturn(Optional.of(batch));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(recallRepository.save(any(FreshProductRecall.class))).thenAnswer(inv -> {
            FreshProductRecall r = inv.getArgument(0);
            if (r.getId() == null) r.setId(901L);
            return r;
        });
        when(fulfilmentItemRepository.findByBatchId(40L)).thenReturn(Collections.singletonList(item));

        InitiateRecallRequest req = InitiateRecallRequest.builder()
                .vendorProductId(300L)
                .batchId(40L)
                .severity(RecallSeverity.CRITICAL)
                .reason("Elevated coliform count in post-packaging retention test")
                .detailedRootCause("Chiller valve temperature fluctuation during sealing")
                .initiatedByUserId(1L)
                .build();

        ProductRecallDto result = recallService.initiateRecall(req);

        assertThat(result.getRecallReference()).startsWith("RCL-");
        assertThat(result.getSeverity()).isEqualTo(RecallSeverity.CRITICAL);
        assertThat(result.getStatus()).isEqualTo(RecallStatus.INITIATED);
        assertThat(result.getAffectedOrdersCount()).isEqualTo(1);
        assertThat(result.getAffectedCustomersCount()).isEqualTo(1);
        assertThat(result.getTotalRefundAmount()).isEqualByComparingTo(new BigDecimal("180.00"));
        assertThat(batch.getBatchStatus()).isEqualTo(BatchStatus.QUARANTINED);

        verify(batchRepository).save(batch);
        verify(recallOrderRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("processRecallRefunds marks customer notifications and completes recall")
    void testProcessRecallRefunds() {
        FreshProductRecall recall = FreshProductRecall.builder()
                .id(901L)
                .recallReference("RCL-202611-001")
                .vendorProduct(product)
                .initiatedByUser(admin)
                .status(RecallStatus.INITIATED)
                .build();

        FreshProductRecallOrder ro = FreshProductRecallOrder.builder()
                .id(1L)
                .recall(recall)
                .order(order)
                .customer(customer)
                .refundAmount(new BigDecimal("180.00"))
                .customerNotified(false)
                .refundProcessed(false)
                .build();

        when(recallRepository.findById(901L)).thenReturn(Optional.of(recall));
        when(recallOrderRepository.findByRecallId(901L)).thenReturn(Collections.singletonList(ro));
        when(recallRepository.save(any(FreshProductRecall.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRecallDto result = recallService.processRecallRefunds(901L);

        assertThat(result.getStatus()).isEqualTo(RecallStatus.COMPLETED);
        assertThat(ro.getCustomerNotified()).isTrue();
        assertThat(ro.getRefundProcessed()).isTrue();
        assertThat(ro.getNotificationSentAt()).isNotNull();

        verify(recallOrderRepository).saveAll(anyList());
    }
}
