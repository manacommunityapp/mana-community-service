package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceLoyaltyDto;
import com.manacommunity.api.commerce.core.model.CommerceLoyaltyAccount;
import com.manacommunity.api.commerce.core.model.CommerceLoyaltyTransaction;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.repository.CommerceLoyaltyAccountRepository;
import com.manacommunity.api.commerce.core.repository.CommerceLoyaltyTransactionRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loyalty points: 1 point per ₹10 spent, redeemable at 1 point = ₹0.50.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceLoyaltyService {

    private static final long POINTS_PER_RUPEE_DIVISOR = 10L;  // ₹10 → 1 point
    private static final double POINT_REDEMPTION_VALUE = 0.50; // 1 point = ₹0.50

    private final CommerceLoyaltyAccountRepository accountRepository;
    private final CommerceLoyaltyTransactionRepository transactionRepository;

    /** Called after an order reaches COMPLETED — awards loyalty points. */
    @Transactional
    public void awardPointsForOrder(CommerceOrder order) {
        if (order.getBuyer() == null || order.getTotalAmount() == null) return;
        long pts = order.getTotalAmount().toBigInteger().longValue() / POINTS_PER_RUPEE_DIVISOR;
        if (pts <= 0) return;
        AppUser buyer = order.getBuyer();
        CommerceLoyaltyAccount account = getOrCreateAccount(buyer);
        account.addPoints(pts);
        accountRepository.save(account);
        recordTransaction(account, "EARN", pts, order.getOrderNumber(),
                "Earned " + pts + " pts for order " + order.getOrderNumber());
        log.info("Loyalty: awarded {} pts to user {} for order {}", pts, buyer.getId(), order.getOrderNumber());
    }

    /** Deducts loyalty points on redemption — returns the discount amount in rupees. */
    @Transactional
    public BigDecimal redeemPoints(AppUser user, long pointsToRedeem) {
        if (pointsToRedeem <= 0) throw new IllegalArgumentException("Points to redeem must be positive");
        CommerceLoyaltyAccount account = accountRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException("No loyalty account found"));
        account.deductPoints(pointsToRedeem);
        accountRepository.save(account);
        BigDecimal discount = BigDecimal.valueOf(pointsToRedeem * POINT_REDEMPTION_VALUE);
        recordTransaction(account, "REDEEM", -pointsToRedeem, null,
                "Redeemed " + pointsToRedeem + " pts for ₹" + discount);
        return discount;
    }

    @Transactional(readOnly = true)
    public CommerceLoyaltyDto getMyLoyalty(AppUser user) {
        CommerceLoyaltyAccount account = accountRepository.findByUser_Id(user.getId())
                .orElse(null);
        if (account == null) {
            return CommerceLoyaltyDto.builder()
                    .availablePoints(0L).totalPointsEarned(0L).totalPointsSpent(0L)
                    .tier("BRONZE").recentTransactions(List.of()).build();
        }
        List<CommerceLoyaltyDto.TxnDto> txns = transactionRepository
                .findByAccount_IdOrderByCreatedAtDesc(account.getId())
                .stream().limit(20).map(this::toTxnDto).collect(Collectors.toList());
        return CommerceLoyaltyDto.builder()
                .accountId(account.getId())
                .availablePoints(account.getAvailablePoints())
                .totalPointsEarned(account.getTotalPointsEarned())
                .totalPointsSpent(account.getTotalPointsSpent())
                .tier(account.getTier())
                .recentTransactions(txns)
                .build();
    }

    private CommerceLoyaltyAccount getOrCreateAccount(AppUser user) {
        return accountRepository.findByUser_Id(user.getId()).orElseGet(() ->
                accountRepository.save(CommerceLoyaltyAccount.builder()
                        .user(user)
                        .community(user.getCommunity())
                        .build()));
    }

    private void recordTransaction(CommerceLoyaltyAccount account, String type, long pts,
                                   String orderNumber, String description) {
        transactionRepository.save(CommerceLoyaltyTransaction.builder()
                .account(account)
                .type(type)
                .points(pts)
                .balanceAfter(account.getAvailablePoints())
                .orderNumber(orderNumber)
                .description(description)
                .build());
    }

    private CommerceLoyaltyDto.TxnDto toTxnDto(CommerceLoyaltyTransaction t) {
        return CommerceLoyaltyDto.TxnDto.builder()
                .id(t.getId()).type(t.getType()).points(t.getPoints())
                .balanceAfter(t.getBalanceAfter()).description(t.getDescription())
                .orderNumber(t.getOrderNumber())
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .build();
    }
}
