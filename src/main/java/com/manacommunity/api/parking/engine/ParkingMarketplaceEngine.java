package com.manacommunity.api.parking.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class ParkingMarketplaceEngine {

    private static final BigDecimal SOCIETY_COMMISSION_PERCENT = new BigDecimal("0.10"); // 10%

    public record MarketplaceBookingCalculation(
            long totalDays,
            BigDecimal grossAmount,
            BigDecimal societyCommission,
            BigDecimal ownerPayout,
            boolean isGoodNeighbor
    ) {}

    /**
     * Calculates rental charges, commission split, and owner payout.
     */
    public MarketplaceBookingCalculation calculateBooking(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal dailyRate,
            boolean isGoodNeighbor
    ) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid booking date range");
        }

        long days = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate) + 1);

        if (isGoodNeighbor || dailyRate == null || dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return new MarketplaceBookingCalculation(days, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true);
        }

        BigDecimal gross = dailyRate.multiply(BigDecimal.valueOf(days));
        BigDecimal commission = gross.multiply(SOCIETY_COMMISSION_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payout = gross.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        return new MarketplaceBookingCalculation(days, gross.setScale(2, RoundingMode.HALF_UP), commission, payout, false);
    }

    /**
     * Validates whether a vehicle is currently eligible to enter gate based on marketplace booking window.
     */
    public boolean isAnprAuthorizedForSlot(LocalDate bookingStart, LocalDate bookingEnd, LocalDate accessDate) {
        if (bookingStart == null || bookingEnd == null || accessDate == null) return false;
        return !accessDate.isBefore(bookingStart) && !accessDate.isAfter(bookingEnd);
    }
}
