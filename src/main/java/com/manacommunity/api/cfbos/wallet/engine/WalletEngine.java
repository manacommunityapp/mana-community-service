package com.manacommunity.api.cfbos.wallet.engine;

import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.cfbos.wallet.repository.CfbosWalletRepository;
import com.manacommunity.api.cfbos.wallet.repository.CfbosWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletEngine {

    private final CfbosWalletRepository walletRepository;
    private final CfbosWalletTransactionRepository transactionRepository;

    @Transactional
    public CfbosWallet getOrCreateWallet(Long residentId) {
        return walletRepository.findByResidentId(residentId)
                .orElseGet(() -> walletRepository.save(
                        CfbosWallet.builder()
                                .residentId(residentId)
                                .balance(BigDecimal.ZERO)
                                .totalCredited(BigDecimal.ZERO)
                                .totalDebited(BigDecimal.ZERO)
                                .isActive(true)
                                .build()
                ));
    }

    @Transactional
    public CfbosWalletTransaction creditWallet(Long residentId, BigDecimal amount,
                                               WalletTransactionType type, String refType,
                                               Long refId, String narration) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CfbosException("Credit amount must be positive");
        }

        CfbosWallet wallet = getOrCreateWallet(residentId);
        BigDecimal newBalance = wallet.getBalance().add(amount);

        wallet.setBalance(newBalance);
        wallet.setTotalCredited(wallet.getTotalCredited().add(amount));
        walletRepository.save(wallet);

        CfbosWalletTransaction txn = CfbosWalletTransaction.builder()
                .wallet(wallet)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(newBalance)
                .referenceType(refType)
                .referenceId(refId)
                .narration(narration)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(txn);
    }

    @Transactional
    public CfbosWalletTransaction debitWallet(Long residentId, BigDecimal amount,
                                              String refType, Long refId, String narration) {
        return debitWallet(residentId, amount, WalletTransactionType.BILL_PAYMENT, refType, refId, narration);
    }

    @Transactional
    public CfbosWalletTransaction debitWallet(Long residentId, BigDecimal amount,
                                              WalletTransactionType type,
                                              String refType, Long refId, String narration) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CfbosException("Debit amount must be positive");
        }

        CfbosWallet wallet = getOrCreateWallet(residentId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new CfbosException("Insufficient wallet balance: available " + wallet.getBalance() + ", required " + amount);
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setTotalDebited(wallet.getTotalDebited().add(amount));
        walletRepository.save(wallet);

        CfbosWalletTransaction txn = CfbosWalletTransaction.builder()
                .wallet(wallet)
                .transactionType(type != null ? type : WalletTransactionType.BILL_PAYMENT)
                .amount(amount)
                .balanceAfter(newBalance)
                .referenceType(refType)
                .referenceId(refId)
                .narration(narration)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(txn);
    }
}
