package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.dispatch.dto.*;
import com.manacommunity.api.fresh.dispatch.model.*;
import com.manacommunity.api.fresh.dispatch.repository.*;
import com.manacommunity.api.fresh.dispatch.service.FreshDispatchSettlementService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshDispatchSettlementServiceTest {

    @Mock
    private FreshPurchaseOrderRepository poRepository;
    @Mock
    private FreshPurchaseOrderItemRepository poItemRepository;
    @Mock
    private FreshDeliveryBatchRepository deliveryBatchRepository;
    @Mock
    private FreshDeliveryItemRepository deliveryItemRepository;
    @Mock
    private FreshVendorSettlementRepository settlementRepository;
    @Mock
    private VmsVendorRepository vendorRepository;
    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private FreshDispatchSettlementService service;

    private VmsVendor testVendor;
    private VendorProduct testProduct;
    private Community testCommunity;

    @BeforeEach
    void setUp() {
        testVendor = VmsVendor.builder().id(12L).businessName("Green Meadows Farms").build();
        testProduct = VendorProduct.builder().id(301L).name("Fresh Cut Dragon Fruit").build();
        testCommunity = Community.builder().id(7L).name("Prestige Lakeside").build();
    }

    @Test
    void testCreatePurchaseOrder_CalculatesTotalCostAndItems() {
        when(vendorRepository.findById(12L)).thenReturn(Optional.of(testVendor));
        when(vendorProductRepository.findById(301L)).thenReturn(Optional.of(testProduct));
        when(poRepository.save(any(FreshPurchaseOrder.class))).thenAnswer(i -> {
            FreshPurchaseOrder po = i.getArgument(0);
            po.setId(10L);
            return po;
        });

        CreatePurchaseOrderRequest.ItemRequest itemReq = new CreatePurchaseOrderRequest.ItemRequest();
        itemReq.setVendorProductId(301L);
        itemReq.setQuantity(new BigDecimal("50.00"));
        itemReq.setUnitCost(new BigDecimal("80.00"));
        itemReq.setDemandSource(DemandSource.SUBSCRIPTION);

        CreatePurchaseOrderRequest req = CreatePurchaseOrderRequest.builder()
                .vendorId(12L)
                .targetDeliveryDate(LocalDate.now().plusDays(1))
                .notes("Morning batch requirement")
                .items(List.of(itemReq))
                .build();

        PurchaseOrderDto result = service.createPurchaseOrder(req);

        assertNotNull(result);
        assertEquals(new BigDecimal("4000.00"), result.getTotalAmount());
        assertEquals(1, result.getItems().size());
        assertEquals(PurchaseOrderStatus.SENT_TO_VENDOR, result.getStatus());
        verify(poRepository, times(1)).save(any(FreshPurchaseOrder.class));
    }

    @Test
    void testCreateDeliveryBatch_CreatesItemsAndBatch() {
        when(communityRepository.findById(7L)).thenReturn(Optional.of(testCommunity));
        when(deliveryBatchRepository.save(any(FreshDeliveryBatch.class))).thenAnswer(i -> {
            FreshDeliveryBatch b = i.getArgument(0);
            b.setId(20L);
            return b;
        });

        CreateDeliveryBatchRequest.DeliveryItemRequest item = new CreateDeliveryBatchRequest.DeliveryItemRequest();
        item.setCommerceOrderId(1001L);
        item.setCustomerName("Rajesh K");
        item.setTower("Tower B");
        item.setFlatNo("1204");
        item.setSortSequence(1);

        CreateDeliveryBatchRequest req = CreateDeliveryBatchRequest.builder()
                .communityId(7L)
                .deliveryDate(LocalDate.now())
                .handlingClass(HandlingClass.CHILLED_COLD_CHAIN)
                .items(List.of(item))
                .build();

        DeliveryBatchDto result = service.createDeliveryBatch(req);

        assertNotNull(result);
        assertEquals(7L, result.getCommunityId());
        assertEquals(1, result.getItems().size());
        assertEquals(DeliveryBatchStatus.PLANNED, result.getStatus());
        verify(deliveryBatchRepository, times(1)).save(any(FreshDeliveryBatch.class));
    }

    @Test
    void testUpdateHandover_SetsDeliveredStatusAndTimestamp() {
        FreshDeliveryItem item = FreshDeliveryItem.builder()
                .id(55L)
                .handoverStatus(HandoverStatus.PENDING)
                .build();

        when(deliveryItemRepository.findById(55L)).thenReturn(Optional.of(item));
        when(deliveryItemRepository.save(any(FreshDeliveryItem.class))).thenAnswer(i -> i.getArgument(0));

        UpdateHandoverRequest req = UpdateHandoverRequest.builder()
                .handoverStatus(HandoverStatus.DELIVERED)
                .proofImageUrl("https://s3.amazonaws.com/proofs/doorstep_55.jpg")
                .notes("Left at door with security guard approval")
                .build();

        DeliveryItemDto result = service.updateHandover(55L, req);

        assertEquals(HandoverStatus.DELIVERED, result.getHandoverStatus());
        assertNotNull(result.getDeliveredAt());
        assertEquals("https://s3.amazonaws.com/proofs/doorstep_55.jpg", result.getProofImageUrl());
    }

    @Test
    void testCreateSettlement_CalculatesNetPayableCorrectly() {
        when(vendorRepository.findById(12L)).thenReturn(Optional.of(testVendor));
        when(settlementRepository.save(any(FreshVendorSettlement.class))).thenAnswer(i -> {
            FreshVendorSettlement s = i.getArgument(0);
            s.setId(88L);
            return s;
        });

        CreateSettlementRequest req = CreateSettlementRequest.builder()
                .vendorId(12L)
                .settlementPeriodStart(LocalDate.now().minusDays(7))
                .settlementPeriodEnd(LocalDate.now().minusDays(1))
                .grossSalesAmount(new BigDecimal("10000.00"))
                .manaCommissionAmount(new BigDecimal("1000.00"))
                .deliveryFeesCollected(new BigDecimal("500.00"))
                .gatewayFeesDeducted(new BigDecimal("200.00"))
                .build();

        VendorSettlementDto result = service.createSettlement(req);

        assertNotNull(result);
        // 10000 - 1000 + 500 - 200 = 9300.00
        assertEquals(new BigDecimal("9300.00"), result.getNetPayableAmount());
        assertEquals(SettlementStatus.PENDING, result.getStatus());
        verify(settlementRepository, times(1)).save(any(FreshVendorSettlement.class));
    }

    @Test
    void testProcessSettlement_SetsUtrAndProcessedStatus() {
        FreshVendorSettlement s = FreshVendorSettlement.builder()
                .id(88L)
                .vendor(testVendor)
                .status(SettlementStatus.APPROVED)
                .build();

        when(settlementRepository.findById(88L)).thenReturn(Optional.of(s));
        when(settlementRepository.save(any(FreshVendorSettlement.class))).thenAnswer(i -> i.getArgument(0));

        ProcessSettlementRequest req = ProcessSettlementRequest.builder()
                .payoutUtrReference("HDFC000123456789")
                .build();

        VendorSettlementDto result = service.processSettlement(88L, req);

        assertEquals(SettlementStatus.PROCESSED, result.getStatus());
        assertEquals("HDFC000123456789", result.getPayoutUtrReference());
        assertNotNull(result.getProcessedAt());
    }
}