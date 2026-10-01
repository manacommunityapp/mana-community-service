package com.manacommunity.api.cfbos.unit.wallet;

import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.cfbos.wallet.repository.CfbosWalletRepository;
import com.manacommunity.api.cfbos.wallet.repository.CfbosWalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletEngineTest {

    @Mock private CfbosWalletRepository walletRepository;
    @Mock private CfbosWalletTransactionRepository transactionRepository;

    private WalletEngine walletEngine;

    @BeforeEach
    void setUp() {
        walletEngine = new WalletEngine(walletRepository, transactionRepository);
    }

    @Test
    @DisplayName("Credit and debit wallet updates balance accurately")
    void creditAndDebitWallet() {
        CfbosWallet wallet = CfbosWallet.builder()
                .id(1L)
                .residentId(10L)
                .balance(new BigDecimal("1000.00"))
                .totalCredited(new BigDecimal("1000.00"))
                .totalDebited(BigDecimal.ZERO)
                .isActive(true)
                .build();

        when(walletRepository.findByResidentId(10L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(CfbosWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(CfbosWalletTransaction.class))).thenAnswer(i -> i.getArgument(0));

        // Credit 500
        CfbosWalletTransaction creditTxn = walletEngine.creditWallet(
                10L, new BigDecimal("500.00"), WalletTransactionType.TOPUP, "TOPUP", null, "Topup 500"
        );
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(creditTxn.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1500.00"));

        // Debit 300
        CfbosWalletTransaction debitTxn = walletEngine.debitWallet(
                10L, new BigDecimal("300.00"), "INVOICE", 101L, "Bill Payment"
        );
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(debitTxn.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }
}
