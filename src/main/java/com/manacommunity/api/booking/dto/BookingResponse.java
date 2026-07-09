package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {

    private Long id;
    private Long amenityId;
    private String amenityName;
    private String amenityType;
    private String bookingDate;
    private String startTime;
    private String endTime;
    private String purpose;
    private String status;
    private Long bookedById;
    private String bookedByName;
    private String createdAt;
}
