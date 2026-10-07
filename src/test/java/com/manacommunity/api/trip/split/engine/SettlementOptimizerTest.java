package com.manacommunity.api.trip.split.engine;

import com.manacommunity.api.trip.split.engine.SettlementOptimizer.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettlementOptimizerTest {

    @Test
    @DisplayName("Optimizes travel group example to minimal direct payments")
    void testGoaSettlementOptimization() {
        // Sandeep=1L (+340000 paise), Ravi=2L (-140000 paise), Priya=3L (-110000 paise), Arun=4L (-90000 paise)
        Map<Long, Long> net = Map.of(
                1L, 340000L,
                2L, -140000L,
                3L, -110000L,
                4L, -90000L
        );

        List<Transfer> transfers = SettlementOptimizer.optimize(net);

        assertEquals(3, transfers.size());
        assertEquals(new Transfer(2L, 1L, 140000L), transfers.get(0));
        assertEquals(new Transfer(3L, 1L, 110000L), transfers.get(1));
        assertEquals(new Transfer(4L, 1L, 90000L), transfers.get(2));

        // Invariant check: applying transfers reduces all net positions to 0
        Map<Long, Long> remaining = new HashMap<>(net);
        for (Transfer t : transfers) {
            remaining.put(t.fromUserId(), remaining.get(t.fromUserId()) + t.amountPaise());
            remaining.put(t.toUserId(), remaining.get(t.toUserId()) - t.amountPaise());
        }
        for (long rem : remaining.values()) {
            assertEquals(0L, rem);
        }
    }

    @Test
    @DisplayName("Throws IllegalStateException if net balances do not sum to 0")
    void testNonZeroSumThrows() {
        Map<Long, Long> net = Map.of(
                1L, 5000L,
                2L, -4000L
        );

        assertThrows(IllegalStateException.class, () -> SettlementOptimizer.optimize(net));
    }
}
