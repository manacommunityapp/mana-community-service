package com.manacommunity.api.trip.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CommuteFareEngine {

    private static final BigDecimal DEFAULT_FUEL_RATE_PER_KM = new BigDecimal("8.00");
    private static final BigDecimal MAX_SURCHARGE_FACTOR = new BigDecimal("1.50");

    public BigDecimal calculateRecommendedFare(Double distanceKm, int totalSeats, BigDecimal tollCost, boolean isAc) {
        if (distanceKm == null || distanceKm <= 0 || totalSeats <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dist = BigDecimal.valueOf(distanceKm);
        BigDecimal fuelCost = dist.multiply(DEFAULT_FUEL_RATE_PER_KM);
        if (isAc) {
            fuelCost = fuelCost.multiply(new BigDecimal("1.10"));
        }

        BigDecimal totalTripCost = fuelCost;
        if (tollCost != null && tollCost.compareTo(BigDecimal.ZERO) > 0) {
            totalTripCost = totalTripCost.add(tollCost);
        }

        int totalOccupants = totalSeats + 1;
        return totalTripCost.divide(BigDecimal.valueOf(totalOccupants), 2, RoundingMode.HALF_UP);
    }

    public boolean isFareWithinFairLimits(BigDecimal requestedPrice, Double distanceKm, int totalSeats) {
        if (requestedPrice == null || requestedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        BigDecimal recommended = calculateRecommendedFare(distanceKm, totalSeats, BigDecimal.ZERO, true);
        BigDecimal cap = recommended.multiply(MAX_SURCHARGE_FACTOR).max(new BigDecimal("30.00"));
        return requestedPrice.compareTo(cap) <= 0;
    }

    public BigDecimal calculateBookingTotal(BigDecimal pricePerSeat, int seatsBooked, BigDecimal discountPercentage) {
        if (pricePerSeat == null || pricePerSeat.compareTo(BigDecimal.ZERO) <= 0 || seatsBooked <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = pricePerSeat.multiply(BigDecimal.valueOf(seatsBooked));
        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = subtotal.multiply(discountPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            return subtotal.subtract(discount).max(BigDecimal.ZERO);
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }
}
