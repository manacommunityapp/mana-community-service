package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderItemRepository;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.service.CommerceWalletService;
import com.manacommunity.api.fresh.catalog.model.DeliveryMode;
import com.manacommunity.api.fresh.catalog.model.FreshProductCatalogProfile;
import com.manacommunity.api.fresh.catalog.model.FulfilmentOwnerType;
import com.manacommunity.api.fresh.catalog.repository.FreshProductCatalogProfileRepository;
import com.manacommunity.api.fresh.fulfilment.dto.FulfilmentGroupDto;
import com.manacommunity.api.fresh.fulfilment.model.*;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentGroupRepository;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.fulfilment.service.FreshFulfilmentGroupService;
import com.manacommunity.api.fresh.recall.dto.InitiateRecallRequest;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.model.FreshProductRecall;
import com.manacommunity.api.fresh.recall.model.RecallSeverity;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallOrderRepository;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallRepository;
import com.manacommunity.api.fresh.recall.service.FreshRecallService;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import com.manacommunity.api.fresh.weight.dto.RecordWeightRequest;
import com.manacommunity.api.fresh.weight.dto.WeightAdjustmentDto;
import com.manacommunity.api.fresh.weight.model.FreshWeightAdjustmentLog;
import com.manacommunity.api.fresh.weight.model.WeightAdjustmentStatus;
import com.manacommunity.api.fresh.weight.repository.FreshWeightAdjustmentLogRepository;
import com.manacommunity.api.fresh.weight.service.FreshWeightAdjustmentService;
import com.manacommunity.api.freshkitchen.model.BatchStatus;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.freshkitchen.model.VendorProductionCapacity;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.freshkitchen.repository.VendorProductionCapacityRepository;
import com.manacommunity.api.freshkitchen.scheduler.DailyBasketScheduler;
import com.manacommunity.api.freshkitchen.service.VendorProductionCapacityService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.manacommunity.api.user.security.UserPrincipal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FreshPrecautionaryHardeningTest {

    // ── Fulfilment Group Mocks ──
    @Mock private CommerceOrderRepository orderRepository;
    @Mock private CommerceOrderItemRepository orderItemRepository;
    @Mock private FreshFulfilmentGroupRepository groupRepository;
    @Mock private FreshFulfilmentItemRepository fulfilmentItemRepository;
    @Mock private VendorProductRepository vendorProductRepository;
    @Mock private FreshProductCatalogProfileRepository catalogProfileRepository;
    @InjectMocks private FreshFulfilmentGroupService fulfilmentGroupService;

    // ── Recall Mocks ──
    @Mock private FreshProductRecallRepository recallRepository;
    @Mock private FreshProductRecallOrderRepository recallOrderRepository;
    @Mock private FoodProductionBatchRepository batchRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private CommerceWalletService walletService;
    @Mock private AuditService auditService;
    @InjectMocks private FreshRecallService recallService;

    // ── Weight Adjustment Mocks ──
    @Mock private FreshWeightAdjustmentLogRepository weightLogRepository;
    @InjectMocks private FreshWeightAdjustmentService weightAdjustmentService;

    // ── Vendor Capacity Mocks ──
    @Mock private VendorProductionCapacityRepository capacityRepository;
    @InjectMocks private VendorProductionCapacityService capacityService;

    private Community community;
    private AppUser customer;
    private VmsVendor vendor;

    @BeforeEach
    void setUp() {
        community = Community.builder().name("Palm Meadows").build();
        community.setId(10L);

        customer = AppUser.builder().fullName("Suresh Menon").email("suresh@mana.in").community(community).build();
        customer.setId(101L);

        vendor = VmsVendor.builder().businessName("Deccan Fresh Agro").build();
        vendor.setId(55L);
    }

    // ── 1. FSSAI Schedule 4: Dietary Segregation (Non-Veg vs Veg) ──

    @Test
    @DisplayName("FSSAI Schedule 4: Chicken and Idli Batter from same vendor must be segregated into separate fulfilment groups")
    void testDietarySegregationSeparatesChickenFromBatters() {
        CommerceOrder order = CommerceOrder.builder()
                .orderNumber("ORD-DIETARY-001")
                .buyer(customer)
                .community(community)
                .status(CommerceOrderStatus.CONFIRMED)
                .build();
        order.setId(201L);

        // Product 1: Fresh Country Chicken (Whole) -> Capability: CHICKEN_SUPPLIER
        VendorProduct chicken = VendorProduct.builder().name("Country Chicken (Cleaned)").vendor(vendor).build();
        chicken.setId(501L);
        FreshProductCatalogProfile chickenProfile = FreshProductCatalogProfile.builder()
                .vendorProduct(chicken)
                .capabilityType(FreshVendorCapabilityType.CHICKEN_SUPPLIER)
                .fulfilmentOwnerType(FulfilmentOwnerType.VENDOR)
                .deliveryMode(DeliveryMode.VENDOR_DELIVERY)
                .storageInstruction("Cold Chain 2-4°C")
                .build();

        // Product 2: Classic Idli/Dosa Batter -> Capability: BATTER_MANUFACTURER
        VendorProduct batter = VendorProduct.builder().name("Idli Batter 1kg").vendor(vendor).build();
        batter.setId(502L);
        FreshProductCatalogProfile batterProfile = FreshProductCatalogProfile.builder()
                .vendorProduct(batter)
                .capabilityType(FreshVendorCapabilityType.BATTER_MANUFACTURER)
                .fulfilmentOwnerType(FulfilmentOwnerType.VENDOR)
                .deliveryMode(DeliveryMode.VENDOR_DELIVERY)
                .storageInstruction("Cold Chain 2-4°C")
                .build();

        CommerceOrderItem item1 = CommerceOrderItem.builder()
                .order(order)
                .productId("501")
                .title("Country Chicken (Cleaned)")
                .unitPrice(new BigDecimal("350.00"))
                .quantity(1)
                .totalPrice(new BigDecimal("350.00"))
                .build();
        item1.setId(1001L);

        CommerceOrderItem item2 = CommerceOrderItem.builder()
                .order(order)
                .productId("502")
                .title("Idli Batter 1kg")
                .unitPrice(new BigDecimal("90.00"))
                .quantity(2)
                .totalPrice(new BigDecimal("180.00"))
                .build();
        item2.setId(1002L);

        when(orderRepository.findById(201L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(201L)).thenReturn(List.of(item1, item2));

        when(vendorProductRepository.findById(501L)).thenReturn(Optional.of(chicken));
        when(vendorProductRepository.findById(502L)).thenReturn(Optional.of(batter));

        when(catalogProfileRepository.findByVendorProductId(501L)).thenReturn(Optional.of(chickenProfile));
        when(catalogProfileRepository.findByVendorProductId(502L)).thenReturn(Optional.of(batterProfile));

        when(groupRepository.save(any(FreshFulfilmentGroup.class))).thenAnswer(i -> {
            FreshFulfilmentGroup g = i.getArgument(0);
            g.setId(new Random().nextLong(1000, 9999));
            return g;
        });

        List<FulfilmentGroupDto> groups = fulfilmentGroupService.splitOrderIntoFulfilmentGroups(201L);

        // Verification: Even though both items are from Deccan Fresh Agro and both require cold chain,
        // they MUST NOT be merged into a single tote bag due to FSSAI non-veg cross-contamination rules.
        assertThat(groups).hasSize(2);

        Set<DietaryCategory> dietaryCategories = new HashSet<>();
        for (FulfilmentGroupDto g : groups) {
            dietaryCategories.add(g.getDietaryCategory());
            assertThat(g.getHandoverOtp()).isNotNull().hasSize(6);
        }

        assertThat(dietaryCategories).containsExactlyInAnyOrder(DietaryCategory.POULTRY, DietaryCategory.VEGETARIAN);
    }

    // ── 2. Recall Emergency In-Flight Interception ──

    @Test
    @DisplayName("Product Recall: Active in-flight groups containing recalled batch are immediately halted and OTP invalidated")
    void testRecallHaltsInFlightFulfilmentGroup() {
        VendorProduct vp = VendorProduct.builder().name("Fresh Paneer").vendor(vendor).build();
        vp.setId(701L);

        FoodProductionBatch batch = FoodProductionBatch.builder()
                .batchNumber("BATCH-PAN-99")
                .batchStatus(BatchStatus.RELEASED)
                .build();
        batch.setId(88L);

        CommerceOrder order = CommerceOrder.builder()
                .orderNumber("ORD-REC-99")
                .buyer(customer)
                .community(community)
                .status(CommerceOrderStatus.CONFIRMED)
                .build();
        order.setId(301L);

        FreshFulfilmentGroup inFlightGroup = FreshFulfilmentGroup.builder()
                .groupReference("GRP-ORD-REC-99-1")
                .order(order)
                .status(FulfilmentGroupStatus.DISPATCHED)
                .handoverOtp("654321")
                .qrCodeToken("QR-TOKEN-ACTIVE")
                .build();
        inFlightGroup.setId(401L);

        FreshFulfilmentItem item = FreshFulfilmentItem.builder()
                .fulfilmentGroup(inFlightGroup)
                .vendorProduct(vp)
                .batch(batch)
                .requestedQuantity(new BigDecimal("2.000"))
                .billedPrice(new BigDecimal("240.00"))
                .build();
        item.setId(501L);

        AppUser admin = AppUser.builder().fullName("Admin User").email("admin@mana.in").build();
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        when(vendorProductRepository.findById(701L)).thenReturn(Optional.of(vp));
        when(batchRepository.findById(88L)).thenReturn(Optional.of(batch));
        when(fulfilmentItemRepository.findByBatchId(88L)).thenReturn(List.of(item));

        when(recallRepository.save(any(FreshProductRecall.class))).thenAnswer(i -> {
            FreshProductRecall r = i.getArgument(0);
            r.setId(10L);
            return r;
        });

        InitiateRecallRequest req = InitiateRecallRequest.builder()
                .vendorProductId(701L)
                .batchId(88L)
                .severity(RecallSeverity.CRITICAL)
                .reason("Foreign matter detected during random sensory audit")
                .initiatedByUserId(1L)
                .build();

        ProductRecallDto dto = recallService.initiateRecall(req);

        assertThat(dto).isNotNull();
        // In-Flight Group must be CANCELLED and delivery OTP cleared to prevent doorstep handover
        assertThat(inFlightGroup.getStatus()).isEqualTo(FulfilmentGroupStatus.CANCELLED);
        assertThat(inFlightGroup.getHandoverOtp()).isNull();
        assertThat(inFlightGroup.getQrCodeToken()).isNull();
    }

    // ── 3. Catch-Weight Overweight Debit Failure & Surcharge Recovery ──

    @Test
    @DisplayName("Catch-Weight: If wallet debit fails due to zero/insufficient balance, record pending surcharge")
    void testInsufficientBalanceRecordsPendingSurcharge() {
        CommerceOrder order = CommerceOrder.builder().orderNumber("ORD-W-01").buyer(customer).community(community).build();
        order.setId(111L);

        FreshFulfilmentGroup group = FreshFulfilmentGroup.builder().order(order).status(FulfilmentGroupStatus.PICKED).build();
        group.setId(222L);

        VendorProduct vp = VendorProduct.builder().name("Fresh Rohu Fish").build();
        vp.setId(333L);

        FreshFulfilmentItem item = FreshFulfilmentItem.builder()
                .fulfilmentGroup(group)
                .vendorProduct(vp)
                .unitOfMeasure("KG")
                .requestedQuantity(new BigDecimal("1.000"))
                .billedPrice(new BigDecimal("500.00"))
                .build();
        item.setId(444L);

        when(fulfilmentItemRepository.findById(444L)).thenReturn(Optional.of(item));
        when(weightLogRepository.save(any(FreshWeightAdjustmentLog.class))).thenAnswer(i -> {
            FreshWeightAdjustmentLog l = i.getArgument(0);
            l.setId(999L);
            return l;
        });

        // Simulate customer wallet debit throwing InsufficientFundsException
        doThrow(new RuntimeException("Insufficient wallet funds: balance is ₹0.00"))
                .when(walletService).debit(any(AppUser.class), any(BigDecimal.class), anyString(), anyString());

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(444L)
                .actualWeightG(new BigDecimal("1120.000")) // +12% overweight (within 15% tolerance)
                .build();

        WeightAdjustmentDto result = weightAdjustmentService.recordActualWeight(req);

        assertThat(result).isNotNull();
        assertThat(result.getAdjustmentStatus()).isEqualTo(WeightAdjustmentStatus.CHARGED_TO_WALLET);
        // Margin protection: Pending surcharge is tracked instead of silent leakage
        assertThat(result.getIsSurchargePending()).isTrue();
        assertThat(result.getPendingSurchargeAmount()).isEqualByComparingTo("60.00");
    }

    // ── 4. Atomic Capacity Reservation ──

    @Test
    @DisplayName("Vendor Capacity: Atomic reservation executes successfully without race conditions")
    void testAtomicCapacityReservationPreventsOverSell() {
        LocalDate date = LocalDate.of(2026, 11, 6);
        VendorProductionCapacity cap = VendorProductionCapacity.builder()
                .vendorId("VND-01")
                .productId("PRD-IDLI-01")
                .productionDate(date)
                .maxCapacity(100)
                .reservedQuantity(30)
                .availableQuantity(70)
                .isSoldOut(false)
                .build();
        cap.setId(55L);

        when(capacityRepository.findByVendorIdAndProductIdAndProductionDate("VND-01", "PRD-IDLI-01", date))
                .thenReturn(Optional.of(cap));
        when(capacityRepository.reserveCapacityAtomic(55L, 5)).thenReturn(1);

        boolean reserved = capacityService.checkAndReserveCapacity("VND-01", "PRD-IDLI-01", date, 5);

        assertThat(reserved).isTrue();
        verify(capacityRepository).reserveCapacityAtomic(55L, 5);
    }

    // ── 5. Distributed Timezone Safeguards ──

    @Test
    @DisplayName("Scheduler Timezone: All cron methods in DailyBasketScheduler must specify zone = Asia/Kolkata")
    void testSchedulerTimezoneIsAsiaKolkata() {
        for (Method method : DailyBasketScheduler.class.getDeclaredMethods()) {
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            if (scheduled != null && !scheduled.cron().isEmpty()) {
                assertThat(scheduled.zone())
                        .as("Method %s must explicitly declare Asia/Kolkata timezone", method.getName())
                        .isEqualTo("Asia/Kolkata");
            }
        }
    }

    // ── 6. Security & Traceability Hardening (OTP Masking & IDOR Guards) ──

    @Test
    @DisplayName("Fulfilment Security: Delivery runner cannot see handover OTP or QR token")
    void testDeliveryRunnerCannotSeeHandoverOtp() {
        FreshFulfilmentGroup group = FreshFulfilmentGroup.builder()
                .groupReference("FG-TEST-001")
                .order(CommerceOrder.builder().orderNumber("ORD-101").buyer(customer).community(community).build())
                .handoverOtp("482910")
                .qrCodeToken("QR-TEST-TOKEN")
                .status(FulfilmentGroupStatus.DISPATCHED)
                .build();
        group.setId(101L);

        when(groupRepository.findById(101L)).thenReturn(Optional.of(group));

        UserPrincipal runnerPrincipal = new UserPrincipal(
                999L, "runner@mana.test", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_DELIVERY_PARTNER")),
                null
        );

        FulfilmentGroupDto result = fulfilmentGroupService.getFulfilmentGroupById(101L, runnerPrincipal);

        assertThat(result).isNotNull();
        assertThat(result.getGroupReference()).isEqualTo("FG-TEST-001");
        assertThat(result.getHandoverOtp()).isNull();
        assertThat(result.getQrCodeToken()).isNull();
        assertThat(result.getCommunityId()).isEqualTo(community.getId());
    }

    @Test
    @DisplayName("Fulfilment Security: Buyer resident can view plaintext handover OTP and QR token")
    void testBuyerCanSeeHandoverOtp() {
        FreshFulfilmentGroup group = FreshFulfilmentGroup.builder()
                .groupReference("FG-TEST-002")
                .order(CommerceOrder.builder().orderNumber("ORD-102").buyer(customer).community(community).build())
                .handoverOtp("654321")
                .qrCodeToken("QR-TEST-BUYER")
                .status(FulfilmentGroupStatus.DISPATCHED)
                .build();
        group.setId(102L);

        when(groupRepository.findById(102L)).thenReturn(Optional.of(group));

        UserPrincipal buyerPrincipal = new UserPrincipal(
                customer.getId(), customer.getEmail(), "pass",
                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")),
                customer
        );

        FulfilmentGroupDto result = fulfilmentGroupService.getFulfilmentGroupById(102L, buyerPrincipal);

        assertThat(result).isNotNull();
        assertThat(result.getHandoverOtp()).isEqualTo("654321");
        assertThat(result.getQrCodeToken()).isEqualTo("QR-TEST-BUYER");
    }

    @Test
    @DisplayName("Fulfilment IDOR Guard: Resident cannot access another resident's order groups")
    void testResidentCannotAccessOtherResidentsOrderGroups() {
        AppUser otherCustomer = new AppUser();
        otherCustomer.setId(888L);
        otherCustomer.setEmail("other@mana.test");

        CommerceOrder order = CommerceOrder.builder()
                .orderNumber("ORD-999")
                .buyer(otherCustomer)
                .community(community)
                .build();
        order.setId(505L);

        when(orderRepository.findById(505L)).thenReturn(Optional.of(order));

        UserPrincipal residentPrincipal = new UserPrincipal(
                customer.getId(), customer.getEmail(), "pass",
                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")),
                customer
        );

        assertThatThrownBy(() -> fulfilmentGroupService.getFulfilmentGroupsByOrder(505L, residentPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to order 505");
    }
}
