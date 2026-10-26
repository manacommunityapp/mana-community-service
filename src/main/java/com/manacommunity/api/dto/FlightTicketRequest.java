package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlightTicketRequest {

    @NotBlank
    private String bookingId;        // from FlightBookResponse

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpaySignature;
}
