package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderItemRepository;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.exception.BatchQuarantinedException;
import com.manacommunity.api.exception.HandoverLockoutException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.InvalidStateTransitionException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.ToleranceExceededException;
import com.manacommunity.api.fresh.catalog.repository.FreshProductCatalogProfileRepository;
import com.manacommunity.api.fresh.fulfilment.dto.FulfilmentGroupDto;
import com.manacommunity.api.fresh.fulfilment.dto.UpdateFulfilmentGroupStatusRequest;
import com.manacommunity.api.fresh.fulfilment.dto.VerifyHandoverRequest;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentGroup;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.fresh.fulfilment.model.FulfilmentGroupStatus;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentGroupRepository;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.fulfilment.service.FreshFulfilmentGroupService;
import com.manacommunity.api.fresh.recall.dto.InitiateRecallRequest;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.model.FreshProductRecall;
import com.manacommunity.api.fresh.recall.model.RecallSeverity;
import com.manacommunity.api.fresh.recall.model.RecallStatus;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallOrderRepository;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallRepository;
import com.manacommunity.api.fresh.recall.service.FreshRecallService;
import com.manacommunity.api.fresh.weight.dto.RecordWeightRequest;
import com.manacommunity.api.fresh.weight.repository.FreshWeightAdjustmentLogRepository;
import com.manacommunity.api.fresh.weight.service.FreshWeightAdjustmentService;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.commerce.core.service.CommerceWalletService;
import com.manacommunity.api.security.AuditService;
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
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshSecurityAndExceptionTest {

    @Mock
    private FreshFulfilmentGroupRepository groupRepository;
    @Mock
    private FreshFulfilmentItemRepository itemRepository;
    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private CommerceOrderItemRepository orderItemRepository;
    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private FreshProductCatalogProfileRepository catalogProfileRepository;
    @Mock
    private FreshProductRecallRepository recallRepository;
    @Mock
    private FreshProductRecallOrderRepository recallOrderRepository;
    @Mock
    private FoodProductionBatchRepository batchRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private FreshWeightAdjustmentLogRepository weightLogRepository;
    @Mock
    private CommerceWalletService walletService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private FreshFulfilmentGroupService fulfilmentGroupService;

    private FreshRecallService recallService;
    private FreshWeightAdjustmentService weightService;

    private CommerceOrder mockOrder;
    private FreshFulfilmentGroup mockGroup;

    @BeforeEach
    void setUp() {
        recallService = new FreshRecallService(
                recallRepository,
                recallOrderRepository,
                itemRepository,
                vendorProductRepository,
                batchRepository,
                userRepository,
                walletService,
                auditService
        );

        weightService = new FreshWeightAdjustmentService(
                itemRepository,
                weightLogRepository,
                userRepository,
                walletService,
                auditService
        );

        AppUser buyer = AppUser.builder().id(101L).fullName("Sandeep Kumar").build();
        Community comm = Community.builder().id(1L).name("My Home Jewel").build();

        mockOrder = CommerceOrder.builder()
                .id(500L)
                .orderNumber("ORD-SEC-001")
                .buyer(buyer)
                .community(comm)
                .channel(CommerceChannel.MARKETPLACE)
                .status(CommerceOrderStatus.CONFIRMED)
                .build();

        mockGroup = FreshFulfilmentGroup.builder()
                .id(1L)
                .groupReference("GRP-ORD-SEC-001-1")
                .order(mockOrder)
                .status(FulfilmentGroupStatus.DISPATCHED)
                .handoverOtp("654321")
                .qrCodeToken("FFG-SEC-QR")
                .failedOtpAttempts(0)
                .isHandoverLocked(false)
                .items(new ArrayList<>())
                .build();
    }

    // =========================================================================
    // 1. Handover Brute-Force & Lockout Tests
    // =========================================================================

    @Test
    @DisplayName("Handover verification locks out group after 5 consecutive failed attempts")
    void testHandoverLockoutAfter5FailedAttempts() {
        mockGroup.setFailedOtpAttempts(4);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(mockGroup));
        when(groupRepository.save(any(FreshFulfilmentGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyHandoverRequest req = VerifyHandoverRequest.builder().otp("000000").build();

        assertThatThrownBy(() -> fulfilmentGroupService.verifyHandover(1L, req))
                .isInstanceOf(HandoverLockoutException.class)
                .hasMessageContaining("Handover verification locked out for group GRP-ORD-SEC-001-1 after 5 failed attempts");

        assertThat(mockGroup.getIsHandoverLocked()).isTrue();
        assertThat(mockGroup.getFailedOtpAttempts()).isEqualTo(5);
        verify(groupRepository).save(mockGroup);
    }

    @Test
    @DisplayName("Handover verification immediately rejects attempts when group is already locked")
    void testHandoverRejectionWhenAlreadyLocked() {
        mockGroup.setIsHandoverLocked(true);
        mockGroup.setFailedOtpAttempts(5);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(mockGroup));

        VerifyHandoverRequest req = VerifyHandoverRequest.builder().otp("654321").build(); // Even with correct OTP

        assertThatThrownBy(() -> fulfilmentGroupService.verifyHandover(1L, req))
                .isInstanceOf(HandoverLockoutException.class)
                .hasMessageContaining("Supervisor override required");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Handover verification returns idempotently if group is already DELIVERED")
    void testHandoverIdempotentWhenDelivered() {
        mockGroup.setStatus(FulfilmentGroupStatus.DELIVERED);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(mockGroup));

        VerifyHandoverRequest req = VerifyHandoverRequest.builder().otp("654321").build();
        FulfilmentGroupDto dto = fulfilmentGroupService.verifyHandover(1L, req);

        assertThat(dto.getStatus()).isEqualTo(FulfilmentGroupStatus.DELIVERED);
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Handover verification rejects handover if group is CANCELLED")
    void testHandoverRejectionWhenCancelled() {
        mockGroup.setStatus(FulfilmentGroupStatus.CANCELLED);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(mockGroup));

        VerifyHandoverRequest req = VerifyHandoverRequest.builder().otp("654321").build();

        assertThatThrownBy(() -> fulfilmentGroupService.verifyHandover(1L, req))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition FreshFulfilmentGroup from CANCELLED to DELIVERED");
    }

    @Test
    @DisplayName("Status update rejects transition from CANCELLED to any other status")
    void testUpdateStatusRejectsTransitionFromCancelled() {
        mockGroup.setStatus(FulfilmentGroupStatus.CANCELLED);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(mockGroup));

        UpdateFulfilmentGroupStatusRequest req = UpdateFulfilmentGroupStatusRequest.builder()
                .status(FulfilmentGroupStatus.DISPATCHED)
                .build();

        assertThatThrownBy(() -> fulfilmentGroupService.updateStatus(1L, req))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition FreshFulfilmentGroup from CANCELLED to DISPATCHED");
    }

    @Test
    @DisplayName("Fulfilment group lookup throws ResourceNotFoundException (404) when missing")
    void testGetGroupByIdThrowsResourceNotFound() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fulfilmentGroupService.getFulfilmentGroupById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FreshFulfilmentGroup not found with id: 999");
    }

    // =========================================================================
    // 2. Recall Concurrency & Idempotency Tests
    // =========================================================================

    @Test
    @DisplayName("Recall refund processing is idempotent and skips already-completed recall")
    void testProcessRecallRefundsIdempotency() {
        VendorProduct testProduct = VendorProduct.builder().id(300L).name("Organic Paneer 200g").build();
        FreshProductRecall recall = FreshProductRecall.builder()
                .id(77L)
                .recallReference("RCL-202611-TEST")
                .vendorProduct(testProduct)
                .status(RecallStatus.COMPLETED)
                .build();

        when(recallRepository.findById(77L)).thenReturn(Optional.of(recall));

        ProductRecallDto dto = recallService.processRecallRefunds(77L);

        assertThat(dto.getStatus()).isEqualTo(RecallStatus.COMPLETED);
        verify(recallOrderRepository, never()).saveAll(any());
        verify(recallRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recall initiation throws ResourceNotFoundException (404) when product is missing")
    void testInitiateRecallThrowsResourceNotFound() {
        when(vendorProductRepository.findById(404L)).thenReturn(Optional.empty());

        InitiateRecallRequest req = InitiateRecallRequest.builder()
                .vendorProductId(404L)
                .severity(RecallSeverity.MAJOR)
                .reason("Test")
                .initiatedByUserId(1L)
                .build();

        assertThatThrownBy(() -> recallService.initiateRecall(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("VendorProduct not found with id: 404");
    }

    // =========================================================================
    // 3. Weight Adjustment State Validation Tests
    // =========================================================================

    @Test
    @DisplayName("Weight adjustment rejects adjustment if group is already DELIVERED")
    void testWeightAdjustmentRejectsDeliveredGroup() {
        mockGroup.setStatus(FulfilmentGroupStatus.DELIVERED);
        FreshFulfilmentItem item = FreshFulfilmentItem.builder()
                .id(22L)
                .fulfilmentGroup(mockGroup)
                .requestedQuantity(new BigDecimal("1.000"))
                .unitOfMeasure("KG")
                .billedPrice(new BigDecimal("100.00"))
                .build();

        when(itemRepository.findById(22L)).thenReturn(Optional.of(item));

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(22L)
                .actualWeightG(new BigDecimal("950.000"))
                .build();

        assertThatThrownBy(() -> weightService.recordActualWeight(req))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition FreshFulfilmentItem from DELIVERED to WEIGHT_ADJUSTED");
    }

    @Test
    @DisplayName("Weight adjustment throws ResourceNotFoundException (404) when item is missing")
    void testWeightAdjustmentThrowsResourceNotFound() {
        when(itemRepository.findById(888L)).thenReturn(Optional.empty());

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(888L)
                .actualWeightG(new BigDecimal("950.000"))
                .build();

        assertThatThrownBy(() -> weightService.recordActualWeight(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FreshFulfilmentItem not found with id: 888");
    }

    // =========================================================================
    // 4. Custom Exception Diagnostic Detail Tests
    // =========================================================================

    @Test
    @DisplayName("ToleranceExceededException carries structured weight variation fields")
    void testToleranceExceededExceptionFields() {
        ToleranceExceededException ex = new ToleranceExceededException(
                "Weight variation exceeds 15%",
                new BigDecimal("1000.000"),
                new BigDecimal("1250.000"),
                new BigDecimal("25.00"),
                new BigDecimal("15.00")
        );

        assertThat(ex.getErrorCode()).isEqualTo("TOLERANCE_EXCEEDED");
        assertThat(ex.getStatus().value()).isEqualTo(422);
        assertThat(ex.getRequestedWeightG()).isEqualByComparingTo(new BigDecimal("1000.000"));
        assertThat(ex.getActualWeightG()).isEqualByComparingTo(new BigDecimal("1250.000"));
        assertThat(ex.getDiffPct()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(ex.getMaxTolerancePct()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    @DisplayName("BatchQuarantinedException carries structured batch quarantine detail")
    void testBatchQuarantinedExceptionFields() {
        BatchQuarantinedException ex = new BatchQuarantinedException(
                55L,
                "BATCH-Q-001",
                "Salmonella test positive"
        );

        assertThat(ex.getErrorCode()).isEqualTo("BATCH_QUARANTINED");
        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getBatchId()).isEqualTo(55L);
        assertThat(ex.getBatchNumber()).isEqualTo("BATCH-Q-001");
        assertThat(ex.getReason()).isEqualTo("Salmonella test positive");
    }
}
