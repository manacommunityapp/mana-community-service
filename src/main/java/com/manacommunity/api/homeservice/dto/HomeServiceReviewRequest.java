package com.manacommunity.api.homeservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HomeServiceReviewRequest {
    private String bookingId;
    private String reviewerUserId;
    private String reviewerName;
    private String reviewerFlatInfo;
    private String revieweeWorkerId;
    private BigDecimal rating;
    private int workQuality;
    private int punctuality;
    private int behaviour;
    private int reliability;
    private String comment;
}
