package com.manacommunity.api.trip.split.engine;

import com.manacommunity.api.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    @Test
    @DisplayName("EQUAL: 100 rupees (10000 paise) split among 3 people distributes remainder to first participant")
    void testEqualSplitWithRemainder() {
        List<SplitInput> inputs = List.of(
                SplitInput.equal(1L),
                SplitInput.equal(2L),
                SplitInput.equal(3L)
        );

        Map<Long, Long> shares = SplitCalculator.calculate(SplitMethod.EQUAL, 10000L, inputs);

        assertEquals(3334L, shares.get(1L));
        assertEquals(3333L, shares.get(2L));
        assertEquals(3333L, shares.get(3L));
        assertEquals(10000L, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("Goa example: Activities 4000 rupees (400000 paise) split equally among 5 participants")
    void testGoaActivitiesSplit() {
        List<SplitInput> inputs = List.of(
                SplitInput.equal(1L),
                SplitInput.equal(2L),
                SplitInput.equal(3L),
                SplitInput.equal(4L),
                SplitInput.equal(5L)
        );

        Map<Long, Long> shares = SplitCalculator.calculate(SplitMethod.EQUAL, 400000L, inputs);

        assertEquals(80000L, shares.get(1L));
        assertEquals(80000L, shares.get(2L));
        assertEquals(80000L, shares.get(3L));
        assertEquals(80000L, shares.get(4L));
        assertEquals(80000L, shares.get(5L));
        assertEquals(400000L, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("PERCENTAGE: splits according to basis points totaling 10000")
    void testPercentageSplit() {
        List<SplitInput> inputs = List.of(
                SplitInput.percentage(1L, 5000L),
                SplitInput.percentage(2L, 3000L),
                SplitInput.percentage(3L, 2000L)
        );

        Map<Long, Long> shares = SplitCalculator.calculate(SplitMethod.PERCENTAGE, 100000L, inputs);

        assertEquals(50000L, shares.get(1L));
        assertEquals(30000L, shares.get(2L));
        assertEquals(20000L, shares.get(3L));
        assertEquals(100000L, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("PERCENTAGE: throws when total percentage is not 100%")
    void testPercentageSplitMustTotal100() {
        List<SplitInput> inputs = List.of(
                SplitInput.percentage(1L, 5000L),
                SplitInput.percentage(2L, 4000L)
        );

        assertThrows(InvalidInputException.class, () ->
                SplitCalculator.calculate(SplitMethod.PERCENTAGE, 100000L, inputs));
    }

    @Test
    @DisplayName("EXACT: shares must sum to total amount")
    void testExactSplit() {
        List<SplitInput> inputs = List.of(
                SplitInput.exact(1L, 6000L),
                SplitInput.exact(2L, 4000L)
        );

        Map<Long, Long> shares = SplitCalculator.calculate(SplitMethod.EXACT, 10000L, inputs);

        assertEquals(6000L, shares.get(1L));
        assertEquals(4000L, shares.get(2L));
        assertEquals(10000L, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("EXACT: throws when shares do not equal total amount")
    void testExactSplitMismatch() {
        List<SplitInput> inputs = List.of(
                SplitInput.exact(1L, 6000L),
                SplitInput.exact(2L, 5000L)
        );

        assertThrows(InvalidInputException.class, () ->
                SplitCalculator.calculate(SplitMethod.EXACT, 10000L, inputs));
    }

    @Test
    @DisplayName("QUANTITY: proportional to unit count")
    void testQuantitySplit() {
        List<SplitInput> inputs = List.of(
                SplitInput.quantity(1L, 3L),
                SplitInput.quantity(2L, 1L)
        );

        Map<Long, Long> shares = SplitCalculator.calculate(SplitMethod.QUANTITY, 1200000L, inputs);

        assertEquals(900000L, shares.get(1L));
        assertEquals(300000L, shares.get(2L));
        assertEquals(1200000L, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("SHARES: weight based split")
    void testSharesWeightSplit() {
        List<SplitInput> inputs = List.of(
                SplitInput.weight(1L, 2L),
                SplitInput.weight(2L, 1L)
        );

        Map<Long, Long> shares = SplitCalculator.calculate(SplitMethod.SHARES, 30000L, inputs);

        assertEquals(20000L, shares.get(1L));
        assertEquals(10000L, shares.get(2L));
        assertEquals(30000L, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("Rejects duplicate participants or non-positive total")
    void testValidationErrors() {
        assertThrows(InvalidInputException.class, () ->
                SplitCalculator.calculate(SplitMethod.EQUAL, 0L, List.of(SplitInput.equal(1L))));

        assertThrows(InvalidInputException.class, () ->
                SplitCalculator.calculate(SplitMethod.EQUAL, 1000L, List.of()));

        assertThrows(InvalidInputException.class, () ->
                SplitCalculator.calculate(SplitMethod.EQUAL, 1000L, List.of(
                        SplitInput.equal(1L),
                        SplitInput.equal(1L)
                )));
    }
}
