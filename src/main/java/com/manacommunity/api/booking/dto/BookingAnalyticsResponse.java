package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BookingAnalyticsResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private String analyticsDate;
    private Integer totalBookings;
    private Integer confirmedBookings;
    private Integer cancelledBookings;
    private Integer noShows;
    private BigDecimal revenue;
    private BigDecimal occupancyPercentage;
    private BigDecimal averageRating;
    private String peakHourStart;
    private String peakHourEnd;
}
