package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchResponse {

    private String searchId;         // Tripjack searchId for follow-up calls
    private List<FlightOption> flights;
    private int totalResults;
    private String currency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightOption {
        private String optionId;         // Tripjack result key
        private List<FlightSegment> segments;
        private FareInfo fare;
        private int totalDurationMinutes;
        private int stops;
        private boolean refundable;
        private String fareIdentifier;   // PUBLISHED | CORPORATE | SME
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightSegment {
        private String flightNumber;     // "6E-234"
        private String airlineCode;      // "6E"
        private String airlineName;      // "IndiGo"
        private String aircraftType;     // "320"
        private String originCode;
        private String originCity;
        private String originTerminal;
        private String originTime;       // ISO "2026-11-15T06:00:00"
        private String destinationCode;
        private String destinationCity;
        private String destinationTerminal;
        private String destinationTime;  // ISO "2026-11-15T07:15:00"
        private int durationMinutes;
        private int stopCount;
        private int cabinBaggage;        // kg
        private int checkInBaggage;      // kg
        private boolean mealIncluded;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FareInfo {
        private double baseFare;
        private double taxesAndFees;
        private double totalFare;        // per adult
        private double totalFareAllPax;  // total for all passengers
        private int adults;
        private int children;
        private int infants;
    }
}
