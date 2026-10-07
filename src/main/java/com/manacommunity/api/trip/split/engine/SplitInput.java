package com.manacommunity.api.trip.split.engine;

/**
 * One participant's inputs for an expense. Only the field relevant to the chosen {@link SplitMethod}
 * is used: weight (SHARES), quantity (QUANTITY), percentageBp (PERCENTAGE), exactPaise (EXACT).
 * EQUAL ignores all of them.
 *
 * @param percentageBp percentage in basis points, so 33.33% is 3333 and 100% is 10000
 */
public record SplitInput(long userId, long weight, long quantity, long percentageBp, long exactPaise) {

    public static SplitInput equal(long userId) {
        return new SplitInput(userId, 1, 1, 0, 0);
    }

    public static SplitInput weight(long userId, long weight) {
        return new SplitInput(userId, weight, 1, 0, 0);
    }

    public static SplitInput quantity(long userId, long quantity) {
        return new SplitInput(userId, 1, quantity, 0, 0);
    }

    public static SplitInput percentage(long userId, long percentageBp) {
        return new SplitInput(userId, 1, 1, percentageBp, 0);
    }

    public static SplitInput exact(long userId, long exactPaise) {
        return new SplitInput(userId, 1, 1, 0, exactPaise);
    }
}
