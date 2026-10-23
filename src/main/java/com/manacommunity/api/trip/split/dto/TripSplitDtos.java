package com.manacommunity.api.trip.split.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Request and response shapes for Trip Split. Amounts are rupees with at most two decimals. */
public final class TripSplitDtos {

    private TripSplitDtos() {
    }

    /** Only the field matching the split method is read: weight, quantity, percentage or amount. */
    public record ParticipantInput(Long userId, Long weight, Long quantity, BigDecimal percentage, BigDecimal amount) {
    }

    /**
     * @param splitMethod EQUAL, PERCENTAGE, EXACT, QUANTITY or SHARES. Leave empty to record the expense now
     *                    and split it later (it stays out of balances until split).
     */
    public record ExpenseRequest(String categoryCode, String description, BigDecimal totalAmount, Long paidByUserId,
                                 LocalDate expenseDate, String currency, String receiptUrl, String splitMethod,
                                 List<ParticipantInput> participants) {
    }

    public record ShareView(Long userId, String userName, BigDecimal share) {
    }

    public record ExpenseView(Long id, String tripId, String categoryCode, String description, BigDecimal totalAmount,
                              String currency, Long paidByUserId, String paidByName, LocalDate expenseDate,
                              String status, String splitMethod, String receiptUrl, List<ShareView> shares) {
    }

    /** @param position RECEIVES, OWES or SETTLED */
    public record BalanceView(Long userId, String userName, BigDecimal paid, BigDecimal share,
                              BigDecimal settledSent, BigDecimal settledReceived, BigDecimal net, String position) {
    }

    public record SummaryView(String tripId, BigDecimal totalSpent, BigDecimal unsplitAmount,
                              List<BalanceView> balances) {
    }

    public record TransferView(Long fromUserId, String fromName, Long toUserId, String toName, BigDecimal amount) {
    }

    public record PaymentRequest(Long toUserId, BigDecimal amount, String method, String reference) {
    }

    public record PaymentView(Long id, Long fromUserId, String fromName, Long toUserId, String toName,
                              BigDecimal amount, String status, String method, String reference,
                              LocalDateTime createdAt, LocalDateTime confirmedAt) {
    }

    public record BudgetRequest(BigDecimal estimatedAmount, String currency) {
    }

    /** Budget progress plus the calling user's own position. */
    public record DashboardView(BigDecimal estimated, BigDecimal spent, BigDecimal remaining, BigDecimal unsplit,
                                BigDecimal yourShare, BigDecimal youPaid, BigDecimal youReceive, BigDecimal youOwe,
                                Map<String, BigDecimal> spentByCategory) {
    }

    /** @param mode OFF, SHARE or SETTLEMENTS */
    public record PrefRequest(String mode) {
    }

    public record PrefView(String mode) {
    }
}
