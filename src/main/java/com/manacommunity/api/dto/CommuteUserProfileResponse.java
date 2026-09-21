package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteUserProfileResponse {
    private Long userId;
    private String name;
    private String flat;
    private String photo;
    private double averageRating;
    private long totalRatings;
    private long ridesOffered;
    private long ridesBooked;
    private boolean kycVerified;
}
