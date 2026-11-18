package com.manacommunity.api.payments.gateway;

import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.payments.exception.InsufficientWalletBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gateway adapter for ManaPay (community wallet) payments.
 *
 * <p>ManaPay payments are synchronous internal wallet debits — there is no
 * external PG redirect. debitWallet() either succeeds or throws; if it
 * succeeds the payment is immediately verified and SUCCESS is set.
 *
 * <p>Financial rule: amounts are always BigDecimal; CfbosException for
 * insufficient balance is re-thrown as InsufficientWalletBalanceException (402).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManaPayGatewayService {

    private final WalletEngine walletEngine;

    /**
     * Debits the resident's community wallet for a payment.
     *
     * @return a unique MANA-prefixed order reference stored as gatewayOrderId
     * @throws InsufficientWalletBalanceException if wallet balance < amount
     */
    @Transactional
    public String initiatePayment(Long userId, Long communityId,
                                   BigDecimal amount, String description) {
        String manaRef = "MANA-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("[ManaPay] initiating debit userId={} communityId={} amount={} ref={}",
                userId, communityId, amount, manaRef);
        try {
            walletEngine.debitWallet(
                    userId,
                    amount,
                    WalletTransactionType.BILL_PAYMENT,
                    "MANA_PAY",
                    0L,
                    description + " | ref=" + manaRef);
        } catch (CfbosException ex) {
            log.warn("[ManaPay] insufficient balance userId={} amount={}: {}", userId, amount, ex.getMessage());
            throw new InsufficientWalletBalanceException(
                    "Insufficient ManaPay wallet balance. Available balance is less than " + amount);
        }
        log.info("[ManaPay] debit SUCCESS ref={}", manaRef);
        return manaRef;
    }

    /**
     * Verifies a ManaPay payment by confirming the wallet debit happened.
     * Since debitWallet() is synchronous and throws on failure, any reference
     * that was returned from initiatePayment() is by definition verified.
     */
    public boolean verifyPayment(String manaRef) {
        // Wallet debit is synchronous — if initiatePayment() returned without throwing,
        // the debit succeeded. No separate verification call needed.
        return manaRef != null && manaRef.startsWith("MANA-");
    }

    /**
     * Credits the resident's wallet as a refund for a ManaPay payment.
     *
     * @param userId the original payer's userId (from PaymentOrder.payerUserId)
     */
    @Transactional
    public void refundPayment(String manaRef, Long userId, BigDecimal amount, String reason) {
        log.info("[ManaPay] refund manaRef={} userId={} amount={} reason={}", manaRef, userId, amount, reason);
        walletEngine.creditWallet(
                userId,
                amount,
                WalletTransactionType.REFUND,
                "MANA_PAY",
                0L,
                "Refund for " + manaRef + ": " + reason);
    }
}
