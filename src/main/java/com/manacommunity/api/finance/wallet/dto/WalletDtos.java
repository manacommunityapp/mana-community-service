package com.manacommunity.api.finance.wallet.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class WalletDtos {

    public record WalletResponse(
        Long id, Long residentId, Long communityId,
        BigDecimal balance, BigDecimal creditLimit,
        String currency, String status, String upiVirtualId
    ) {}

    public record WalletTransactionResponse(
        Long id, String txnType, String direction,
        BigDecimal amount, BigDecimal balanceAfter,
        String referenceType, Long referenceId,
        String description, String paymentMethod,
        String externalTxnRef, String status,
        OffsetDateTime initiatedAt, OffsetDateTime completedAt
    ) {}

    public record TopupRequest(BigDecimal amount, String paymentMethod) {}

    public record TopupInitResult(String paymentUrl, String externalTxnRef, String status) {}

    public record ConfirmTopupRequest(String externalTxnRef) {}

    public record PayInvoiceRequest(Long walletId, Long invoiceId, BigDecimal amount) {}

    public record PayBookingRequest(Long walletId, Long bookingId, BigDecimal amount) {}

    public record SettleExpenseRequest(Long walletId, Long expenseId, BigDecimal amount) {}

    public record ApproveCreditLineRequest(Long walletId, BigDecimal amount, int repaymentDays) {}

    public record RefundRequest(String reason) {}

    public record CreditLineResponse(
        Long id, Long walletId, BigDecimal approvedAmount,
        BigDecimal drawnAmount, BigDecimal interestRatePct,
        int repaymentDays, String status, OffsetDateTime approvedAt, OffsetDateTime expiresAt
    ) {}
}
