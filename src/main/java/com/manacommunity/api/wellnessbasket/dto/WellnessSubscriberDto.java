package com.manacommunity.api.wellnessbasket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessSubscriberDto {
    private Long id;
    private Long userId;
    private String planType;
    private String portionSize;
    private String goalType;
    private List<String> dislikedIngredients;
    private String extraNotes;
    private String subscriptionStatus;
    private LocalDate pauseFrom;
    private LocalDate pauseUntil;
    private String cashfreeSubscriptionId;
    private Long clusterId;
    private String clusterSocietyName;
    private String clusterStatus;
    private Integer clusterCurrentSubscribers;
    private Integer clusterActivationThreshold;
    private OffsetDateTime createdAt;
}
