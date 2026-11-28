package com.manacommunity.api.db.optimization.wallet.service;

import com.manacommunity.api.db.optimization.wallet.model.DeductionResultDto;
import com.manacommunity.api.db.optimization.wallet.model.WalletAccountDto;
import com.manacommunity.api.db.optimization.wallet.model.WalletLedgerEntryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class DeadlockFreeWalletServiceImpl implements DeadlockFreeWalletService {

    private final Map<Long, WalletAccountDto> walletTable = new ConcurrentHashMap<>();
    private final Map<Long, List<WalletLedgerEntryDto>> ledgerTable = new ConcurrentHashMap<>();
    private final AtomicLong ledgerSeq = new AtomicLong(7000);

    @Override
    public WalletAccountDto createWallet(Long walletId, Long residentId, BigDecimal initialBalance) {
        WalletAccountDto wallet = WalletAccountDto.builder()
                .walletId(walletId)
                .residentId(residentId)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .version(1L)
                .status("ACTIVE")
                .build();

        walletTable.put(walletId, wallet);
        ledgerTable.put(walletId, new CopyOnWriteArrayList<>());
        log.info("Created wallet {} with initial balance {}", walletId, wallet.getBalance());
        return wallet;
    }

    @Override
    public DeductionResultDto deductBalance(Long walletId, BigDecimal amount, String transactionType, String referenceId) {
        WalletAccountDto wallet = walletTable.get(walletId);
        if (wallet == null) {
            return DeductionResultDto.builder()
                    .success(false)
                    .walletId(walletId)
                    .errorCode("WALLET_NOT_FOUND")
                    .errorMessage("Wallet ID " + walletId + " does not exist.")
                    .build();
        }

        // Strict Global Lock Ordering:
        // Always acquire parent lock (wallet object) BEFORE touching child ledger table.
        // This completely eliminates lock-order inversion deadlocks.
        synchronized (wallet) {
            // Atomic CAS condition: balance >= amount
            if (wallet.getBalance().compareTo(amount) < 0) {
                log.warn("Deduction failed for wallet {}: Insufficient balance (Balance: {}, Requested: {})",
                        walletId, wallet.getBalance(), amount);
                return DeductionResultDto.builder()
                        .success(false)
                        .walletId(walletId)
                        .deductedAmount(amount)
                        .balanceAfter(wallet.getBalance())
                        .transactionType(transactionType)
                        .errorCode("INSUFFICIENT_FUNDS")
                        .errorMessage("Insufficient wallet balance for " + transactionType)
                        .build();
            }

            // Step 1: Update parent entity balance
            BigDecimal newBalance = wallet.getBalance().subtract(amount);
            wallet.setBalance(newBalance);
            wallet.setVersion(wallet.getVersion() + 1);

            // Step 2: Insert into child ledger table
            long ledgerId = ledgerSeq.incrementAndGet();
            WalletLedgerEntryDto ledgerEntry = WalletLedgerEntryDto.builder()
                    .ledgerId(ledgerId)
                    .walletId(walletId)
                    .amount(amount.negate())
                    .transactionType(transactionType)
                    .balanceAfter(newBalance)
                    .referenceId(referenceId)
                    .timestamp(LocalDateTime.now())
                    .build();

            ledgerTable.get(walletId).add(ledgerEntry);

            log.info("Successfully deducted {} for {} from wallet {}. New balance: {}",
                    amount, transactionType, walletId, newBalance);

            return DeductionResultDto.builder()
                    .success(true)
                    .walletId(walletId)
                    .deductedAmount(amount)
                    .balanceAfter(newBalance)
                    .ledgerEntryId(ledgerId)
                    .transactionType(transactionType)
                    .build();
        }
    }

    @Override
    public BigDecimal getBalance(Long walletId) {
        WalletAccountDto wallet = walletTable.get(walletId);
        return wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
    }

    @Override
    public List<WalletLedgerEntryDto> getLedgerEntries(Long walletId) {
        return ledgerTable.getOrDefault(walletId, Collections.emptyList());
    }
}
