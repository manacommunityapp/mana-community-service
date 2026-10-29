package com.manacommunity.api.trip.split.engine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Computes who has paid, who owes, and the net position of each participant.
 * Net = paid - share + settlementsSent - settlementsReceived. Positive means the person should receive
 * money; negative means they owe. Across a trip the nets always sum to zero.
 */
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    /** An expense that has been split. {@code shares} must sum to {@code totalPaise}. */
    public record ExpenseLine(long paidByUserId, long totalPaise, Map<Long, Long> shares) {
    }

    /** A confirmed reimbursement from one participant to another. */
    public record PaymentLine(long fromUserId, long toUserId, long amountPaise) {
    }

    public record Balance(long userId, long paidPaise, long sharePaise, long sentPaise, long receivedPaise) {
        public long netPaise() {
            return paidPaise - sharePaise + sentPaise - receivedPaise;
        }
    }

    /**
     * @param participantIds everyone who should appear in the result, even with no activity
     */
    public static Map<Long, Balance> compute(Collection<Long> participantIds,
                                             Collection<ExpenseLine> expenses,
                                             Collection<PaymentLine> payments) {
        Map<Long, long[]> acc = new LinkedHashMap<>(); // [paid, share, sent, received]
        for (Long id : participantIds) {
            acc.computeIfAbsent(id, k -> new long[4]);
        }
        for (ExpenseLine e : expenses) {
            acc.computeIfAbsent(e.paidByUserId(), k -> new long[4])[0] += e.totalPaise();
            for (Map.Entry<Long, Long> share : e.shares().entrySet()) {
                acc.computeIfAbsent(share.getKey(), k -> new long[4])[1] += share.getValue();
            }
        }
        for (PaymentLine p : payments) {
            acc.computeIfAbsent(p.fromUserId(), k -> new long[4])[2] += p.amountPaise();
            acc.computeIfAbsent(p.toUserId(), k -> new long[4])[3] += p.amountPaise();
        }

        Map<Long, Balance> result = new LinkedHashMap<>();
        acc.forEach((id, v) -> result.put(id, new Balance(id, v[0], v[1], v[2], v[3])));
        return result;
    }
}
