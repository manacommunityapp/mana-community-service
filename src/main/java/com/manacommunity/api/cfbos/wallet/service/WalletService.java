package com.manacommunity.api.cfbos.wallet.service;

import com.manacommunity.api.cfbos.wallet.dto.*;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.entity.SecurityDeposit;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.cfbos.wallet.repository.CfbosWalletRepository;
import com.manacommunity.api.cfbos.wallet.repository.CfbosWalletTransactionRepository;
import com.manacommunity.api.cfbos.wallet.repository.SecurityDepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletEngine walletEngine;
    private final CfbosWalletRepository walletRepository;
    private final CfbosWalletTransactionRepository transactionRepository;
    private final SecurityDepositRepository securityDepositRepository;

    @Transactional
    public WalletDto getWallet(Long residentId) {
        CfbosWallet wallet = walletEngine.getOrCreateWallet(residentId);
        return toDto(wallet);
    }

    @Transactional
    public WalletTransactionDto topupWallet(TopupWalletRequest request) {
        CfbosWalletTransaction txn = walletEngine.creditWallet(
                request.getResidentId(),
                request.getAmount(),
                WalletTransactionType.TOPUP,
                "TOPUP",
                null,
                request.getNarration() != null ? request.getNarration() : "Wallet Top-up"
        );
        return toDto(txn);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionDto> getPassbook(Long residentId) {
        CfbosWallet wallet = walletEngine.getOrCreateWallet(residentId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SecurityDeposit> getSecurityDeposits(Long residentId) {
        return securityDepositRepository.findByResidentId(residentId);
    }

    @Transactional
    public SecurityDeposit saveSecurityDeposit(SecurityDeposit deposit) {
        return securityDepositRepository.save(deposit);
    }

    private WalletDto toDto(CfbosWallet w) {
        return WalletDto.builder()
                .id(w.getId())
                .residentId(w.getResidentId())
                .balance(w.getBalance())
                .totalCredited(w.getTotalCredited())
                .totalDebited(w.getTotalDebited())
                .isActive(w.getIsActive())
                .build();
    }

    private WalletTransactionDto toDto(CfbosWalletTransaction t) {
        return WalletTransactionDto.builder()
                .id(t.getId())
                .walletId(t.getWallet().getId())
                .transactionType(t.getTransactionType())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .referenceType(t.getReferenceType())
                .referenceId(t.getReferenceId())
                .narration(t.getNarration())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
