package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceWalletDto;
import com.manacommunity.api.commerce.core.model.CommerceWallet;
import com.manacommunity.api.commerce.core.model.CommerceWalletTransaction;
import com.manacommunity.api.commerce.core.repository.CommerceWalletRepository;
import com.manacommunity.api.commerce.core.repository.CommerceWalletTransactionRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceWalletService {

    private final CommerceWalletRepository walletRepository;
    private final CommerceWalletTransactionRepository txnRepository;

    @Transactional
    public CommerceWalletDto getOrCreateWallet(AppUser user) {
        CommerceWallet wallet = walletRepository
                .findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseGet(() -> walletRepository.save(CommerceWallet.builder()
                        .user(user).community(user.getCommunity()).build()));
        return toDto(wallet, true);
    }

    @Transactional
    public CommerceWalletDto topUp(AppUser user, BigDecimal amount, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive.");
        CommerceWallet wallet = getWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);
        recordTxn(wallet, "TOPUP", amount, referenceId, "Wallet top-up");
        return toDto(wallet, false);
    }

    @Transactional
    public CommerceWalletDto debit(AppUser user, BigDecimal amount, String referenceId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive.");
        CommerceWallet wallet = getWallet(user);
        if (wallet.getBalance().compareTo(amount) < 0) throw new IllegalStateException("Insufficient wallet balance.");
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet = walletRepository.save(wallet);
        recordTxn(wallet, "PAYMENT", amount.negate(), referenceId, description);
        return toDto(wallet, false);
    }

    @Transactional
    public CommerceWalletDto refund(AppUser user, BigDecimal amount, String referenceId) {
        CommerceWallet wallet = getWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);
        recordTxn(wallet, "REFUND", amount, referenceId, "Refund to wallet");
        return toDto(wallet, false);
    }

    private CommerceWallet getWallet(AppUser user) {
        return walletRepository.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found. Please initialise wallet first."));
    }

    private void recordTxn(CommerceWallet wallet, String type, BigDecimal amount, String refId, String desc) {
        txnRepository.save(CommerceWalletTransaction.builder()
                .wallet(wallet)
                .txnRef("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .txnType(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceId(refId)
                .description(desc)
                .build());
    }

    private CommerceWalletDto toDto(CommerceWallet w, boolean includeTxns) {
        List<CommerceWalletDto.WalletTxnDto> txns = null;
        if (includeTxns) {
            txns = txnRepository.findByWalletIdOrderByCreatedAtDesc(w.getId()).stream()
                    .map(t -> CommerceWalletDto.WalletTxnDto.builder()
                            .id(t.getId()).txnRef(t.getTxnRef()).txnType(t.getTxnType())
                            .amount(t.getAmount()).balanceAfter(t.getBalanceAfter())
                            .referenceId(t.getReferenceId()).description(t.getDescription())
                            .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());
        }
        return CommerceWalletDto.builder()
                .id(w.getId()).userId(w.getUser().getId()).userName(w.getUser().getFullName())
                .balance(w.getBalance()).currency(w.getCurrency()).status(w.getStatus())
                .transactions(txns)
                .build();
    }
}
