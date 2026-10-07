package com.manacommunity.api.trip.split.engine;

import com.manacommunity.api.trip.split.engine.BalanceCalculator.Balance;
import com.manacommunity.api.trip.split.engine.BalanceCalculator.ExpenseLine;
import com.manacommunity.api.trip.split.engine.BalanceCalculator.PaymentLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BalanceCalculatorTest {

    @Test
    @DisplayName("Computes paid, share, and nets, and accounts for confirmed reimbursement payments")
    void testComputeBalancesWithReimbursements() {
        // Participant 1 paid 3000, split equally with participant 2 (1500 each)
        // Participant 2 then pays participant 1 500
        List<Long> participants = List.of(1L, 2L);
        List<ExpenseLine> expenses = List.of(
                new ExpenseLine(1L, 3000L, Map.of(1L, 1500L, 2L, 1500L))
        );
        List<PaymentLine> payments = List.of(
                new PaymentLine(2L, 1L, 500L)
        );

        Map<Long, Balance> balances = BalanceCalculator.compute(participants, expenses, payments);

        Balance b1 = balances.get(1L);
        assertEquals(3000L, b1.paidPaise());
        assertEquals(1500L, b1.sharePaise());
        assertEquals(0L, b1.sentPaise());
        assertEquals(500L, b1.receivedPaise());
        // Net: 3000 - 1500 + 0 - 500 = +1000
        assertEquals(1000L, b1.netPaise());

        Balance b2 = balances.get(2L);
        assertEquals(0L, b2.paidPaise());
        assertEquals(1500L, b2.sharePaise());
        assertEquals(500L, b2.sentPaise());
        assertEquals(0L, b2.receivedPaise());
        // Net: 0 - 1500 + 500 - 0 = -1000
        assertEquals(-1000L, b2.netPaise());

        assertEquals(0L, b1.netPaise() + b2.netPaise());
    }
}
