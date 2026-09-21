package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteRatingResponse {
    private Long id;
    private Long rideId;
    private Long raterId;
    private String raterName;
    private Long ratedId;
    private String ratedName;
    private int score;
    private String comment;
    private LocalDateTime createdAt;
}
