package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodPayment;
import com.manacommunity.api.food.entity.FoodWallet;
import com.manacommunity.api.food.entity.FoodWalletTransaction;
import com.manacommunity.api.food.repository.FoodPaymentRepository;
import com.manacommunity.api.food.repository.FoodWalletRepository;
import com.manacommunity.api.food.repository.FoodWalletTransactionRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodPaymentService {

    private final FoodPaymentRepository paymentRepo;
    private final FoodWalletRepository walletRepo;
    private final FoodWalletTransactionRepository walletTransactionRepo;

    @Transactional
    public Map<String, Object> createPayment(FoodPayment.PaymentOrderType orderType, Long orderId,
                                              BigDecimal amount, FoodPayment.FoodPaymentMethod paymentMethod,
                                              AppUser user, Community community) {
        FoodPayment payment = FoodPayment.builder()
                .orderType(orderType)
                .orderId(orderId)
                .user(user)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(FoodPayment.PaymentStatus.PENDING)
                .community(community)
                .build();

        FoodPayment saved = paymentRepo.save(payment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentByOrder(String orderType, Long orderId) {
        List<FoodPayment> payments = paymentRepo.findByOrderTypeAndOrderId(orderType, orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Payment", "orderId", orderId.toString());
        }
        return toResponse(payments.get(0));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyPayments(Long userId, Long communityId, Pageable pageable) {
        return paymentRepo.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWallet(Long userId, Long communityId) {
        FoodWallet wallet = walletRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId.toString()));

        Map<String, Object> map = new HashMap<>();
        map.put("id", wallet.getId());
        map.put("userId", wallet.getUser().getId());
        map.put("balance", wallet.getBalance());
        map.put("communityId", wallet.getCommunity() != null ? wallet.getCommunity().getId() : null);
        map.put("createdAt", wallet.getCreatedAt());
        map.put("updatedAt", wallet.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> creditWallet(Long userId, BigDecimal amount, String referenceType,
                                             Long referenceId, Long communityId) {
        FoodWallet wallet = walletRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId.toString()));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepo.save(wallet);

        FoodWalletTransaction txn = FoodWalletTransaction.builder()
                .wallet(wallet)
                .transactionType(FoodWalletTransaction.WalletTransactionType.CREDIT)
                .amount(amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description("Credit of " + amount + " for " + referenceType)
                .balanceAfter(wallet.getBalance())
                .build();

        FoodWalletTransaction savedTxn = walletTransactionRepo.save(txn);

        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", savedTxn.getId());
        map.put("walletId", wallet.getId());
        map.put("amount", amount);
        map.put("type", "CREDIT");
        map.put("balanceAfter", wallet.getBalance());
        map.put("referenceType", referenceType);
        map.put("referenceId", referenceId);
        return map;
    }

    @Transactional
    public Map<String, Object> debitWallet(Long userId, BigDecimal amount, String referenceType,
                                            Long referenceId, Long communityId) {
        FoodWallet wallet = walletRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId.toString()));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance. Available: " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepo.save(wallet);

        FoodWalletTransaction txn = FoodWalletTransaction.builder()
                .wallet(wallet)
                .transactionType(FoodWalletTransaction.WalletTransactionType.DEBIT)
                .amount(amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description("Debit of " + amount + " for " + referenceType)
                .balanceAfter(wallet.getBalance())
                .build();

        FoodWalletTransaction savedTxn = walletTransactionRepo.save(txn);

        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", savedTxn.getId());
        map.put("walletId", wallet.getId());
        map.put("amount", amount);
        map.put("type", "DEBIT");
        map.put("balanceAfter", wallet.getBalance());
        map.put("referenceType", referenceType);
        map.put("referenceId", referenceId);
        return map;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getWalletTransactions(Long userId, Long communityId, Pageable pageable) {
        FoodWallet wallet = walletRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId.toString()));

        return walletTransactionRepo.findByWalletId(wallet.getId(), pageable)
                .map(txn -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", txn.getId());
                    map.put("walletId", txn.getWallet().getId());
                    map.put("transactionType", txn.getTransactionType() != null ? txn.getTransactionType().name() : null);
                    map.put("amount", txn.getAmount());
                    map.put("referenceType", txn.getReferenceType());
                    map.put("referenceId", txn.getReferenceId());
                    map.put("description", txn.getDescription());
                    map.put("balanceAfter", txn.getBalanceAfter());
                    map.put("createdAt", txn.getCreatedAt());
                    return map;
                });
    }

    private Map<String, Object> toResponse(FoodPayment payment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", payment.getId());
        map.put("orderType", payment.getOrderType() != null ? payment.getOrderType().name() : null);
        map.put("orderId", payment.getOrderId());
        map.put("userId", payment.getUser().getId());
        map.put("amount", payment.getAmount());
        map.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
        map.put("paymentGateway", payment.getPaymentGateway());
        map.put("transactionId", payment.getTransactionId());
        map.put("status", payment.getStatus() != null ? payment.getStatus().name() : null);
        map.put("gatewayResponse", payment.getGatewayResponse());
        map.put("communityId", payment.getCommunity() != null ? payment.getCommunity().getId() : null);
        map.put("createdAt", payment.getCreatedAt());
        map.put("updatedAt", payment.getUpdatedAt());
        return map;
    }
}
