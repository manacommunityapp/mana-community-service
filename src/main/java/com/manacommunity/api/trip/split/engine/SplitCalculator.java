package com.manacommunity.api.trip.split.engine;

import com.manacommunity.api.exception.InvalidInputException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Divides an expense (in paise) among participants. The returned shares always sum to exactly the
 * expense total: proportional methods use largest-remainder rounding, so no paisa is created or lost.
 */
public final class SplitCalculator {

    private static final long TOTAL_BASIS_POINTS = 10_000L;

    private SplitCalculator() {
    }

    /**
     * @return userId to share in paise, in the same order as {@code inputs}
     * @throws InvalidInputException if the inputs are inconsistent with the method
     */
    public static Map<Long, Long> calculate(SplitMethod method, long totalPaise, List<SplitInput> inputs) {
        if (method == null) {
            throw new InvalidInputException("Split method is required");
        }
        if (totalPaise <= 0) {
            throw new InvalidInputException("Expense amount must be greater than zero");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidInputException("At least one participant is required to split an expense");
        }
        Set<Long> seen = new HashSet<>();
        for (SplitInput in : inputs) {
            if (!seen.add(in.userId())) {
                throw new InvalidInputException("Participant listed more than once: " + in.userId());
            }
        }

        return switch (method) {
            case EQUAL -> proportional(totalPaise, inputs, in -> 1L);
            case SHARES -> proportional(totalPaise, inputs, in -> requirePositive(in.weight(), "Share weight"));
            case QUANTITY -> proportional(totalPaise, inputs, in -> requirePositive(in.quantity(), "Quantity"));
            case PERCENTAGE -> percentage(totalPaise, inputs);
            case EXACT -> exact(totalPaise, inputs);
        };
    }

    private interface Weight {
        long of(SplitInput input);
    }

    private static Map<Long, Long> percentage(long totalPaise, List<SplitInput> inputs) {
        long sum = 0;
        for (SplitInput in : inputs) {
            if (in.percentageBp() < 0) {
                throw new InvalidInputException("Percentage cannot be negative");
            }
            sum += in.percentageBp();
        }
        if (sum != TOTAL_BASIS_POINTS) {
            throw new InvalidInputException("Percentages must add up to 100 but add up to " + (sum / 100.0));
        }
        // Zero-percent participants simply receive a zero share.
        return proportional(totalPaise, inputs, SplitInput::percentageBp);
    }

    private static Map<Long, Long> exact(long totalPaise, List<SplitInput> inputs) {
        long sum = 0;
        Map<Long, Long> result = new LinkedHashMap<>();
        for (SplitInput in : inputs) {
            if (in.exactPaise() < 0) {
                throw new InvalidInputException("Exact amount cannot be negative");
            }
            sum += in.exactPaise();
            result.put(in.userId(), in.exactPaise());
        }
        if (sum != totalPaise) {
            throw new InvalidInputException("Exact amounts add up to " + sum + " paise but the expense is "
                    + totalPaise + " paise");
        }
        return result;
    }

    /** Largest-remainder apportionment. Ties on the remainder go to the earlier participant. */
    private static Map<Long, Long> proportional(long totalPaise, List<SplitInput> inputs, Weight weight) {
        long weightSum = 0;
        long[] weights = new long[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            weights[i] = weight.of(inputs.get(i));
            weightSum += weights[i];
        }
        if (weightSum <= 0) {
            throw new InvalidInputException("Split weights must add up to more than zero");
        }

        long[] shares = new long[inputs.size()];
        long[] remainders = new long[inputs.size()];
        long allocated = 0;
        for (int i = 0; i < inputs.size(); i++) {
            long product = Math.multiplyExact(totalPaise, weights[i]);
            shares[i] = product / weightSum;
            remainders[i] = product % weightSum;
            allocated += shares[i];
        }

        long leftover = totalPaise - allocated;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            order.add(i);
        }
        // Stable sort: equal remainders keep input order.
        order.sort((a, b) -> Long.compare(remainders[b], remainders[a]));
        for (int k = 0; k < leftover; k++) {
            shares[order.get(k)]++;
        }

        Map<Long, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            result.put(inputs.get(i).userId(), shares[i]);
        }
        return result;
    }

    private static long requirePositive(long value, String what) {
        if (value <= 0) {
            throw new InvalidInputException(what + " must be greater than zero");
        }
        return value;
    }
}
