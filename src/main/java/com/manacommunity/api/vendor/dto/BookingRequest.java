package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull
    private Long vendorId;
    @NotNull
    private Long serviceId;
    @NotNull
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String bookingType;
    private String address;
    private String notes;
    private String paymentMethod;
    private List<BookingItemRequest> items;

    @Data
    public static class BookingItemRequest {
        private Long serviceId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String notes;
    }
}
