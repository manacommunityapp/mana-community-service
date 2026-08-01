package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResourceBookingResponse {

    private Long id;
    private String bookingNumber;
    private Long resourceId;
    private String resourceName;
    private String resourceCategory;
    private String bookingDate;
    private String startTime;
    private String endTime;
    private String endDate;
    private String purpose;
    private Integer numberOfGuests;
    private String status;
    private String approvalStatus;
    private String qrCode;
    private String checkedInAt;
    private String checkedOutAt;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String paymentStatus;
    private String paymentReference;
    private Boolean isRecurring;
    private Integer rating;
    private String ratingComment;
    private Long bookedById;
    private String bookedByName;
    private String approvedByName;
    private String cancellationReason;
    private List<EquipmentResponse> equipment;
    private String createdAt;
    private String updatedAt;
}
