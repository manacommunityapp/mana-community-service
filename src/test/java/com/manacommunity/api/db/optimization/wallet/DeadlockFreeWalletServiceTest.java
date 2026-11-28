package com.manacommunity.api.db.optimization.wallet;

import com.manacommunity.api.db.optimization.wallet.model.DeductionResultDto;
import com.manacommunity.api.db.optimization.wallet.model.WalletAccountDto;
import com.manacommunity.api.db.optimization.wallet.model.WalletLedgerEntryDto;
import com.manacommunity.api.db.optimization.wallet.service.DeadlockFreeWalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockFreeWalletServiceTest {

    private DeadlockFreeWalletServiceImpl walletService;
    private final Long walletId = 9001L;

    @BeforeEach
    void setUp() {
        walletService = new DeadlockFreeWalletServiceImpl();
    }

    @Test
    @DisplayName("Should execute 20 concurrent transactions on the same wallet with zero deadlocks and 100% financial consistency")
    void testConcurrentDeductionsZeroDeadlocks() throws InterruptedException, ExecutionException {
        // Initial balance: 5,000 INR
        walletService.createWallet(walletId, 201L, BigDecimal.valueOf(5000));

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<DeductionResultDto>> futures = new ArrayList<>();

        // 20 concurrent transactions: 10 milk subscriptions (50 INR each) + 5 clubhouse bookings (200 INR each) + 5 electricity prepay (100 INR each)
        // Total deduction = (10 * 50) + (5 * 200) + (5 * 100) = 500 + 1000 + 500 = 2,000 INR
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await(); // ensure all 20 threads hit the service at the exact same instant
                if (index < 10) {
                    return walletService.deductBalance(walletId, BigDecimal.valueOf(50), "MILK_SUBSCRIPTION", "MILK-" + index);
                } else if (index < 15) {
                    return walletService.deductBalance(walletId, BigDecimal.valueOf(200), "CLUBHOUSE_BOOKING", "CLUB-" + index);
                } else {
                    return walletService.deductBalance(walletId, BigDecimal.valueOf(100), "PREPAID_ELECTRICITY", "ELEC-" + index);
                }
            }));
        }

        startLatch.countDown(); // Release all 20 threads simultaneously

        for (Future<DeductionResultDto> f : futures) {
            DeductionResultDto res = f.get();
            assertTrue(res.isSuccess(), "Transaction must succeed without deadlocks");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Expected balance: 5,000 - 2,000 = 3,000
        BigDecimal finalBalance = walletService.getBalance(walletId);
        assertEquals(new BigDecimal("3000"), finalBalance);

        // Verify ledger entries
        List<WalletLedgerEntryDto> ledger = walletService.getLedgerEntries(walletId);
        assertEquals(20, ledger.size(), "All 20 ledger entries must be recorded sequentially");
    }

    @Test
    @DisplayName("Should reject deduction when balance is insufficient without locking or creating orphan records")
    void testInsufficientBalanceRejection() {
        walletService.createWallet(walletId, 202L, BigDecimal.valueOf(100));

        DeductionResultDto result = walletService.deductBalance(
                walletId, BigDecimal.valueOf(250), "CLUBHOUSE_BOOKING", "CLUB-FAIL");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("INSUFFICIENT_FUNDS", result.getErrorCode());
        assertEquals(new BigDecimal("100"), walletService.getBalance(walletId));
        assertEquals(0, walletService.getLedgerEntries(walletId).size());
    }
}
