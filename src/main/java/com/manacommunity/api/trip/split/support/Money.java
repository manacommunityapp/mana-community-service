package com.manacommunity.api.trip.split.support;

import com.manacommunity.api.exception.InvalidInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Conversions between API rupee amounts and the integer paise used everywhere internally. */
public final class Money {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    /** Rupees to paise. Rejects more than two decimal places instead of silently rounding money. */
    public static long toPaise(BigDecimal rupees) {
        if (rupees == null) {
            throw new InvalidInputException("Amount is required");
        }
        try {
            return rupees.multiply(HUNDRED).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            throw new InvalidInputException("Amount can have at most two decimal places: " + rupees.toPlainString());
        }
    }

    public static BigDecimal toRupees(long paise) {
        return BigDecimal.valueOf(paise, 2);
    }

    /** Percent (33.33) to basis points (3333). At most two decimal places. */
    public static long percentToBasisPoints(BigDecimal percent) {
        if (percent == null) {
            throw new InvalidInputException("Percentage is required");
        }
        try {
            return percent.multiply(HUNDRED).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            throw new InvalidInputException("Percentage can have at most two decimal places: "
                    + percent.toPlainString());
        }
    }
}
