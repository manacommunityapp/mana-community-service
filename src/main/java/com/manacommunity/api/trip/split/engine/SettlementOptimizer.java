package com.manacommunity.api.trip.split.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Turns net balances into a small list of transfers. Repeatedly matches the person owed the most with
 * the person who owes the most, which settles everyone in at most (people - 1) transfers.
 * (The true minimum is NP-hard; this greedy result is what the apps people compare against produce.)
 */
public final class SettlementOptimizer {

    private SettlementOptimizer() {
    }

    public record Transfer(long fromUserId, long toUserId, long amountPaise) {
    }

    private record Entry(long userId, long amountPaise) {
    }

    /**
     * @param netByUser positive = should receive, negative = owes. Must sum to zero.
     */
    public static List<Transfer> optimize(Map<Long, Long> netByUser) {
        long sum = netByUser.values().stream().mapToLong(Long::longValue).sum();
        if (sum != 0) {
            throw new IllegalStateException("Net balances must sum to zero but sum to " + sum + " paise");
        }

        // Largest amount first; ties broken by user id so results are deterministic.
        Comparator<Entry> order = Comparator.comparingLong(Entry::amountPaise).reversed()
                .thenComparingLong(Entry::userId);
        PriorityQueue<Entry> creditors = new PriorityQueue<>(order);
        PriorityQueue<Entry> debtors = new PriorityQueue<>(order);
        netByUser.forEach((id, net) -> {
            if (net > 0) {
                creditors.add(new Entry(id, net));
            } else if (net < 0) {
                debtors.add(new Entry(id, -net));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Entry creditor = creditors.poll();
            Entry debtor = debtors.poll();
            long amount = Math.min(creditor.amountPaise(), debtor.amountPaise());
            transfers.add(new Transfer(debtor.userId(), creditor.userId(), amount));
            if (creditor.amountPaise() > amount) {
                creditors.add(new Entry(creditor.userId(), creditor.amountPaise() - amount));
            }
            if (debtor.amountPaise() > amount) {
                debtors.add(new Entry(debtor.userId(), debtor.amountPaise() - amount));
            }
        }
        return transfers;
    }
}
