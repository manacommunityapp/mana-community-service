package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightBookResponse {

    private String bookingId;       // Mana internal booking reference
    private String airlinePnr;      // Airline/Tripjack PNR e.g. "ABCXYZ"
    private String tripjackOrderId; // Tripjack order ID for ticketing
    private String status;          // HOLD | TICKETED | FAILED
    private double totalAmount;
    private String currency;
    private String holdExpiresAt;   // ISO — must ticket before this time
    private String paymentRef;      // Razorpay order ID (set after payment init)
}
