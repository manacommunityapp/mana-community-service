package com.manacommunity.api.db.optimization.wallet.service;

import com.manacommunity.api.db.optimization.wallet.model.DeductionResultDto;
import com.manacommunity.api.db.optimization.wallet.model.WalletAccountDto;
import com.manacommunity.api.db.optimization.wallet.model.WalletLedgerEntryDto;

import java.math.BigDecimal;
import java.util.List;

public interface DeadlockFreeWalletService {

    WalletAccountDto createWallet(Long walletId, Long residentId, BigDecimal initialBalance);

    DeductionResultDto deductBalance(Long walletId, BigDecimal amount, String transactionType, String referenceId);

    BigDecimal getBalance(Long walletId);

    List<WalletLedgerEntryDto> getLedgerEntries(Long walletId);
}
