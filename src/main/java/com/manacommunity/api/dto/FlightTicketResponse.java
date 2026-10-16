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
public class FlightTicketResponse {

    private String bookingId;
    private String airlinePnr;
    private String status;              // TICKETED | FAILED
    private String eTicketUrl;          // PDF download URL
    private List<String> eTicketNumbers;
    private double totalPaid;
    private String issuedAt;            // ISO timestamp
    private String airline;
    private String flightNumber;
    private String origin;
    private String destination;
    private String departureTime;       // ISO
    private String arrivalTime;         // ISO
    private List<PassengerTicket> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerTicket {
        private String name;
        private String ticketNumber;
        private String seatNumber;
        private String mealCode;
        private String baggageAllowance;
    }
}
