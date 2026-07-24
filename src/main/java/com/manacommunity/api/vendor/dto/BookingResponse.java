package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String bookingNumber;
    private VendorRef vendor;
    private ServiceRef service;
    private UserRef user;
    private String bookingType;
    private String status;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalTime scheduledEndTime;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
    private String paymentStatus;
    private String paymentMethod;
    private String address;
    private String notes;
    private String cancellationReason;
    private List<BookingItemResponse> items;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class VendorRef {
        private Long id;
        private String businessName;
        private String logoUrl;
        private String phone;
    }

    @Data
    @Builder
    public static class ServiceRef {
        private Long id;
        private String name;
        private BigDecimal basePrice;
    }

    @Data
    @Builder
    public static class UserRef {
        private Long id;
        private String fullName;
        private String phone;
    }

    @Data
    @Builder
    public static class BookingItemResponse {
        private Long id;
        private String serviceName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
    }
}
