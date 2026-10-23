package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.entity.TransactionEscrow;
import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.entity.TransactionSettlement;
import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.RefundDestination;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import com.manacommunity.api.transactioncore.repository.TransactionEscrowRepository;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.repository.TransactionSettlementRepository;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManaTransactionCoreServiceTest {

    @Mock
    private TransactionIntentRepository intentRepository;
    @Mock
    private TransactionEscrowRepository escrowRepository;
    @Mock
    private TransactionSettlementRepository settlementRepository;
    @Mock
    private WalletEngine walletEngine;
    @Mock
    private AccountingEngine accountingEngine;
    @Mock
    private DocumentSequenceService documentSequenceService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ManaTransactionCoreService transactionCoreService;

    private PaymentIntentRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = PaymentIntentRequest.builder()
                .idempotencyKey("TEST-IDEM-001")
                .domain(TransactionDomain.GROUP_BUYING)
                .orderReferenceId("ORD-12345")
                .communityId(1L)
                .payerId(10L)
                .payerName("Sandeep Roy")
                .payerEmail("sandeep@example.com")
                .amount(new BigDecimal("1500.00"))
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_DELIVERY_VERIFICATION)
                .splits(List.of(
                        PaymentSplit.builder()
                                .recipientType("VENDOR")
                                .recipientId(100L)
                                .amount(new BigDecimal("1500.00"))
                                .commissionAmount(new BigDecimal("75.00"))
                                .build()
                ))
                .build();
    }

    @Test
    void testCreatePaymentIntent_New_Success() {
        when(intentRepository.findByIdempotencyKey("TEST-IDEM-001")).thenReturn(Optional.empty());
        when(intentRepository.save(any(TransactionIntent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentResponse response = transactionCoreService.createPaymentIntent(testRequest);

        assertNotNull(response);
        assertNotNull(response.getIntentId());
        assertEquals("TEST-IDEM-001", response.getIdempotencyKey());
        assertEquals(TransactionDomain.GROUP_BUYING, response.getDomain());
        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertTrue(response.isEscrowHeld());

        verify(intentRepository, times(1)).save(any(TransactionIntent.class));
        verify(escrowRepository, times(1)).save(any(TransactionEscrow.class));
    }

    @Test
    void testCreatePaymentIntent_Idempotent_ReturnsExisting() {
        TransactionIntent existing = TransactionIntent.builder()
                .intentId("TXN-EXISTING-01")
                .idempotencyKey("TEST-IDEM-001")
                .domain(TransactionDomain.GROUP_BUYING)
                .orderReferenceId("ORD-12345")
                .amount(new BigDecimal("1500.00"))
                .status(TransactionIntentStatus.INITIATED)
                .build();

        when(intentRepository.findByIdempotencyKey("TEST-IDEM-001")).thenReturn(Optional.of(existing));

        PaymentIntentResponse response = transactionCoreService.createPaymentIntent(testRequest);

        assertNotNull(response);
        assertEquals("TXN-EXISTING-01", response.getIntentId());
        verify(intentRepository, never()).save(any());
    }

    @Test
    void testReleaseEscrow_Success() {
        String intentId = "TXN-TEST-RELEASE";
        TransactionIntent intent = TransactionIntent.builder()
                .intentId(intentId)
                .domain(TransactionDomain.MARKETPLACE)
                .orderReferenceId("MKT-999")
                .amount(new BigDecimal("2000.00"))
                .status(TransactionIntentStatus.ESCROW_HELD)
                .splitsJson("[{\"recipientType\":\"MERCHANT\",\"recipientId\":50,\"amount\":2000.00,\"commissionAmount\":100.00,\"tdsAmount\":0.00}]")
                .build();

        TransactionEscrow escrow = TransactionEscrow.builder()
                .escrowId("ESC-TEST-RELEASE")
                .intentId(intentId)
                .heldAmount(new BigDecimal("2000.00"))
                .releasedAmount(BigDecimal.ZERO)
                .status("HELD")
                .build();

        when(intentRepository.findByIntentId(intentId)).thenReturn(Optional.of(intent));
        when(escrowRepository.findByIntentId(intentId)).thenReturn(Optional.of(escrow));
        when(settlementRepository.save(any(TransactionSettlement.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowReleaseRequest releaseReq = EscrowReleaseRequest.builder()
                .intentId(intentId)
                .verificationCode("OTP-9988")
                .verifiedBy("DELIVERY_AGENT")
                .releaseReason("Delivered to resident")
                .build();

        SettlementResult result = transactionCoreService.releaseEscrow(releaseReq);

        assertNotNull(result);
        assertEquals(TransactionIntentStatus.SETTLED, result.getStatus());
        assertEquals(new BigDecimal("2000.00"), result.getTotalSettled());
        assertEquals("RELEASED", escrow.getStatus());
        verify(settlementRepository, atLeastOnce()).save(any(TransactionSettlement.class));
    }

    @Test
    void testProcessRefund_InstantWallet_Success() {
        String intentId = "TXN-REFUND-001";
        TransactionIntent intent = TransactionIntent.builder()
                .intentId(intentId)
                .domain(TransactionDomain.GROUP_BUYING)
                .orderReferenceId("GB-ORDER-77")
                .payerId(15L)
                .amount(new BigDecimal("500.00"))
                .status(TransactionIntentStatus.CAPTURED)
                .build();

        when(intentRepository.findByIntentId(intentId)).thenReturn(Optional.of(intent));
        when(escrowRepository.findByIntentId(intentId)).thenReturn(Optional.empty());

        RefundRequest refundReq = RefundRequest.builder()
                .intentId(intentId)
                .refundAmount(new BigDecimal("100.00"))
                .destination(RefundDestination.WALLET_INSTANT)
                .reason("Tier discount reached higher savings")
                .build();

        RefundResult result = transactionCoreService.processRefund(refundReq);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getAmountRefunded());
        assertEquals(RefundDestination.WALLET_INSTANT, result.getDestination());
        assertEquals(TransactionIntentStatus.PARTIALLY_REFUNDED, result.getStatus());

        verify(walletEngine, times(1)).creditWallet(eq(15L), eq(new BigDecimal("100.00")),
                eq(WalletTransactionType.REFUND), anyString(), any(), anyString());
    }

    @Test
    void testGetWalletBalance() {
        CfbosWallet wallet = CfbosWallet.builder()
                .residentId(10L)
                .balance(new BigDecimal("4500.50"))
                .build();

        when(walletEngine.getOrCreateWallet(10L)).thenReturn(wallet);

        BigDecimal balance = transactionCoreService.getWalletBalance(10L);
        assertEquals(new BigDecimal("4500.50"), balance);
    }
}
