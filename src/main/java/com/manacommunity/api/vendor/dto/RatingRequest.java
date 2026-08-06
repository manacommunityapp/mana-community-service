package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RatingRequest {
    @NotNull
    private Long vendorId;
    private Long bookingId;
    @NotNull
    private BigDecimal overallRating;
    private String comment;
    private Boolean isAnonymous;
    private List<CriteriaRating> criteria;

    @Data
    public static class CriteriaRating {
        private String name;
        private BigDecimal score;
    }
}
