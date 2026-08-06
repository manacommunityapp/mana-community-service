package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RatingResponse {
    private Long id;
    private Long vendorId;
    private UserRef user;
    private Long bookingId;
    private BigDecimal overallRating;
    private String comment;
    private Boolean isAnonymous;
    private String status;
    private List<CriteriaResponse> criteria;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class UserRef {
        private Long id;
        private String fullName;
    }

    @Data
    @Builder
    public static class CriteriaResponse {
        private String name;
        private BigDecimal score;
    }
}
