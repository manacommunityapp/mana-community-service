package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.service.CommerceWalletService;
import com.manacommunity.api.fresh.dispatch.dto.CreateSettlementRequest;
import com.manacommunity.api.fresh.dispatch.dto.VendorSettlementDto;
import com.manacommunity.api.fresh.dispatch.model.FreshVendorSettlement;
import com.manacommunity.api.fresh.dispatch.model.SettlementStatus;
import com.manacommunity.api.fresh.dispatch.repository.FreshVendorSettlementRepository;
import com.manacommunity.api.fresh.dispatch.service.FreshDispatchSettlementService;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentGroup;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.fresh.fulfilment.model.FulfilmentGroupStatus;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.processing.dto.LogTemperatureRequest;
import com.manacommunity.api.fresh.processing.dto.TemperatureLogDto;
import com.manacommunity.api.fresh.processing.model.BatchStatus;
import com.manacommunity.api.fresh.processing.model.FreshBatchTemperatureLog;
import com.manacommunity.api.fresh.processing.model.FreshProcessingBatch;
import com.manacommunity.api.fresh.processing.repository.FreshBatchTemperatureLogRepository;
import com.manacommunity.api.fresh.processing.repository.FreshProcessingBatchRepository;
import com.manacommunity.api.fresh.processing.service.FreshProcessingService;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.model.FreshProductRecall;
import com.manacommunity.api.fresh.recall.model.FreshProductRecallOrder;
import com.manacommunity.api.fresh.recall.model.RecallSeverity;
import com.manacommunity.api.fresh.recall.model.RecallStatus;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallOrderRepository;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallRepository;
import com.manacommunity.api.fresh.recall.service.FreshRecallService;
import com.manacommunity.api.fresh.security.FreshSecurityEvaluator;
import com.manacommunity.api.fresh.vendor.dto.VendorDocumentDto;
import com.manacommunity.api.fresh.vendor.model.DocumentVerificationStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorDocument;
import com.manacommunity.api.fresh.vendor.model.VendorDocumentType;
import com.manacommunity.api.fresh.vendor.model.VendorStaffRole;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorDocumentRepository;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorStaffRepository;
import com.manacommunity.api.fresh.vendor.service.FreshVendorDocumentService;
import com.manacommunity.api.fresh.weight.dto.RecordWeightRequest;
import com.manacommunity.api.fresh.weight.dto.WeightAdjustmentDto;
import com.manacommunity.api.fresh.weight.model.FreshWeightAdjustmentLog;
import com.manacommunity.api.fresh.weight.model.WeightAdjustmentStatus;
import com.manacommunity.api.fresh.weight.repository.FreshWeightAdjustmentLogRepository;
import com.manacommunity.api.fresh.weight.service.FreshWeightAdjustmentService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshFinancialAndAuditHardeningTest {

    // Weight Service Mocks
    @Mock private FreshFulfilmentItemRepository itemRepository;
    @Mock private FreshWeightAdjustmentLogRepository weightLogRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private CommerceWalletService walletService;
    @Mock private AuditService auditService;
    @InjectMocks private FreshWeightAdjustmentService weightAdjustmentService;

    // Recall Service Mocks
    @Mock private FreshProductRecallRepository recallRepository;
    @Mock private FreshProductRecallOrderRepository recallOrderRepository;
    @InjectMocks private FreshRecallService recallService;

    // Dispatch Settlement Service Mocks
    @Mock private FreshVendorSettlementRepository settlementRepository;
    @Mock private VmsVendorRepository vendorRepository;
    @InjectMocks private FreshDispatchSettlementService settlementService;

    // Processing Service Mocks
    @Mock private FreshProcessingBatchRepository batchRepository;
    @Mock private FreshBatchTemperatureLogRepository temperatureLogRepository;
    @InjectMocks private FreshProcessingService processingService;

    // Vendor Staff & Security Evaluator Mocks
    @Mock private FreshVendorStaffRepository staffRepository;
    private FreshSecurityEvaluator securityEvaluator;

    // Vendor Document Service Mocks
    @Mock private FreshVendorDocumentRepository documentRepository;
    @InjectMocks private FreshVendorDocumentService documentService;

    private AppUser customer;
    private CommerceOrder order;
    private FreshFulfilmentGroup group;
    private FreshFulfilmentItem item;

    @BeforeEach
    void setUp() {
        securityEvaluator = new FreshSecurityEvaluator(staffRepository);

        Community community = Community.builder().name("Green Valley").build();
        community.setId(1L);

        customer = AppUser.builder().fullName("Jane Doe").email("jane@mana.in").community(community).build();
        customer.setId(101L);

        order = CommerceOrder.builder().orderNumber("ORD-FRESH-101").buyer(customer).community(community).build();
        order.setId(201L);

        group = FreshFulfilmentGroup.builder().order(order).status(FulfilmentGroupStatus.PICKED).build();
        group.setId(301L);

        VendorProduct vp = VendorProduct.builder().name("Fresh Rohu Fish (Whole)").build();
        vp.setId(701L);

        item = FreshFulfilmentItem.builder()
                .fulfilmentGroup(group)
                .vendorProduct(vp)
                .unitOfMeasure("KG")
                .requestedQuantity(new BigDecimal("1.000"))
                .billedPrice(new BigDecimal("500.00"))
                .build();
        item.setId(401L);
    }

    // ── 1. Weight Adjustment Real Wallet Execution ──

    @Test
    @DisplayName("Underweight item executes wallet refund and logs audit trail")
    void testUnderweightExecutesWalletRefundAndAudit() {
        when(itemRepository.findById(401L)).thenReturn(Optional.of(item));
        when(weightLogRepository.save(any(FreshWeightAdjustmentLog.class))).thenAnswer(i -> {
            FreshWeightAdjustmentLog l = i.getArgument(0);
            l.setId(99L);
            return l;
        });

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(401L)
                .actualWeightG(new BigDecimal("900.000")) // 10% under-weight
                .build();

        WeightAdjustmentDto result = weightAdjustmentService.recordActualWeight(req);

        assertThat(result).isNotNull();
        assertThat(result.getAdjustmentStatus()).isEqualTo(WeightAdjustmentStatus.REFUNDED_TO_WALLET);
        assertThat(result.getRefundOrChargeAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getWalletTransactionRef()).isNotNull().startsWith("TXN-WADJ-");

        // Verify walletService was called to credit refund
        verify(walletService).refund(eq(customer), eq(new BigDecimal("50.00")), anyString());
        // Verify AuditService logged durable mutation
        verify(auditService).record(eq(AuditAction.FRESH_WEIGHT_ADJUSTED), eq(AuditModule.FRESH_COMMERCE),
                eq("FreshWeightAdjustmentLog"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Overweight item within tolerance debits wallet balance")
    void testOverweightWithinToleranceDebitsWallet() {
        when(itemRepository.findById(401L)).thenReturn(Optional.of(item));
        when(weightLogRepository.save(any(FreshWeightAdjustmentLog.class))).thenAnswer(i -> {
            FreshWeightAdjustmentLog l = i.getArgument(0);
            l.setId(100L);
            return l;
        });

        RecordWeightRequest req = RecordWeightRequest.builder()
                .fulfilmentItemId(401L)
                .actualWeightG(new BigDecimal("1080.000")) // 8% over-weight (within 15% tolerance)
                .build();

        WeightAdjustmentDto result = weightAdjustmentService.recordActualWeight(req);

        assertThat(result.getAdjustmentStatus()).isEqualTo(WeightAdjustmentStatus.CHARGED_TO_WALLET);
        assertThat(result.getRefundOrChargeAmount()).isEqualByComparingTo("40.00");
        assertThat(result.getWalletTransactionRef()).isNotNull().startsWith("TXN-WADJ-");

        verify(walletService).debit(eq(customer), eq(new BigDecimal("40.00")), anyString(), contains("Weight variation"));
        verify(auditService).record(eq(AuditAction.FRESH_WEIGHT_ADJUSTED), eq(AuditModule.FRESH_COMMERCE),
                eq("FreshWeightAdjustmentLog"), anyString(), anyString(), anyString());
    }

    // ── 2. Product Recall Real Wallet Refunds ──

    @Test
    @DisplayName("Product recall execution credits customer wallets and updates audit log")
    void testProcessRecallRefundsExecutesWalletCredit() {
        VendorProduct vp = VendorProduct.builder().name("Fresh Batters").build();
        vp.setId(15L);

        FreshProductRecall recall = FreshProductRecall.builder()
                .recallReference("REC-20261105-001")
                .vendorProduct(vp)
                .severity(RecallSeverity.CRITICAL)
                .status(RecallStatus.INITIATED)
                .build();
        recall.setId(10L);

        FreshProductRecallOrder ro = FreshProductRecallOrder.builder()
                .recall(recall)
                .order(order)
                .customer(customer)
                .refundAmount(new BigDecimal("120.00"))
                .refundProcessed(false)
                .build();
        ro.setId(501L);

        when(recallRepository.findById(10L)).thenReturn(Optional.of(recall));
        when(recallOrderRepository.findByRecallId(10L)).thenReturn(List.of(ro));

        ProductRecallDto dto = recallService.processRecallRefunds(10L);

        assertThat(dto.getStatus()).isEqualTo(RecallStatus.COMPLETED);
        assertThat(ro.getRefundProcessed()).isTrue();
        assertThat(ro.getWalletTransactionRef()).isNotNull().startsWith("TXN-RECALL-");

        verify(walletService).refund(eq(customer), eq(new BigDecimal("120.00")), contains("RECALL-REC-20261105-001-501"));
        verify(auditService).record(eq(AuditAction.FRESH_RECALL_REFUNDED), eq(AuditModule.FRESH_COMMERCE),
                eq("FreshProductRecall"), eq("REC-20261105-001"), eq("INITIATED"), eq("COMPLETED"));
    }

    // ── 3. Authoritative Settlements & Statutory Deductions ──

    @Test
    @DisplayName("Settlement accurately computes TDS (194-O 1%), GST (18% on commission), and net payable")
    void testSettlementComputesTdsAndGst() {
        VmsVendor vendor = VmsVendor.builder().businessName("Deccan Fresh Agro").build();
        vendor.setId(55L);
        when(vendorRepository.findById(55L)).thenReturn(Optional.of(vendor));

        when(settlementRepository.save(any(FreshVendorSettlement.class))).thenAnswer(i -> {
            FreshVendorSettlement s = i.getArgument(0);
            s.setId(88L);
            return s;
        });

        CreateSettlementRequest req = CreateSettlementRequest.builder()
                .vendorId(55L)
                .settlementPeriodStart(LocalDate.of(2026, 11, 1))
                .settlementPeriodEnd(LocalDate.of(2026, 11, 7))
                .grossSalesAmount(new BigDecimal("100000.00"))
                .manaCommissionAmount(new BigDecimal("10000.00")) // 10%
                .deliveryFeesCollected(new BigDecimal("2000.00"))
                .gatewayFeesDeducted(new BigDecimal("1500.00"))
                .refundClawbacksAmount(new BigDecimal("500.00"))
                .build();

        VendorSettlementDto s = settlementService.createSettlement(req);

        // TDS 1% of 100,000 = 1,000.00
        assertThat(s.getTdsDeductedAmount()).isEqualByComparingTo("1000.00");
        // GST 18% of 10,000 commission = 1,800.00
        assertThat(s.getGstOnCommissionAmount()).isEqualByComparingTo("1800.00");
        assertThat(s.getRefundClawbacksAmount()).isEqualByComparingTo("500.00");

        // Net Payable: 100,000 - 10,000 (comm) - 1,800 (gst) - 1,000 (tds) - 500 (clawback) + 2,000 (delivery) - 1,500 (gateway)
        // = 87,200.00
        assertThat(s.getNetPayableAmount()).isEqualByComparingTo("87200.00");
    }

    // ── 4. Cold-Chain Breach Auto-Quarantine ──

    @Test
    @DisplayName("Temperature exceeding 4.0°C automatically triggers batch QUARANTINED status and audit")
    void testTemperatureBreachQuarantinesBatch() {
        FreshProcessingBatch batch = FreshProcessingBatch.builder()
                .batchNumber("BAT-CHIX-20261105-01")
                .status(BatchStatus.IN_PRODUCTION)
                .storageTempCelsius(new BigDecimal("2.5"))
                .build();
        batch.setId(77L);

        when(batchRepository.findById(77L)).thenReturn(Optional.of(batch));

        LogTemperatureRequest req = LogTemperatureRequest.builder()
                .loggedTempCelsius(new BigDecimal("5.2")) // Breaches 4.0°C chilled limit
                .locationTag("Cold Room A")
                .build();

        TemperatureLogDto result = processingService.logTemperature(77L, req);

        assertThat(result).isNotNull();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.QUARANTINED);

        verify(auditService).record(eq(AuditAction.FRESH_BATCH_QUARANTINED), eq(AuditModule.FRESH_COMMERCE),
                eq("FreshProcessingBatch"), eq("BAT-CHIX-20261105-01"), eq("IN_PRODUCTION"), eq("QUARANTINED"));
    }

    // ── 5. Multi-Tenant RBAC & IDOR Security Evaluator ──

    @Test
    @DisplayName("Security evaluator authorizes active staff of matching vendor")
    void testSecurityEvaluatorAuthorizesVendorStaff() {
        UserPrincipal principal = new UserPrincipal(5L, "staff@deccan.in", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR")), null);

        when(staffRepository.existsByVendorIdAndUserIdAndIsActiveTrue(10L, 5L)).thenReturn(true);

        boolean allowed = securityEvaluator.isVendorStaff(principal, 10L);
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Security evaluator rejects cross-vendor access attempt (IDOR prevention)")
    void testSecurityEvaluatorBlocksCrossVendorAccess() {
        UserPrincipal principal = new UserPrincipal(5L, "staff@deccan.in", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR")), null);

        when(staffRepository.existsByVendorIdAndUserIdAndIsActiveTrue(20L, 5L)).thenReturn(false);

        boolean allowed = securityEvaluator.isVendorStaff(principal, 20L);
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Security evaluator permits admin bypass across all vendors")
    void testSecurityEvaluatorAllowsAdminBypass() {
        UserPrincipal admin = new UserPrincipal(1L, "admin@mana.in", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), null);

        boolean allowed = securityEvaluator.isVendorStaff(admin, 999L);
        assertThat(allowed).isTrue();
        verifyNoInteractions(staffRepository);
    }

    @Test
    @DisplayName("Security evaluator validates granular vendor role correctly")
    void testSecurityEvaluatorValidatesGranularRole() {
        UserPrincipal principal = new UserPrincipal(8L, "owner@deccan.in", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR")), null);

        when(staffRepository.existsByVendorIdAndUserIdAndVendorRoleAndIsActiveTrue(10L, 8L, VendorStaffRole.VENDOR_OWNER))
                .thenReturn(true);

        boolean isOwner = securityEvaluator.hasVendorRole(principal, 10L, "VENDOR_OWNER");
        assertThat(isOwner).isTrue();

        boolean isAccountant = securityEvaluator.hasVendorRole(principal, 10L, "VENDOR_ACCOUNTANT");
        assertThat(isAccountant).isFalse();
    }

    // ── 6. Document Verification Audit Trail ──

    @Test
    @DisplayName("Document verification decision emits durable audit log")
    void testDocumentVerificationEmitsAudit() {
        VmsVendor vendor = VmsVendor.builder().businessName("Ocean Fresh Catch").build();
        vendor.setId(30L);

        FreshVendorDocument doc = FreshVendorDocument.builder()
                .vendor(vendor)
                .documentType(VendorDocumentType.FSSAI_LICENSE)
                .documentNumber("FSSAI-12345678901234")
                .status(DocumentVerificationStatus.PENDING_VERIFICATION)
                .build();
        doc.setId(888L);

        when(documentRepository.findById(888L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(FreshVendorDocument.class))).thenAnswer(i -> i.getArgument(0));

        VendorDocumentDto verified = documentService.verifyDocument(888L, true, null, 1L);

        assertThat(verified.getStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        verify(auditService).record(eq(AuditAction.FRESH_DOCUMENT_VERIFIED), eq(AuditModule.FRESH_COMMERCE),
                eq("FreshVendorDocument"), eq("888"), eq("PENDING_VERIFICATION"), eq("VERIFIED"));
    }
}
