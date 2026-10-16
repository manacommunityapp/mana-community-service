package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightFareQuoteResponse {

    private String optionId;
    private boolean priceChanged;          // true if price shifted since search
    private double previousTotalFare;
    private double currentTotalFare;
    private double baseFare;
    private double taxesAndFees;
    private double totalFareAllPax;
    private int seatsAvailable;
    private boolean available;             // false = flight no longer bookable
    private String fareIdentifier;
    private String expiresAt;              // ISO — hold must be confirmed before this
}
